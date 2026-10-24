package com.example.iptv.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "iptv_db";
    // v2: Channel.sourceId, ChannelServer.userAgent/referrer/status/lastChecked
    // v3: Channel.logoAuto (all migrated in place, see onUpgrade)
    private static final int DATABASE_VERSION = 3;

    // Table and column names
    private static final String TABLE_CHANNEL = "Channel";
    private static final String COLUMN_CHANNEL_ID = "id";
    private static final String COLUMN_CHANNEL_NAME = "name";
    private static final String COLUMN_CHANNEL_LOGO_URL = "logoUrl";
    private static final String COLUMN_CHANNEL_COUNTRY_ID = "countryId";
    private static final String COLUMN_CHANNEL_CATEGORY_ID = "categoryId";
    private static final String COLUMN_CHANNEL_SOURCE_ID = "sourceId";
    private static final String COLUMN_CHANNEL_LOGO_AUTO = "logoAuto";

    private static final String TABLE_CHANNEL_SERVER = "ChannelServer";
    private static final String COLUMN_CHANNEL_SERVER_ID = "id";
    private static final String COLUMN_CHANNEL_SERVER_CHANNEL_ID = "channelId";
    private static final String COLUMN_CHANNEL_SERVER_NAME = "serverName";
    private static final String COLUMN_CHANNEL_SERVER_STREAM_URL = "streamUrl";
    private static final String COLUMN_CHANNEL_SERVER_USER_AGENT = "userAgent";
    private static final String COLUMN_CHANNEL_SERVER_REFERRER = "referrer";
    private static final String COLUMN_CHANNEL_SERVER_STATUS = "status";
    private static final String COLUMN_CHANNEL_SERVER_LAST_CHECKED = "lastChecked";

    private static final String TABLE_COUNTRY = "Country";
    private static final String COLUMN_COUNTRY_ID = "id";
    private static final String COLUMN_COUNTRY_NAME = "name";
    private static final String COLUMN_COUNTRY_FLAG_URL = "flagUrl";

    private static final String TABLE_CATEGORY = "Category";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";

    private static final String TABLE_FAVORITE = "Favorite";
    private static final String COLUMN_FAVORITE_ID = "id";
    private static final String COLUMN_FAVORITE_CHANNEL_ID = "channel_id";

    // Create tables with ON DELETE CASCADE
    private static final String CREATE_COUNTRY_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_COUNTRY + " (" +
                    COLUMN_COUNTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_COUNTRY_NAME + " TEXT NOT NULL, " +
                    COLUMN_COUNTRY_FLAG_URL + " TEXT" +
                    ")";

    private static final String CREATE_CATEGORY_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORY + " (" +
                    COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CATEGORY_NAME + " TEXT NOT NULL" +
                    ")";

    private static final String CREATE_CHANNEL_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CHANNEL + " (" +
                    COLUMN_CHANNEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CHANNEL_NAME + " TEXT NOT NULL, " +
                    COLUMN_CHANNEL_LOGO_URL + " TEXT, " +
                    COLUMN_CHANNEL_COUNTRY_ID + " INTEGER NOT NULL, " +
                    COLUMN_CHANNEL_CATEGORY_ID + " INTEGER NOT NULL, " +
                    COLUMN_CHANNEL_SOURCE_ID + " TEXT, " +
                    COLUMN_CHANNEL_LOGO_AUTO + " INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (" + COLUMN_CHANNEL_COUNTRY_ID + ") REFERENCES " + TABLE_COUNTRY + "(" + COLUMN_COUNTRY_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + COLUMN_CHANNEL_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_CATEGORY_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_CHANNEL_SERVER_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CHANNEL_SERVER + " (" +
                    COLUMN_CHANNEL_SERVER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CHANNEL_SERVER_CHANNEL_ID + " INTEGER NOT NULL, " +
                    COLUMN_CHANNEL_SERVER_NAME + " TEXT NOT NULL, " +
                    COLUMN_CHANNEL_SERVER_STREAM_URL + " TEXT NOT NULL, " +
                    COLUMN_CHANNEL_SERVER_USER_AGENT + " TEXT, " +
                    COLUMN_CHANNEL_SERVER_REFERRER + " TEXT, " +
                    COLUMN_CHANNEL_SERVER_STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_CHANNEL_SERVER_LAST_CHECKED + " INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (" + COLUMN_CHANNEL_SERVER_CHANNEL_ID + ") REFERENCES " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_FAVORITE_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE + " (" +
                    COLUMN_FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FAVORITE_CHANNEL_ID + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + COLUMN_FAVORITE_CHANNEL_ID + ") REFERENCES " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_ID + ") ON DELETE CASCADE" +
                    ")";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.execSQL(CREATE_COUNTRY_TABLE_QUERY);
        db.execSQL(CREATE_CATEGORY_TABLE_QUERY);
        db.execSQL(CREATE_CHANNEL_TABLE_QUERY);
        db.execSQL(CREATE_CHANNEL_SERVER_TABLE_QUERY);
        db.execSQL(CREATE_FAVORITE_TABLE_QUERY);
        createIndexes(db);
    }

    private static void createIndexes(SQLiteDatabase db) {
        // Imports look channels up by iptv-org id; lists load servers per channel
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_channel_source ON " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_SOURCE_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_server_channel ON " + TABLE_CHANNEL_SERVER + "(" + COLUMN_CHANNEL_SERVER_CHANNEL_ID + ")");
        // Lists are sorted by name
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_channel_name ON " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_NAME + " COLLATE NOCASE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migrate in place so imported channels and favorites survive app updates
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL + " ADD COLUMN " + COLUMN_CHANNEL_SOURCE_ID + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL_SERVER + " ADD COLUMN " + COLUMN_CHANNEL_SERVER_USER_AGENT + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL_SERVER + " ADD COLUMN " + COLUMN_CHANNEL_SERVER_REFERRER + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL_SERVER + " ADD COLUMN " + COLUMN_CHANNEL_SERVER_STATUS + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL_SERVER + " ADD COLUMN " + COLUMN_CHANNEL_SERVER_LAST_CHECKED + " INTEGER NOT NULL DEFAULT 0");
            createIndexes(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_CHANNEL + " ADD COLUMN " + COLUMN_CHANNEL_LOGO_AUTO + " INTEGER NOT NULL DEFAULT 0");
            // Imported rows: their logos came from the source and may be refreshed
            db.execSQL("UPDATE " + TABLE_CHANNEL + " SET " + COLUMN_CHANNEL_LOGO_AUTO + " = 1 WHERE "
                    + COLUMN_CHANNEL_SOURCE_ID + " IS NOT NULL");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_channel_name ON " + TABLE_CHANNEL + "(" + COLUMN_CHANNEL_NAME + " COLLATE NOCASE)");
        }
    }

    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL_SERVER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNEL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRY);
        onCreate(db);
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = super.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");
        return db;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase db = super.getReadableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");
        return db;
    }

}
