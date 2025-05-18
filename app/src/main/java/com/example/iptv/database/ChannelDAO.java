package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Channel;

import java.util.ArrayList;
import java.util.List;

public class ChannelDAO {

    private static final String TABLE_CHANNEL = "Channel";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LOGO_URL = "logoUrl";
    private static final String COLUMN_COUNTRY_ID = "countryId";
    private static final String COLUMN_CATEGORY_ID = "categoryId";

    private SQLiteDatabase db;
    private ChannelServerDAO csd;

    public ChannelDAO(SQLiteDatabase db) {
        this.db = db;
        this.csd = new ChannelServerDAO(db);
    }

    public long insert(Channel channel) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, channel.getName());
        values.put(COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(COLUMN_COUNTRY_ID, channel.getCountryId());
        values.put(COLUMN_CATEGORY_ID, channel.getCategoryId());

        long id = db.insert(TABLE_CHANNEL, null, values);
        if (id != -1) {
            channel.setId((int) id);  // assign the auto-generated ID to the object
        }
        return id;
    }

    public List<Channel> getAll() {
        List<Channel> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CHANNEL, null);
        if (cursor.moveToFirst()) {
            do {
                Channel channel = new Channel(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGO_URL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
                        csd.getByChannelId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                );
                channel.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                list.add(channel);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public Channel getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CHANNEL + " WHERE " + COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Channel channel = new Channel(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGO_URL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
                    csd.getByChannelId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
            );
            channel.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            cursor.close();
            return channel;
        }
        cursor.close();
        return null;
    }

    public int update(Channel channel) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, channel.getName());
        values.put(COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(COLUMN_COUNTRY_ID, channel.getCountryId());
        values.put(COLUMN_CATEGORY_ID, channel.getCategoryId());

        return db.update(TABLE_CHANNEL, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(channel.getId())});
    }

    public int delete(int id) {
        return db.delete(TABLE_CHANNEL, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHANNEL, null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }
}
