package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {
    private SQLiteDatabase db;

    public FavoriteDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long addToFavorites(int channelId) {
        ContentValues values = new ContentValues();
        values.put("channelId", channelId);
        return db.insert("Favorite", null, values);
    }

    public boolean isFavorite(int channelId) {
        Cursor cursor = db.rawQuery("SELECT id FROM Favorite WHERE channelId = ?", new String[]{String.valueOf(channelId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public int removeFromFavorites(int channelId) {
        return db.delete("Favorite", "channelId = ?", new String[]{String.valueOf(channelId)});
    }

    public List<Integer> getAllFavoriteChannelIds() {
        List<Integer> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT channelId FROM Favorite", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
