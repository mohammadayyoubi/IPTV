package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.OOP.Country;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        return db.insert("ChannelServer", null, values);
    }



    public List<ChannelServer> getByChannelId(int channelId) {
        List<ChannelServer> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM ChannelServer WHERE channelId = ?", new String[]{String.valueOf(channelId)});
        if (cursor.moveToFirst()) {
            do {
                ChannelServer server = new ChannelServer(
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3)
                );
                list.add(server);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    //may be used if later we develop server management
    public void deleteChannelServersByID(int channelId) {
        // Delete all servers associated with the channel
        db.delete("ChannelServer", "channelId = ?", new String[]{String.valueOf(channelId)});
    }

    // will be used when deleting a channel, so first should delete all its servers then the channel
    public void deleteChannelServersByChannelID(int channelId) {
        // Delete all servers associated with the channel
        db.delete("ChannelServer", "channelId = ?", new String[]{String.valueOf(channelId)});
    }

}

