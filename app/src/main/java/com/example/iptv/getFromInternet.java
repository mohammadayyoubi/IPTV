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

//get all channels and streams method
    public static void getAllChannelsAndStreams(ChannelDAO channelDao, CountryDAO countryDao, CategoryDAO categoryDao, ChannelServerDAO channelServerDao) {
        new Thread(() -> {
            try {
                Log.d("SyncChannels", "Started syncing channels and streams...");

                // --- 1. Fetch channels.json ---
                Log.d("SyncChannels", "Fetching channels.json...");
                URL channelsUrl = new URL("https://iptv-org.github.io/api/channels.json");
                HttpURLConnection channelsConn = (HttpURLConnection) channelsUrl.openConnection();
                channelsConn.setRequestMethod("GET");
                BufferedReader channelsReader = new BufferedReader(new InputStreamReader(channelsConn.getInputStream()));
                StringBuilder channelsResponse = new StringBuilder();
                String line;
                while ((line = channelsReader.readLine()) != null) {
                    channelsResponse.append(line);
                }
                channelsReader.close();
                JSONArray channelsArray = new JSONArray(channelsResponse.toString());
                Log.d("SyncChannels", "Fetched " + channelsArray.length() + " channels.");

                // --- 2. Fetch streams.json ---
                Log.d("SyncChannels", "Fetching streams.json...");
                URL streamsUrl = new URL("https://iptv-org.github.io/api/streams.json");
                HttpURLConnection streamsConn = (HttpURLConnection) streamsUrl.openConnection();
                streamsConn.setRequestMethod("GET");
                BufferedReader streamsReader = new BufferedReader(new InputStreamReader(streamsConn.getInputStream()));
                StringBuilder streamsResponse = new StringBuilder();
                while ((line = streamsReader.readLine()) != null) {
                    streamsResponse.append(line);
                }
                streamsReader.close();
                JSONArray streamsArray = new JSONArray(streamsResponse.toString());
                Log.d("SyncChannels", "Fetched " + streamsArray.length() + " streams.");

                // --- 3. Map channelId -> List of stream URLs ---
                Map<String, List<String>> channelStreams = new HashMap<>();
                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject stream = streamsArray.getJSONObject(i);
                    String channelId = stream.getString("channel");
                    String url = stream.getString("url");

                    if (!channelStreams.containsKey(channelId)) {
                        channelStreams.put(channelId, new ArrayList<>());
                    }
                    channelStreams.get(channelId).add(url);
                }
                Log.d("SyncChannels", "Mapped streams to channels. Total entries: " + channelStreams.size());

                // --- 4. Insert each channel and related servers ---
                int insertedCount = 0;
                for (int i = 0; i < channelsArray.length(); i++) {
                    JSONObject obj = channelsArray.getJSONObject(i);
                    String channelId = obj.getString("id");
                    String name = obj.optString("name", "NoName");
                    String logo = obj.optString("logo", "");
                    String countryCode = obj.optString("country", "");
                    JSONArray catArray = obj.optJSONArray("categories");
                    String categoryName = (catArray != null && catArray.length() > 0) ? catArray.getString(0) : "General";

                    // Convert ISO country code to full country name
                    String fullCountryName = new Locale("", countryCode).getDisplayCountry(Locale.ENGLISH);
                    Log.d("CountryResolve", countryCode + " → " + fullCountryName);

                    Log.d("SyncChannel", "Processing: " + name + ", Country: " + fullCountryName + ", Category: " + categoryName);

                    // Resolve country ID
                    Country country = countryDao.getByName(fullCountryName);
                    if (country == null) {
                        Log.w("SyncChannel", "Skipped '" + name + "' due to missing country: " + fullCountryName);
                        continue;
                    }
                    int countryId = country.getId();

                    // Resolve category ID
                    Category category = categoryDao.getByName(categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1).toLowerCase());
                    if (category == null) {
                        Log.w("SyncChannel", "Skipped '" + name + "' due to missing category: " + categoryName);
                        continue;
                    }
                    int categoryId = category.getId();

                    // Prepare list of channel servers
                    List<ChannelServer> servers = new ArrayList<>();
                    List<String> urls = channelStreams.get(channelId);
                    if (urls != null) {
                        for (int j = 0; j < urls.size(); j++) {
                            servers.add(new ChannelServer(0, "Server " + (j + 1), urls.get(j)));
                        }
                    }

                    // Insert channel
                    Channel channel = new Channel(name, logo, countryId, categoryId, servers);
                    long insertedId = channelDao.insert(channel);
                    Log.d("SyncChannel", "Inserted channel: " + name + " → DB ID: " + insertedId);

                    // Insert all related servers
                    for (ChannelServer cs : servers) {
                        cs.setChannelId((int) insertedId);
                        channelServerDao.insert(cs);
                    }

                    Log.d("InsertedChannel", name + " with " + servers.size() + " server(s)");
                    insertedCount++;
                }

                Log.i("SyncChannels", "Done. Inserted " + insertedCount + " channels.");

            } catch (Exception e) {
                Log.e("SyncChannels", "Error occurred during channel sync", e);
            }
        }).start();
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
public static void getAllChannelsByCountry(String countryCode, ChannelDAO channelDao, CountryDAO countryDao, CategoryDAO categoryDao, ChannelServerDAO channelServerDao, ChannelCallback callback) {
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
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONArray channelsArray = new JSONArray(response.toString());

            Log.d("IPTV", "Fetching streams.json...");
            URL streamsUrl = new URL("https://iptv-org.github.io/api/streams.json");
            HttpURLConnection streamsConn = (HttpURLConnection) streamsUrl.openConnection();
            streamsConn.setRequestMethod("GET");

            BufferedReader streamsReader = new BufferedReader(new InputStreamReader(streamsConn.getInputStream()));
            StringBuilder streamsResponse = new StringBuilder();
            while ((line = streamsReader.readLine()) != null) {
                streamsResponse.append(line);
            }
            streamsReader.close();
            JSONArray streamsArray = new JSONArray(streamsResponse.toString());

            // Build map: channel_id → list of stream URLs
            Map<String, List<String>> channelStreams = new HashMap<>();
            for (int i = 0; i < streamsArray.length(); i++) {
                JSONObject streamObj = streamsArray.getJSONObject(i);
                String channelId = streamObj.getString("channel");
                String url = streamObj.getString("url");

                if (!channelStreams.containsKey(channelId)) {
                    channelStreams.put(channelId, new ArrayList<>());
                }
                channelStreams.get(channelId).add(url);
            }

            // Now filter and insert channels with their servers
            for (int i = 0; i < channelsArray.length(); i++) {
                JSONObject obj = channelsArray.getJSONObject(i);
                String channelId = obj.getString("id");
                String name = obj.optString("name", "Unnamed");
                String logo = obj.optString("logo", "");
                String channelCountry = obj.optString("country", "");
                JSONArray categories = obj.optJSONArray("categories");
                String categoryName = (categories != null && categories.length() > 0)
                        ? categories.getString(0)
                        : "General";

                if (!channelCountry.equalsIgnoreCase(countryCode)) continue;

                String fullCountryName = new Locale("", channelCountry).getDisplayCountry(Locale.ENGLISH);
                Country country = countryDao.getByName(fullCountryName);
                if (country == null) continue;
                int countryId = country.getId();

                Category category = categoryDao.getByName(
                        categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1).toLowerCase()
                );
                if (category == null) continue;
                int categoryId = category.getId();

                // Add server list from map
                List<ChannelServer> servers = new ArrayList<>();
                List<String> urls = channelStreams.get(channelId);
                if (urls != null) {
                    for (int j = 0; j < urls.size(); j++) {
                        servers.add(new ChannelServer(0, "Server " + (j + 1), urls.get(j)));
                    }
                }

                Channel channel = new Channel(name, logo, countryId, categoryId, servers);
                long insertedId = channelDao.insert(channel);

                for (ChannelServer cs : servers) {
                    cs.setChannelId((int) insertedId);
                    channelServerDao.insert(cs);
                }

                filteredChannels.add(channel);
                Log.d("InsertedChannel", name + " with " + servers.size() + " server(s)");
            }

            Log.i("IPTV", "Saved " + filteredChannels.size() + " channels for " + countryCode);

            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onChannelsFetched(filteredChannels);
            });

        } catch (Exception e) {
            Log.e("IPTV", "Error in getAllChannelsByCountry", e);
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onChannelsFetched(new ArrayList<>());
            });
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
                Log.d("IPTV", "Fetching channels.json and streams.json once...");

                // Fetch channels.json
                URL channelsUrl = new URL("https://iptv-org.github.io/api/channels.json");
                HttpURLConnection conn = (HttpURLConnection) channelsUrl.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONArray channelsArray = new JSONArray(response.toString());

                // Fetch streams.json
                URL streamsUrl = new URL("https://iptv-org.github.io/api/streams.json");
                HttpURLConnection streamsConn = (HttpURLConnection) streamsUrl.openConnection();
                streamsConn.setRequestMethod("GET");
                BufferedReader streamsReader = new BufferedReader(new InputStreamReader(streamsConn.getInputStream()));
                StringBuilder streamsResponse = new StringBuilder();
                while ((line = streamsReader.readLine()) != null) {
                    streamsResponse.append(line);
                }
                streamsReader.close();
                JSONArray streamsArray = new JSONArray(streamsResponse.toString());

                // Map channelId -> List of stream URLs
                Map<String, List<String>> channelStreams = new HashMap<>();
                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject streamObj = streamsArray.getJSONObject(i);
                    String channelId = streamObj.getString("channel");
                    String url = streamObj.getString("url");

                    channelStreams.computeIfAbsent(channelId, k -> new ArrayList<>()).add(url);
                }

                // Group channels by country
                Map<String, List<Channel>> countryChannelsMap = new HashMap<>();
                for (int i = 0; i < channelsArray.length(); i++) {
                    JSONObject obj = channelsArray.getJSONObject(i);
                    String channelId = obj.getString("id");
                    String name = obj.optString("name", "Unnamed");
                    String logo = obj.optString("logo", "");
                    String countryCode = obj.optString("country", "");
                    JSONArray categories = obj.optJSONArray("categories");
                    String categoryName = (categories != null && categories.length() > 0)
                            ? categories.getString(0)
                            : "General";

                    String fullCountryName = new Locale("", countryCode).getDisplayCountry(Locale.ENGLISH);
                    if (fullCountryName.isEmpty()) continue;

                    Country country = countryDao.getByName(fullCountryName);
                    Category category = categoryDao.getByName(categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1).toLowerCase());

                    if (country == null || category == null) continue;

                    List<ChannelServer> servers = new ArrayList<>();
                    List<String> urls = channelStreams.get(channelId);
                    if (urls != null) {
                        for (int j = 0; j < urls.size(); j++) {
                            servers.add(new ChannelServer(0, "Server " + (j + 1), urls.get(j)));
                        }
                    }

                    Channel channel = new Channel(name, logo, country.getId(), category.getId(), servers);
                    countryChannelsMap.computeIfAbsent(fullCountryName, k -> new ArrayList<>()).add(channel);
                }

                // Insert limited channels per country
                for (Map.Entry<String, List<Channel>> entry : countryChannelsMap.entrySet()) {
                    String countryName = entry.getKey();
                    List<Channel> channels = entry.getValue();

                    int limit;
                    if (maxChannelsPerCountry <= 0) {
                        // No limit specified, load all channels
                        limit = channels.size();
                    } else if (channels.size() > maxChannelsPerCountry) {
                        // Apply the specified limit
                        limit = maxChannelsPerCountry;
                    } else {
                        // Load all available channels if fewer than the limit
                        limit = channels.size();
                    }

                    List<Channel> limitedChannels = channels.subList(0, limit);

                    for (Channel ch : limitedChannels) {
                        long insertedId = channelDao.insert(ch);
                        for (ChannelServer cs : ch.getServers()) {
                            cs.setChannelId((int) insertedId);
                            channelServerDao.insert(cs);
                        }
                    }

                    Log.i("IPTV", "Inserted " + limit + " channels for " + countryName);
                }

                // Notify completion on main thread
                new Handler(Looper.getMainLooper()).post(onComplete);

            } catch (Exception e) {
                Log.e("IPTV", "Error fetching channels", e);
            }
        }).start();
    }


}


