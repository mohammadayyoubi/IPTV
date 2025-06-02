package com.example.iptv.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "iptv_db";
    private static final int DATABASE_VERSION = 1;

    // Channel Table
    private static final String TABLE_CHANNEL = "Channel";
    private static final String COLUMN_CHANNEL_ID = "id";
    private static final String COLUMN_CHANNEL_NAME = "name";
    private static final String COLUMN_CHANNEL_LOGO_URL = "logoUrl";
    private static final String COLUMN_CHANNEL_COUNTRY_ID = "countryId";
    private static final String COLUMN_CHANNEL_CATEGORY_ID = "categoryId";


    // ChannelServer Table
    private static final String TABLE_CHANNEL_SERVER = "ChannelServer";
    private static final String COLUMN_CHANNEL_SERVER_ID = "id";
    private static final String COLUMN_CHANNEL_SERVER_CHANNEL_ID = "channelId";
    private static final String COLUMN_CHANNEL_SERVER_NAME = "serverName";
    private static final String COLUMN_CHANNEL_SERVER_STREAM_URL = "streamUrl";

    private static final String CREATE_CHANNEL_SERVER_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CHANNEL_SERVER + " ("
                    + COLUMN_CHANNEL_SERVER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CHANNEL_SERVER_CHANNEL_ID + " INTEGER NOT NULL, "
                    + COLUMN_CHANNEL_SERVER_NAME + " TEXT NOT NULL, "
                    + COLUMN_CHANNEL_SERVER_STREAM_URL + " TEXT NOT NULL, "
                    + "FOREIGN KEY (" + COLUMN_CHANNEL_SERVER_CHANNEL_ID + ") REFERENCES " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_ID + ")"
                    + ")";

    // Country Table
    private static final String TABLE_COUNTRY = "Country";
    private static final String COLUMN_COUNTRY_ID = "id";
    private static final String COLUMN_COUNTRY_NAME = "name";
    private static final String COLUMN_COUNTRY_FLAG_URL = "flagUrl";

    private static final String CREATE_COUNTRY_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_COUNTRY + " ("
                    + COLUMN_COUNTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_COUNTRY_NAME + " TEXT NOT NULL, "
                    + COLUMN_COUNTRY_FLAG_URL + " TEXT"
                    + ")";

    // Category Table
    private static final String TABLE_CATEGORY = "Category";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";

    private static final String CREATE_CATEGORY_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORY + " ("
                    + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CATEGORY_NAME + " TEXT NOT NULL"
                    + ")";

    // Favorite Table
    private static final String TABLE_FAVORITE = "Favorite";
    private static final String COLUMN_FAVORITE_ID = "id";
    private static final String COLUMN_FAVORITE_CHANNEL_ID = "channel_id";

    private static final String CREATE_FAVORITE_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE + " ("
                    + COLUMN_FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_FAVORITE_CHANNEL_ID + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + COLUMN_FAVORITE_CHANNEL_ID + ") REFERENCES " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_ID + ")"
                    + ")";
    //create Channel Tbale Query
    private static final String CREATE_CHANNEL_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CHANNEL + " ("
                    + COLUMN_CHANNEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CHANNEL_NAME + " TEXT NOT NULL, "
                    + COLUMN_CHANNEL_LOGO_URL + " TEXT, "
                    + COLUMN_CHANNEL_COUNTRY_ID + " INTEGER NOT NULL, "
                    + COLUMN_CHANNEL_CATEGORY_ID + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + COLUMN_CHANNEL_COUNTRY_ID + ") REFERENCES " + TABLE_COUNTRY + "(" + COLUMN_COUNTRY_ID + "), "
                    + "FOREIGN KEY (" + COLUMN_CHANNEL_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_CATEGORY_ID + ")"
                    + ")";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CHANNEL_TABLE_QUERY);
        db.execSQL(CREATE_CHANNEL_SERVER_TABLE_QUERY);
        db.execSQL(CREATE_COUNTRY_TABLE_QUERY);
        db.execSQL(CREATE_CATEGORY_TABLE_QUERY);
        db.execSQL(CREATE_FAVORITE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL_SERVER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE);
        onCreate(db);
    }

    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL_SERVER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE);
        onCreate(db);
    }
}
