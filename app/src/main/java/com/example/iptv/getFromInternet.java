package com.example.iptv;

import android.os.Looper;

import com.example.iptv.Interfaces.CategoryCallback;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Country;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import android.os.Handler;



public class getFromInternet {
    public static ArrayList<Country> getAllCountries() {
        ArrayList<Country> countryList = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        String flagBaseUrl = "https://flagcdn.com/w80/";

        for (String iso : Locale.getISOCountries()) {
            Locale locale = new Locale("", iso);
            String countryName = locale.getDisplayCountry(Locale.ENGLISH);

            if (!countryName.isEmpty() && !seenCodes.contains(iso)) {
                String flagUrl = flagBaseUrl + iso.toLowerCase() + ".png";
                countryList.add(new Country(countryName, flagUrl));
                seenCodes.add(iso);
            }
        }

        return countryList;
    }
    public static void getAllCategories(CategoryCallback callback) {
        new Thread(() -> {
            ArrayList<Category> categoryList = new ArrayList<>();
            String urlString = "https://iptv-org.github.io/api/categories.json";

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject categoryObject = jsonArray.getJSONObject(i);
                    String name = categoryObject.getString("name");
                    categoryList.add(new Category(name));
                }

                // Call the callback on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onCategoriesLoaded(categoryList);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}


