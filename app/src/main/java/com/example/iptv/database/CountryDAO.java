package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Country;

import java.util.ArrayList;
import java.util.List;

public class CountryDAO {
    private SQLiteDatabase db;

    public CountryDAO(SQLiteDatabase db) {
        this.db = db;
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
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                );
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
                return new Country(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                );
            }
            return null;
        } finally {
            cursor.close(); // 🔐 always close cursor
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



}
