package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private SQLiteDatabase db;

    public CategoryDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(Category category) {
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        return db.insert("Category", null, values);
    }

    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM Category", null);
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category(
                        cursor.getInt(0),
                        cursor.getString(1)
                );
                list.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public Category getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM Category WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Category category = new Category(
                    cursor.getInt(0),
                    cursor.getString(1)
            );
            cursor.close();
            return category;
        }
        cursor.close();
        return null;
    }
}
