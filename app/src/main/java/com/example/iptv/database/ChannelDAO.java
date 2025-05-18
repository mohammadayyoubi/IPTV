package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Channel;

import java.util.ArrayList;
import java.util.List;

public class ChannelDAO {
    private SQLiteDatabase db;
    private ChannelServerDAO csd;

    public ChannelDAO(SQLiteDatabase db) {
        this.db = db;
        csd=new ChannelServerDAO(db);
    }

    public long insert(Channel channel) {
        ContentValues values = new ContentValues();
        values.put("name", channel.getName());
        values.put("logoUrl", channel.getLogoUrl());
        values.put("countryId", channel.getCountryId());
        values.put("categoryId", channel.getCategoryId());
        return db.insert("Channel", null, values);
    }

    public List<Channel> getAll() {
        List<Channel> list = new ArrayList<>();

        Cursor channelCursor = db.rawQuery("SELECT * FROM Channel", null);
        if (channelCursor.moveToFirst()) {
            do {

                Channel channel = new Channel(
                        channelCursor.getInt(0),
                        channelCursor.getString(1),
                        channelCursor.getString(2),
                        channelCursor.getInt(3),
                        channelCursor.getInt(4),
                        csd.getByChannelId(channelCursor.getInt(0))

                );
                list.add(channel);
            } while (channelCursor.moveToNext());
        }
        channelCursor.close();
        return list;
    }

    public Channel getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM Channel WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Channel channel = new Channel(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getInt(4),
                    csd.getByChannelId(cursor.getInt(0))
            );
            cursor.close();
            return channel;
        }
        cursor.close();
        return null;
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Channel", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }

}
