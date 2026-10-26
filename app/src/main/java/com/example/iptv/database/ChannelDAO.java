package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Channel;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChannelDAO {

    private static final String TABLE_CHANNEL = "Channel";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LOGO_URL = "logoUrl";
    private static final String COLUMN_COUNTRY_ID = "countryId";
    private static final String COLUMN_CATEGORY_ID = "categoryId";
    private static final String COLUMN_SOURCE_ID = "sourceId";
    private static final String COLUMN_LOGO_AUTO = "logoAuto";

    // Server totals per channel, joined into paged list queries
    private static final String STATS_JOIN = " FROM Channel c LEFT JOIN (SELECT channelId, COUNT(*) AS total, "
            + "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS broken FROM ChannelServer GROUP BY channelId) s "
            + "ON s.channelId = c.id ";

    private SQLiteDatabase db;
    private ChannelServerDAO csd;

    public ChannelDAO(SQLiteDatabase db) {
        this.db = db;
        this.csd = new ChannelServerDAO(db);
    }

    /** Runs bulk imports in one transaction: thousands of inserts go from minutes to seconds. */
    public void runInTransaction(Runnable work) {
        db.beginTransaction();
        try {
            work.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public long insert(Channel channel) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, channel.getName());
        values.put(COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(COLUMN_COUNTRY_ID, channel.getCountryId());
        values.put(COLUMN_CATEGORY_ID, channel.getCategoryId());
        values.put(COLUMN_SOURCE_ID, channel.getSourceId());
        values.put(COLUMN_LOGO_AUTO, channel.isLogoAuto() ? 1 : 0);

        long id = db.insert(TABLE_CHANNEL, null, values);
        if (id != -1) {
            channel.setId((int) id);  // assign the auto-generated ID to the object
        }
        return id;
    }

    public List<Channel> getAll() {
        return query("SELECT * FROM " + TABLE_CHANNEL, null);
    }

    public Channel getById(int id) {
        List<Channel> list = query("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    /** The channel imported from iptv-org with this id, if any. */
    public Channel getBySourceId(String sourceId) {
        List<Channel> list = query("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_SOURCE_ID + " = ? LIMIT 1",
                new String[]{sourceId});
        return list.isEmpty() ? null : list.get(0);
    }

    /** Channels imported before sourceId existed: matched by name + country (case-insensitive). */
    public Channel getByNameAndCountry(String name, int countryId) {
        List<Channel> list = query("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_NAME + " = ? COLLATE NOCASE AND "
                        + COLUMN_COUNTRY_ID + " = ? LIMIT 1",
                new String[]{name, String.valueOf(countryId)});
        return list.isEmpty() ? null : list.get(0);
    }

    public int update(Channel channel) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, channel.getName());
        values.put(COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(COLUMN_COUNTRY_ID, channel.getCountryId());
        values.put(COLUMN_CATEGORY_ID, channel.getCategoryId());
        values.put(COLUMN_LOGO_AUTO, channel.isLogoAuto() ? 1 : 0);

        return db.update(TABLE_CHANNEL, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(channel.getId())});
    }

    /** Sets the logo (and links the iptv-org id) without touching the admin-edited fields. */
    public int updateLogoAndSource(int id, String logoUrl, String sourceId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOGO_URL, logoUrl);
        values.put(COLUMN_LOGO_AUTO, 1);
        if (sourceId != null) values.put(COLUMN_SOURCE_ID, sourceId);
        return db.update(TABLE_CHANNEL, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int deleteChanelByID(int id) {
        csd.deleteChannelServersByChannelID(id); // delete associated servers first (cascade)
        return db.delete(TABLE_CHANNEL, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHANNEL, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public void deleteAllChannels() {
       // csd.deleteAllChannelServers(); // Delete all associated servers first
        db.delete(TABLE_CHANNEL, null, null); // Replace "channels" with your actual table name
    }

    public List<Channel> getAllByCountryId(int countryId) {
        return query("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_COUNTRY_ID + " = ?",
                new String[]{String.valueOf(countryId)});
    }

    public List<Channel> getAllByCategoryId(int categoryId) {
        return query("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)});
    }

    // ───────────── Paged lists (home, favorites, filtered, admin) ─────────────

    /** One page of channels without their servers; server totals are filled in for availability. */
    public List<Channel> queryPage(ChannelQuery q, int limit, int offset) {
        List<String> args = new ArrayList<>();
        String where = where(q, args, true);
        String order = " ORDER BY ";
        if (q.availability == ChannelQuery.Availability.RECENT) {
            // Recently watched: newest first
            order += "(SELECT h.watchedAt FROM History h WHERE h.channel_id = c.id) DESC, ";
        } else if (q.priorityCountryId != 0) {
            // The viewer's own country first, then A-Z
            order += "(c.countryId = ?) DESC, ";
            args.add(String.valueOf(q.priorityCountryId));
        }
        order += "c.name COLLATE NOCASE, c.id";
        args.add(String.valueOf(limit));
        args.add(String.valueOf(offset));
        String sql = "SELECT c.id, c.name, c.logoUrl, c.countryId, c.categoryId, c.sourceId, c.logoAuto, "
                + "IFNULL(s.total, 0) AS total, IFNULL(s.broken, 0) AS broken" + STATS_JOIN + where
                + order + " LIMIT ? OFFSET ?";
        List<Channel> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                Channel channel = new Channel(cursor.getString(1), cursor.getString(2),
                        cursor.getInt(3), cursor.getInt(4), null);
                channel.setId(cursor.getInt(0));
                channel.setSourceId(cursor.getString(5));
                channel.setLogoAuto(cursor.getInt(6) == 1);
                channel.setServerStats(cursor.getInt(7), cursor.getInt(8));
                list.add(channel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /** How many channels match {@code q} in each availability group (q.availability is ignored). */
    public ChannelQuery.Counts countByAvailability(ChannelQuery q) {
        List<String> args = new ArrayList<>();
        String where = where(q, args, false);
        String sql = "SELECT "
                + "SUM(CASE WHEN IFNULL(s.total, 0) > 0 AND IFNULL(s.broken, 0) < s.total THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN IFNULL(s.total, 0) > 0 AND IFNULL(s.broken, 0) >= s.total THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN IFNULL(s.total, 0) = 0 THEN 1 ELSE 0 END), COUNT(*), "
                + "SUM(CASE WHEN c.id IN (SELECT channel_id FROM History) THEN 1 ELSE 0 END)" + STATS_JOIN + where;
        ChannelQuery.Counts counts = new ChannelQuery.Counts();
        Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]));
        try {
            if (cursor.moveToFirst()) {
                counts.watchable = cursor.getInt(0);
                counts.notWorking = cursor.getInt(1);
                counts.noServers = cursor.getInt(2);
                counts.all = cursor.getInt(3);
                counts.recent = cursor.getInt(4);
            }
        } finally {
            cursor.close();
        }
        return counts;
    }

    private static String where(ChannelQuery q, List<String> args, boolean withAvailability) {
        List<String> clauses = new ArrayList<>();
        if (q.search != null && !q.search.trim().isEmpty()) {
            // '!' is the LIKE escape character, so a search for "100%" matches literally
            clauses.add("c.name LIKE ? ESCAPE '!'");
            String escaped = q.search.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            args.add("%" + escaped + "%");
        }
        if (q.countryId != 0) {
            clauses.add("c.countryId = ?");
            args.add(String.valueOf(q.countryId));
        }
        if (q.categoryId != 0) {
            clauses.add("c.categoryId = ?");
            args.add(String.valueOf(q.categoryId));
        }
        if (q.favoritesOnly) {
            clauses.add("c.id IN (SELECT channel_id FROM Favorite)");
        }
        if (withAvailability) {
            switch (q.availability) {
                case WATCHABLE:
                    clauses.add("IFNULL(s.total, 0) > 0 AND IFNULL(s.broken, 0) < s.total");
                    break;
                case NOT_WORKING:
                    clauses.add("IFNULL(s.total, 0) > 0 AND IFNULL(s.broken, 0) >= s.total");
                    break;
                case NO_SERVERS:
                    clauses.add("IFNULL(s.total, 0) = 0");
                    break;
                case RECENT:
                    clauses.add("c.id IN (SELECT channel_id FROM History)");
                    break;
                default:
                    break;
            }
        }
        return clauses.isEmpty() ? "" : " WHERE " + TextUtils.join(" AND ", clauses);
    }

    // ───────────── In-memory index used by imports / logo refresh ─────────────

    /** The few columns imports need, for every channel, in one query. */
    public static class Lite {
        public int id, countryId;
        public String name, logoUrl, sourceId;
        public boolean logoAuto;
    }

    public static class Index {
        public final Map<String, Lite> bySource = new HashMap<>();
        public final Map<String, Lite> byNameAndCountry = new HashMap<>();
        /** Every row, including channels that share a name within a country */
        public final List<Lite> all = new ArrayList<>();
        public boolean hasRowsWithoutSource;

        public static String key(String name, int countryId) {
            return name.toLowerCase(Locale.ROOT) + "|" + countryId;
        }

        public void add(Lite lite) {
            all.add(lite);
            if (lite.sourceId != null) bySource.put(lite.sourceId, lite);
            else hasRowsWithoutSource = true;
            byNameAndCountry.put(key(lite.name, lite.countryId), lite);
        }
    }

    public Index loadIndex() {
        Index index = new Index();
        Cursor cursor = db.rawQuery("SELECT id, name, countryId, logoUrl, sourceId, logoAuto FROM " + TABLE_CHANNEL, null);
        try {
            while (cursor.moveToNext()) {
                Lite lite = new Lite();
                lite.id = cursor.getInt(0);
                lite.name = cursor.getString(1);
                lite.countryId = cursor.getInt(2);
                lite.logoUrl = cursor.getString(3);
                lite.sourceId = cursor.getString(4);
                lite.logoAuto = cursor.getInt(5) == 1;
                index.add(lite);
            }
        } finally {
            cursor.close();
        }
        return index;
    }

    /** Distinct countries that have at least one channel (for "update my countries"). */
    public List<Integer> getCountryIdsInUse() {
        List<Integer> ids = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT countryId FROM " + TABLE_CHANNEL, null);
        try {
            while (cursor.moveToNext()) ids.add(cursor.getInt(0));
        } finally {
            cursor.close();
        }
        return ids;
    }

    private List<Channel> query(String sql, String[] args) {
        List<Channel> list = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args);
        try {
            int idCol = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int nameCol = cursor.getColumnIndexOrThrow(COLUMN_NAME);
            int logoCol = cursor.getColumnIndexOrThrow(COLUMN_LOGO_URL);
            int countryCol = cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_ID);
            int categoryCol = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID);
            int sourceCol = cursor.getColumnIndexOrThrow(COLUMN_SOURCE_ID);
            while (cursor.moveToNext()) {
                Channel channel = new Channel(
                        cursor.getString(nameCol),
                        cursor.getString(logoCol),
                        cursor.getInt(countryCol),
                        cursor.getInt(categoryCol),
                        null
                );
                channel.setId(cursor.getInt(idCol));
                channel.setSourceId(cursor.getString(sourceCol));
                channel.setLogoAuto(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOGO_AUTO)) == 1);
                list.add(channel);
                ids.add(channel.getId());
            }
        } finally {
            cursor.close();
        }
        // Load all servers in a few batched queries instead of one query per channel
        java.util.Map<Integer, List<com.example.iptv.OOP.ChannelServer>> servers = csd.getByChannelIds(ids);
        for (Channel channel : list) {
            List<com.example.iptv.OOP.ChannelServer> own = servers.get(channel.getId());
            channel.setServers(own != null ? own : new ArrayList<>());
        }
        return list;
    }

}
