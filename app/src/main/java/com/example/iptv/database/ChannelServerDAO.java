package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.ChannelServer;

import java.util.ArrayList;
import java.util.List;

public class ChannelServerDAO {
    private SQLiteDatabase db;

    public ChannelServerDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(ChannelServer server) {
        ContentValues values = new ContentValues();
        values.put("channelId", server.getChannelId());
        values.put("serverName", server.getServerName());
        values.put("streamUrl", server.getStreamUrl());
        values.put("userAgent", server.getUserAgent());
        values.put("referrer", server.getReferrer());
        values.put("status", server.getStatus());
        values.put("lastChecked", server.getLastChecked());
        long id = db.insert("ChannelServer", null, values);
        if (id != -1) server.setId((int) id);
        return id;
    }

    public List<ChannelServer> getByChannelId(int channelId) {
        return query("SELECT * FROM ChannelServer WHERE channelId = ? ORDER BY id", new String[]{String.valueOf(channelId)});
    }

    /** Servers of many channels at once, grouped by channel id (avoids one query per channel). */
    public java.util.Map<Integer, List<ChannelServer>> getByChannelIds(List<Integer> channelIds) {
        java.util.Map<Integer, List<ChannelServer>> result = new java.util.HashMap<>();
        final int chunk = 900; // SQLite's bound-parameter limit is 999
        for (int start = 0; start < channelIds.size(); start += chunk) {
            List<Integer> part = channelIds.subList(start, Math.min(start + chunk, channelIds.size()));
            StringBuilder placeholders = new StringBuilder();
            String[] args = new String[part.size()];
            for (int i = 0; i < part.size(); i++) {
                placeholders.append(i == 0 ? "?" : ",?");
                args[i] = String.valueOf(part.get(i));
            }
            for (ChannelServer server : query("SELECT * FROM ChannelServer WHERE channelId IN (" + placeholders + ") ORDER BY id", args)) {
                List<ChannelServer> list = result.get(server.getChannelId());
                if (list == null) result.put(server.getChannelId(), list = new ArrayList<>());
                list.add(server);
            }
        }
        return result;
    }

    /** Every server in the database, used by the stream checker. */
    public List<ChannelServer> getAll() {
        return query("SELECT * FROM ChannelServer ORDER BY id", null);
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM ChannelServer", null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public boolean exists(int channelId, String streamUrl) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM ChannelServer WHERE channelId = ? AND streamUrl = ? LIMIT 1",
                new String[]{String.valueOf(channelId), streamUrl});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    /** channelId → stream URLs already stored, so imports can skip duplicates without a query each. */
    public java.util.Map<Integer, java.util.Set<String>> loadUrlIndex() {
        java.util.Map<Integer, java.util.Set<String>> index = new java.util.HashMap<>();
        Cursor cursor = db.rawQuery("SELECT channelId, streamUrl FROM ChannelServer", null);
        try {
            while (cursor.moveToNext()) {
                java.util.Set<String> urls = index.get(cursor.getInt(0));
                if (urls == null) index.put(cursor.getInt(0), urls = new java.util.HashSet<>());
                urls.add(cursor.getString(1));
            }
        } finally {
            cursor.close();
        }
        return index;
    }

    /** Servers to test: all of them, or only those unchecked / last checked before {@code olderThan}. */
    public List<ChannelServer> getForCheck(long olderThan) {
        if (olderThan <= 0) return getAll();
        return query("SELECT * FROM ChannelServer WHERE lastChecked < ? ORDER BY id",
                new String[]{String.valueOf(olderThan)});
    }

    /** Writes many check results in one transaction (one disk sync instead of thousands). */
    public void updateStatuses(List<int[]> idAndStatus, long checkedAt) {
        if (idAndStatus.isEmpty()) return;
        db.beginTransaction();
        try {
            android.database.sqlite.SQLiteStatement stmt =
                    db.compileStatement("UPDATE ChannelServer SET status = ?, lastChecked = ? WHERE id = ?");
            for (int[] pair : idAndStatus) {
                stmt.bindLong(1, pair[1]);
                stmt.bindLong(2, checkedAt);
                stmt.bindLong(3, pair[0]);
                stmt.executeUpdateDelete();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateStatus(int serverId, int status, long checkedAt) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("lastChecked", checkedAt);
        db.update("ChannelServer", values, "id = ?", new String[]{String.valueOf(serverId)});
    }

    private List<ChannelServer> query(String sql, String[] args) {
        List<ChannelServer> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args);
        try {
            int idCol = cursor.getColumnIndexOrThrow("id");
            int channelCol = cursor.getColumnIndexOrThrow("channelId");
            int nameCol = cursor.getColumnIndexOrThrow("serverName");
            int urlCol = cursor.getColumnIndexOrThrow("streamUrl");
            int uaCol = cursor.getColumnIndexOrThrow("userAgent");
            int refCol = cursor.getColumnIndexOrThrow("referrer");
            int statusCol = cursor.getColumnIndexOrThrow("status");
            int checkedCol = cursor.getColumnIndexOrThrow("lastChecked");
            while (cursor.moveToNext()) {
                ChannelServer server = new ChannelServer(
                        cursor.getInt(channelCol),
                        cursor.getString(nameCol),
                        cursor.getString(urlCol),
                        cursor.getString(uaCol),
                        cursor.getString(refCol)
                );
                server.setId(cursor.getInt(idCol));
                server.setStatus(cursor.getInt(statusCol));
                server.setLastChecked(cursor.getLong(checkedCol));
                list.add(server);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    //may be used if later we develop server management
    public void deleteChannelServersByID(int channelId) {
        // Delete all servers associated with the channel
        db.delete("ChannelServer", "channelId = ?", new String[]{String.valueOf(channelId)});
    }

    public void deleteAllChannelServers() {
        db.delete("ChannelServer", null, null);
    }

    // will be used when deleting a channel, so first should delete all its servers then the channel
    public void deleteChannelServersByChannelID(int channelId) {
        // Delete all servers associated with the channel
        db.delete("ChannelServer", "channelId = ?", new String[]{String.valueOf(channelId)});
    }

}
