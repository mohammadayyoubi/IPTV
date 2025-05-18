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


}
