package com.example.iptv.Interfaces;

import com.example.iptv.OOP.Category;

import java.util.ArrayList;

public interface CategoryCallback {
    void onCategoriesLoaded(ArrayList<Category> categories);
}
