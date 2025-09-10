package com.example.iptv;

import android.os.Looper;

import com.example.iptv.Interfaces.CategoryCallback;
import com.example.iptv.Interfaces.ChannelCallback;
import com.example.iptv.Interfaces.CountryCodeCallback;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.CountryDAO;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.os.Handler;
import android.util.Log;


public class getFromInternet {
//get all countries method
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

//get all categories method
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


//get user country code by his ip
public static void getUserCountryCode(CountryCodeCallback callback) {
    new Thread(() -> {
        try {
            Log.d("IPAPI", "Requesting country from ipinfo.io...");

            URL url = new URL("https://ipinfo.io/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            Log.d("IPAPI", "Response code: " + responseCode);

            if (responseCode != 200) {
                Log.e("IPAPI", "Failed to fetch country. HTTP Code: " + responseCode);
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onCountryCodeReceived("LB"); // fallback to Lebanon
                });
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d("IPAPI", "Raw response: " + response);

            JSONObject json = new JSONObject(response.toString());
            String countryCode = json.getString("country"); // e.g. "LB"
            Log.d("IPAPI", "Resolved country code: " + countryCode);

            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onCountryCodeReceived(countryCode);
            });

        } catch (Exception e) {
            Log.e("IPAPI", "Error fetching country code", e);
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onCountryCodeReceived("LB"); // fallback to Lebanon
            });
        }
    }).start();
}

//get channels accourding to user country
public static void getAllChannelsByCountry(String countryCode,
                                           ChannelDAO channelDao,
                                           CountryDAO countryDao,
                                           CategoryDAO categoryDao,
                                           ChannelServerDAO channelServerDao,
                                           ChannelCallback callback) {
    new Thread(() -> {
        List<Channel> filteredChannels = new ArrayList<>();

        try {
            Log.d("IPTV", "Fetching channels.json...");
            URL channelsUrl = new URL("https://iptv-org.github.io/api/channels.json");
            HttpURLConnection conn = (HttpURLConnection) channelsUrl.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            JSONArray channelsArray = new JSONArray(response.toString());

            Log.d("IPTV", "Fetching streams.json...");
            URL streamsUrl = new URL("https://iptv-org.github.io/api/streams.json");
            HttpURLConnection streamsConn = (HttpURLConnection) streamsUrl.openConnection();
            streamsConn.setRequestMethod("GET");
            BufferedReader streamsReader = new BufferedReader(new InputStreamReader(streamsConn.getInputStream()));
            StringBuilder streamsResponse = new StringBuilder();
            while ((line = streamsReader.readLine()) != null) streamsResponse.append(line);
            streamsReader.close();
            JSONArray streamsArray = new JSONArray(streamsResponse.toString());

            Log.d("IPTV", "Fetching logos.json...");
            URL logosUrl = new URL("https://iptv-org.github.io/api/logos.json");
            HttpURLConnection logosConn = (HttpURLConnection) logosUrl.openConnection();
            logosConn.setRequestMethod("GET");
            BufferedReader logosReader = new BufferedReader(new InputStreamReader(logosConn.getInputStream()));
            StringBuilder logosResponse = new StringBuilder();
            while ((line = logosReader.readLine()) != null) logosResponse.append(line);
            logosReader.close();
            JSONArray logosArray = new JSONArray(logosResponse.toString());

            // Build channelId → logoUrl map (pick first non-empty)
            Map<String, String> channelLogos = new HashMap<>();
            for (int i = 0; i < logosArray.length(); i++) {
                JSONObject logoObj = logosArray.getJSONObject(i);
                String chId = logoObj.optString("channel", "");
                String urlLogo = logoObj.optString("url", "");
                if (!chId.isEmpty() && !urlLogo.isEmpty() && !channelLogos.containsKey(chId)) {
                    channelLogos.put(chId, urlLogo);
                }
            }

            // Map channel_id → list of stream URLs
            Map<String, List<String>> channelStreams = new HashMap<>();
            for (int i = 0; i < streamsArray.length(); i++) {
                JSONObject streamObj = streamsArray.getJSONObject(i);
                String channelId = streamObj.isNull("channel") ? null : streamObj.optString("channel", null);
                String urlStream = streamObj.optString("url", "");
                if (channelId == null || urlStream.isEmpty()) continue;
                channelStreams.computeIfAbsent(channelId, k -> new ArrayList<>()).add(urlStream);
            }

            // Filter and insert channels
            for (int i = 0; i < channelsArray.length(); i++) {
                JSONObject obj = channelsArray.getJSONObject(i);
                String channelId = obj.optString("id", "");
                String name = obj.optString("name", "Unnamed");
                String channelCountry = obj.optString("country", "");
                JSONArray categories = obj.optJSONArray("categories");
                String categoryName = (categories != null && categories.length() > 0)
                        ? categories.optString(0, "General")
                        : "General";

                if (!channelCountry.equalsIgnoreCase(countryCode)) continue;

                String fullCountryName = new Locale("", channelCountry).getDisplayCountry(Locale.ENGLISH);
                Country country = countryDao.getByName(fullCountryName);
                if (country == null) continue;
                int countryId = country.getId();

                String normalizedCategory = categoryName.substring(0,1).toUpperCase() +
                        categoryName.substring(1).toLowerCase();
                Category category = categoryDao.getByName(normalizedCategory);
                if (category == null) continue;
                int categoryId = category.getId();

                // Use logo from logos.json if present, else fallback to channels.json
                String logo = channelLogos.getOrDefault(channelId, obj.optString("logo", ""));

                List<ChannelServer> servers = new ArrayList<>();
                List<String> urls = channelStreams.get(channelId);
                if (urls != null) {
                    for (int j = 0; j < urls.size(); j++) {
                        servers.add(new ChannelServer(0, "Server " + (j+1), urls.get(j)));
                    }
                }

                Channel channel = new Channel(name, logo, countryId, categoryId, servers);
                long insertedId = channelDao.insert(channel);
                for (ChannelServer cs : servers) {
                    cs.setChannelId((int) insertedId);
                    channelServerDao.insert(cs);
                }

                filteredChannels.add(channel);
            }

            Log.i("IPTV", "Saved " + filteredChannels.size() + " channels for " + countryCode);
            new Handler(Looper.getMainLooper()).post(() -> callback.onChannelsFetched(filteredChannels));

        } catch (Exception e) {
            Log.e("IPTV", "Error in getAllChannelsByCountry", e);
            new Handler(Looper.getMainLooper()).post(() -> callback.onChannelsFetched(new ArrayList<>()));
        }
    }).start();
}

    public static void getAllChannelsOnceAndFilter(
            ChannelDAO channelDao,
            CountryDAO countryDao,
            CategoryDAO categoryDao,
            ChannelServerDAO channelServerDao,
            int maxChannelsPerCountry,
            Runnable onComplete) {

        new Thread(() -> {
            try {
                Log.d("IPTV", "Fetching channels.json / streams.json / logos.json...");
                JSONArray channelsArray = fetchJsonArray("https://iptv-org.github.io/api/channels.json");
                JSONArray streamsArray  = fetchJsonArray("https://iptv-org.github.io/api/streams.json");
                JSONArray logosArray    = fetchJsonArray("https://iptv-org.github.io/api/logos.json");

                // Build channelId → logoUrl map
                Map<String, String> channelLogos = new HashMap<>();
                for (int i = 0; i < logosArray.length(); i++) {
                    JSONObject logoObj = logosArray.getJSONObject(i);
                    String chId = logoObj.optString("channel", "");
                    String urlLogo = logoObj.optString("url", "");
                    if (!chId.isEmpty() && !urlLogo.isEmpty() && !channelLogos.containsKey(chId)) {
                        channelLogos.put(chId, urlLogo);
                    }
                }

                // Map streams
                Map<String, List<String>> channelStreams = new HashMap<>();
                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject streamObj = streamsArray.getJSONObject(i);
                    String channelId = streamObj.isNull("channel") ? null : streamObj.optString("channel", null);
                    String urlStream = streamObj.optString("url", "");
                    if (channelId == null || urlStream.isEmpty()) continue;
                    channelStreams.computeIfAbsent(channelId, k -> new ArrayList<>()).add(urlStream);
                }

                // Group channels by country
                Map<String, List<Channel>> countryChannelsMap = new HashMap<>();
                for (int i = 0; i < channelsArray.length(); i++) {
                    JSONObject obj = channelsArray.getJSONObject(i);
                    String channelId = obj.optString("id", "");
                    String name = obj.optString("name", "Unnamed");
                    String countryCode = obj.optString("country", "");
                    JSONArray categories = obj.optJSONArray("categories");
                    String categoryName = (categories != null && categories.length() > 0)
                            ? categories.optString(0, "General")
                            : "General";

                    String fullCountryName = new Locale("", countryCode).getDisplayCountry(Locale.ENGLISH);
                    if (fullCountryName.isEmpty()) continue;

                    Country country = countryDao.getByName(fullCountryName);
                    Category category = categoryDao.getByName(
                            categoryName.substring(0,1).toUpperCase() + categoryName.substring(1).toLowerCase()
                    );
                    if (country == null || category == null) continue;

                    String logo = channelLogos.getOrDefault(channelId, obj.optString("logo", ""));

                    List<ChannelServer> servers = new ArrayList<>();
                    List<String> urls = channelStreams.get(channelId);
                    if (urls != null) {
                        for (int j = 0; j < urls.size(); j++) {
                            servers.add(new ChannelServer(0, "Server " + (j+1), urls.get(j)));
                        }
                    }

                    Channel channel = new Channel(name, logo, country.getId(), category.getId(), servers);
                    countryChannelsMap.computeIfAbsent(fullCountryName, k -> new ArrayList<>()).add(channel);
                }

                // Insert limited channels per country
                for (Map.Entry<String, List<Channel>> entry : countryChannelsMap.entrySet()) {
                    String countryName = entry.getKey();
                    List<Channel> channels = entry.getValue();

                    int limit = (maxChannelsPerCountry <= 0 || maxChannelsPerCountry > channels.size())
                            ? channels.size() : maxChannelsPerCountry;

                    for (int i = 0; i < limit; i++) {
                        Channel ch = channels.get(i);
                        long insertedId = channelDao.insert(ch);
                        for (ChannelServer cs : ch.getServers()) {
                            cs.setChannelId((int) insertedId);
                            channelServerDao.insert(cs);
                        }
                    }

                    Log.i("IPTV", "Inserted " + limit + " channels for " + countryName);
                }

                new Handler(Looper.getMainLooper()).post(onComplete);

            } catch (Exception e) {
                Log.e("IPTV", "Error fetching channels", e);
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }).start();
    }


    private static JSONArray fetchJsonArray(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "IPTV-App/1.0 (Android)");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(40000);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return new JSONArray(sb.toString());
        }
    }
}


