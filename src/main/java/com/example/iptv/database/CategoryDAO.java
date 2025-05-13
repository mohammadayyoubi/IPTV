package com.example.iptv.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.iptv.OOP.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private static final String TABLE_CATEGORY = "Category";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";

    private SQLiteDatabase db;

    public CategoryDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(Category category) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, category.getName());
        return db.insert(TABLE_CATEGORY, null, values);
    }

    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORY, null);
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category(

                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                );
                category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                list.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public Category getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORY + " WHERE " + COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Category category = new Category(

                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            );
            category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            cursor.close();
            return category;
        }
        cursor.close();
        return null;
    }

    public Category getByName(String name) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORY + " WHERE " + COLUMN_NAME + " = ?",
                new String[]{name});
        if (cursor.moveToFirst()) {
            Category category = new Category(

                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            );
            category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            cursor.close();
            return category;
        }
        cursor.close();
        return null;
    }

    public int update(Category category) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, category.getName());
        return db.update(TABLE_CATEGORY, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(category.getId())});
    }

    public int delete(int id) {
        return db.delete(TABLE_CATEGORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
