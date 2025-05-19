package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Country;

import java.util.ArrayList;
import java.util.List;

public class CountryDAO {
    private SQLiteDatabase db;
    private ChannelServerDAO csd;
    private ChannelDAO cd;

    public CountryDAO(SQLiteDatabase db) {
        this.db = db;
        this.csd = new ChannelServerDAO(db);
        this.cd = new ChannelDAO(db);

    }

    public long insert(Country country) {
        ContentValues values = new ContentValues();
        values.put("name", country.getName());
        values.put("flagUrl", country.getFlagUrl());
        return db.insert("Country", null, values);
    }

    public List<Country> getAll() {
        List<Country> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM Country", null);
        if (cursor.moveToFirst()) {
            do {
                Country country = new Country(

                        cursor.getString(1),
                        cursor.getString(2)
                );
                country.setId(cursor.getInt(0));
                list.add(country);
            } while (cursor.moveToNext());
        }
        cursor.close(); // ✅ important!
        return list;
    }

    public Country getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM Country WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) {
                Country c=new Country(

                        cursor.getString(1),
                        cursor.getString(2)
                );
                c.setId(cursor.getInt(0));
                return c;

            }
            return null;
        } finally {
            cursor.close(); // 🔐 always close cursor
        }
    }

    public Country getByName(String name) {
        Cursor cursor = db.rawQuery("SELECT * FROM Country WHERE name = ?", new String[]{name});

        try {
            if (cursor.moveToFirst()) {
                Country c=new Country(
                        cursor.getString(1),
                        cursor.getString(2)
                );
                c.setId(cursor.getInt(0));
                return c;
                }
            return null;
        } finally {
            cursor.close();
            }
    }

    public int update(Country country) {

        ContentValues values = new ContentValues();
        values.put("name", country.getName());
        values.put("flagUrl", country.getFlagUrl());
        return db.update("Country", values, "id = ?", new String[]{String.valueOf(country.getId())});
    }

    public int delete(int id) {
        return db.delete("Country", "id = ?", new String[]{String.valueOf(id)});
    }

    public int count() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Country", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }


    public void deleteAllCountries() {
        // Delete all rows from the "Channel" table and channel server, thats bequase of foreign key constraints
        cd.deleteAllChannels();
        csd.deleteAllChannelServers();
        // Delete all rows from the "Country" table
        db.delete("Country", null, null); // replace "country_table" with your actual table name
    }



}
