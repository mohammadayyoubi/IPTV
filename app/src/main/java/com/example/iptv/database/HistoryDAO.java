package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** "Recently watched": one row per channel with the last time it played. */
public class HistoryDAO {
    private final SQLiteDatabase db;

    public HistoryDAO(SQLiteDatabase db) {
        this.db = db;
    }

    /** Marks the channel as watched now (moves it to the top of Recently watched). */
    public void recordWatched(int channelId) {
        ContentValues values = new ContentValues();
        values.put("channel_id", channelId);
        values.put("watchedAt", System.currentTimeMillis());
        db.insertWithOnConflict("History", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clear() {
        db.delete("History", null, null);
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM History", null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }
}
