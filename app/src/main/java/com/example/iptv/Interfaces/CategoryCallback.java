package com.example.iptv.Interfaces;

import com.example.iptv.OOP.Category;

import java.util.ArrayList;
/**
 * Called when the category list is fetched from the internet.
 * Runs on the main thread.
 */
public interface CategoryCallback {
    void onCategoriesLoaded(ArrayList<Category> categories);
}
