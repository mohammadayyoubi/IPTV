package com.example.iptv.database;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "iptv_db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Channel Table
        db.execSQL("CREATE TABLE IF NOT EXISTS Channel ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "logoUrl TEXT,"
                + "countryId INTEGER NOT NULL,"
                + "categoryId INTEGER NOT NULL,"
                + "FOREIGN KEY (countryId) REFERENCES Country(id),"
                + "FOREIGN KEY (categoryId) REFERENCES Category(id)"
                + ")");

        // Create ChannelServer Table
        db.execSQL("CREATE TABLE IF NOT EXISTS ChannelServer ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "channelId INTEGER NOT NULL,"
                + "serverName TEXT NOT NULL,"
                + "streamUrl TEXT NOT NULL,"
                + "FOREIGN KEY (channelId) REFERENCES Channel(id)"
                + ")");

        // Create Country Table
        db.execSQL("CREATE TABLE IF NOT EXISTS Country ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "flagUrl TEXT"
                + ")");

        // Create Category Table
        db.execSQL("CREATE TABLE IF NOT EXISTS Category ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL"
                + ")");

        // Create Favorite Table
        db.execSQL("CREATE TABLE IF NOT EXISTS Favorite ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "channel_id INTEGER NOT NULL,"
                + "FOREIGN KEY (channel_id) REFERENCES Channel(id)"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Channel");
        db.execSQL("DROP TABLE IF EXISTS ChannelServer");
        db.execSQL("DROP TABLE IF EXISTS Country");
        db.execSQL("DROP TABLE IF EXISTS Category");
        db.execSQL("DROP TABLE IF EXISTS Favorite");
        onCreate(db);
    }
}

