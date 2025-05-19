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
        // Start a background thread because network operations are not allowed on the main thread
        new Thread(() -> {
            ArrayList<Category> categoryList = new ArrayList<>(); // Will hold fetched categories
            String urlString = "https://iptv-org.github.io/api/categories.json"; // API endpoint

            try {
                // Create URL and open HTTP connection
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET"); // Request type: GET

                // Prepare to read the response
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                // Read response line by line
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close(); // Close the input stream

                // Parse the JSON array from response
                JSONArray jsonArray = new JSONArray(response.toString());

                // Loop through each JSON object in the array
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject categoryObject = jsonArray.getJSONObject(i);

                    // Extract the "name" field (we ignore "id" for now)
                    String name = categoryObject.getString("name");

                    // Create a Category object and add it to the list
                    categoryList.add(new Category(name));
                }

                // Return the result to the main thread using Handler
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onCategoriesLoaded(categoryList); // Invoke the callback
                });

            } catch (Exception e) {
                e.printStackTrace(); // Log errors (network failure, JSON issue, etc.)
            }
        }).start(); // Start the background thread
    }

}


