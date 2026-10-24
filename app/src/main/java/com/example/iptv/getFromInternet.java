package com.example.iptv;

import android.os.Looper;

import com.example.iptv.Interfaces.CategoryCallback;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
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

    /** Progress and result of a long admin task, delivered on the main thread. */
    public interface ImportCallback {
        /** @param percent overall completion 0..100, or -1 when unknown */
        void onProgress(String status, int percent);
        void onDone(ImportOptions.Result result);
        void onError(String message);
    }

    /** The DAOs a task works with (all on the same database connection). */
    public static class Daos {
        final android.content.Context context;
        final ChannelDAO channels;
        final CountryDAO countries;
        final CategoryDAO categories;
        final ChannelServerDAO servers;

        public Daos(android.content.Context context, ChannelDAO channels, CountryDAO countries,
                    CategoryDAO categories, ChannelServerDAO servers) {
            this.context = context.getApplicationContext();
            this.channels = channels;
            this.countries = countries;
            this.categories = categories;
            this.servers = servers;
        }
    }

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

    /** Blocking download of iptv-org categories.json. */
    private static ArrayList<Category> fetchCategories() throws Exception {
        ArrayList<Category> categoryList = new ArrayList<>();
        HttpURLConnection conn = (HttpURLConnection) new URL("https://iptv-org.github.io/api/categories.json").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        } finally {
            conn.disconnect();
        }
        JSONArray jsonArray = new JSONArray(response.toString());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject categoryObject = jsonArray.getJSONObject(i);
            categoryList.add(new Category(categoryObject.getString("name")));
        }
        return categoryList;
    }

//get all categories method
    public static void getAllCategories(CategoryCallback callback) {
        new Thread(() -> {
            ArrayList<Category> categoryList = new ArrayList<>();
            try {
                categoryList = fetchCategories();
            } catch (Exception e) {
                // Deliver an empty list so the caller can stop its progress indicator
                Log.e("Categories", "Failed to load categories", e);
            }
            ArrayList<Category> result = categoryList;
            new Handler(Looper.getMainLooper()).post(() -> callback.onCategoriesLoaded(result));
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

    // ───────────────────────── Import / update ─────────────────────────

    /**
     * Imports channels from iptv-org + Free-TV (see {@link Catalog}) in the background.
     * Re-importing never duplicates: existing channels only get the servers they're missing,
     * a missing logo, and (when {@code updateLogos}) a logo that changed at the source.
     */
    public static void importChannels(ImportOptions options, Daos daos, ImportCallback callback) {
        runInBackground(callback, progress -> runImport(options, daos, progress));
    }

    /**
     * "Update database": adds missing categories and countries, then imports new channels,
     * servers and logos for the chosen scope (by default: countries already in the database).
     */
    public static void updateDatabase(ImportOptions options, Daos daos, ImportCallback callback) {
        runInBackground(callback, progress -> {
            ImportOptions.Result result = new ImportOptions.Result();
            progress.update("Updating categories and countries…", 0);
            syncCategoriesAndCountries(daos, result);
            progress.update("Categories and countries updated", 5);

            if (options.countryCodes != null && options.countryCodes.isEmpty()) {
                // "My countries" with an empty database: nothing to update
                return result;
            }
            ImportOptions.Result channels = runImport(options, daos, progress.scaled(5, 100));
            channels.newCategories = result.newCategories;
            channels.newCountries = result.newCountries;
            return channels;
        });
    }

    /** Adds every iptv-org category missing locally (Categories page → Load all). */
    public static void loadAllCategories(Daos daos, ImportCallback callback) {
        runInBackground(callback, progress -> {
            progress.update("Downloading categories…", 10);
            ImportOptions.Result result = new ImportOptions.Result();
            Set<String> have = new HashSet<>();
            for (Category c : daos.categories.getAll()) have.add(c.getName().toLowerCase(Locale.ROOT));
            for (Category c : fetchCategories()) {
                if (have.add(c.getName().toLowerCase(Locale.ROOT))) {
                    daos.categories.insert(c);
                    result.newCategories++;
                }
            }
            return result;
        });
    }

    /** Adds every country missing locally, with its flag (Countries page → Load all). */
    public static int loadAllCountries(Daos daos) {
        Set<String> have = new HashSet<>();
        for (Country c : daos.countries.getAll()) have.add(c.getName().toLowerCase(Locale.ROOT));
        int added = 0;
        for (Country c : getAllCountries()) {
            if (have.add(c.getName().toLowerCase(Locale.ROOT))) {
                daos.countries.insert(c);
                added++;
            }
        }
        return added;
    }

    /** ISO codes of the countries that already have channels. */
    public static Set<String> countryCodesInUse(Daos daos) {
        Map<String, String> isoByName = new HashMap<>();
        for (String iso : Locale.getISOCountries()) {
            isoByName.put(new Locale("", iso).getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT), iso);
        }
        Set<String> codes = new HashSet<>();
        for (int id : daos.channels.getCountryIdsInUse()) {
            Country country = daos.countries.getById(id);
            if (country == null) continue;
            String iso = isoByName.get(country.getName().toLowerCase(Locale.ROOT));
            if (iso != null) codes.add(iso);
        }
        return codes;
    }

    private static void syncCategoriesAndCountries(Daos daos, ImportOptions.Result result) throws Exception {
        Set<String> haveCategories = new HashSet<>();
        for (Category c : daos.categories.getAll()) haveCategories.add(c.getName().toLowerCase(Locale.ROOT));
        for (Category c : fetchCategories()) {
            if (haveCategories.add(c.getName().toLowerCase(Locale.ROOT))) {
                daos.categories.insert(c);
                result.newCategories++;
            }
        }
        Set<String> haveCountries = new HashSet<>();
        for (Country c : daos.countries.getAll()) haveCountries.add(c.getName().toLowerCase(Locale.ROOT));
        for (Country c : getAllCountries()) {
            if (haveCountries.add(c.getName().toLowerCase(Locale.ROOT))) {
                daos.countries.insert(c);
                result.newCountries++;
            }
        }
    }

    private static ImportOptions.Result runImport(ImportOptions options, Daos daos, ProgressSink progress) throws Exception {
        boolean testing = options.serverFilter == ImportOptions.ServerFilter.WORKING;
        // Share of the bar per phase: download, (stream test), save
        ProgressSink download = progress.scaled(0, testing ? 20 : 40);
        ProgressSink test = progress.scaled(20, 80);
        ProgressSink saving = progress.scaled(testing ? 80 : 40, 100);

        download.update("Downloading channel catalogue…", 0);
        List<Catalog.Entry> entries = Catalog.load((status, fraction) -> download.update(status, (int) (fraction * 100)));

        // Scope: one country, a set of countries, or everything
        List<Catalog.Entry> scoped = new ArrayList<>();
        for (Catalog.Entry entry : entries) {
            if (entry.countryCode == null) continue;
            if (options.countryCode != null && !entry.countryCode.equalsIgnoreCase(options.countryCode)) continue;
            if (options.countryCodes != null && !options.countryCodes.contains(entry.countryCode)) continue;
            scoped.add(entry);
        }

        // Server filter
        if (options.serverFilter == ImportOptions.ServerFilter.WORKING) {
            List<Catalog.Stream> all = new ArrayList<>();
            for (Catalog.Entry e : scoped) all.addAll(e.streams);
            NumberFormat nf = NumberFormat.getInstance();
            Set<String> working = StreamChecker.checkStreams(daos.context, all, new StreamChecker.Progress() {
                @Override
                public void onProgress(int done, int total) {
                    test.update("Testing streams… " + nf.format(done) + " / " + nf.format(total),
                            total == 0 ? 100 : (int) (done * 100L / total));
                }

                @Override
                public void onPaused(String reason) {
                    test.update("Paused: " + reason + ". Waiting for the connection…", -1);
                }
            });
            for (Catalog.Entry e : scoped) {
                e.streams.removeIf(s -> !working.contains(s.url));
                for (Catalog.Stream s : e.streams) s.status = ChannelServer.STATUS_WORKING;
            }
        }
        if (options.serverFilter != ImportOptions.ServerFilter.ALL) {
            scoped.removeIf(e -> e.streams.isEmpty());
        }

        // Cap per country, channels with streams first
        Map<String, List<Catalog.Entry>> byCountry = new HashMap<>();
        for (Catalog.Entry entry : scoped) {
            List<Catalog.Entry> list = byCountry.get(entry.countryCode);
            if (list == null) byCountry.put(entry.countryCode, list = new ArrayList<>());
            list.add(entry);
        }

        int toSave = 0;
        for (List<Catalog.Entry> list : byCountry.values()) {
            toSave += options.maxPerCountry > 0 ? Math.min(options.maxPerCountry, list.size()) : list.size();
        }
        final int totalToSave = toSave;
        saving.update("Saving channels…", 0);
        Resolver resolver = new Resolver(daos.countries, daos.categories);
        ChannelDAO.Index index = daos.channels.loadIndex();
        Map<Integer, Set<String>> urlIndex = daos.servers.loadUrlIndex();
        ImportOptions.Result result = new ImportOptions.Result();
        NumberFormat nf2 = NumberFormat.getInstance();
        int[] saved = {0};
        daos.channels.runInTransaction(() -> {
            for (List<Catalog.Entry> list : byCountry.values()) {
                list.sort((a, b) -> Boolean.compare(!b.streams.isEmpty(), !a.streams.isEmpty()));
                int limit = options.maxPerCountry > 0 ? Math.min(options.maxPerCountry, list.size()) : list.size();
                for (int i = 0; i < limit; i++) {
                    save(list.get(i), options, resolver, index, urlIndex, daos, result);
                    if (++saved[0] % 250 == 0) {
                        saving.update("Saving channels… " + nf2.format(saved[0]) + " / " + nf2.format(totalToSave),
                                (int) (saved[0] * 100L / Math.max(1, totalToSave)));
                    }
                }
            }
        });
        saving.update("Saving channels… done", 100);
        Log.i("IPTV", "Import done: " + result.summary());
        return result;
    }

    private static void save(Catalog.Entry entry, ImportOptions options, Resolver resolver,
                             ChannelDAO.Index index, Map<Integer, Set<String>> urlIndex,
                             Daos daos, ImportOptions.Result result) {
        Country country = resolver.country(entry.countryCode);
        if (country == null) return;
        Category category = resolver.category(entry.categoryName);
        if (category == null) return;

        ChannelDAO.Lite existing = index.bySource.get(entry.sourceId);
        if (existing == null) {
            // Rows imported before sourceId existed are matched by name + country
            ChannelDAO.Lite legacy = index.byNameAndCountry.get(ChannelDAO.Index.key(entry.name, country.getId()));
            if (legacy != null && legacy.sourceId == null) existing = legacy;
        }

        if (existing == null) {
            List<ChannelServer> servers = new ArrayList<>();
            for (int i = 0; i < entry.streams.size(); i++) servers.add(toServer(entry.streams.get(i), i + 1));
            Channel channel = new Channel(entry.name, entry.logoUrl, country.getId(), category.getId(), servers);
            channel.setSourceId(entry.sourceId);
            channel.setLogoAuto(true);
            long id = daos.channels.insert(channel);
            if (id == -1) return;
            Set<String> urls = new HashSet<>();
            for (ChannelServer cs : servers) {
                cs.setChannelId((int) id);
                daos.servers.insert(cs);
                urls.add(cs.getStreamUrl());
            }
            urlIndex.put((int) id, urls);
            ChannelDAO.Lite lite = new ChannelDAO.Lite();
            lite.id = (int) id;
            lite.name = entry.name;
            lite.countryId = country.getId();
            lite.logoUrl = entry.logoUrl;
            lite.sourceId = entry.sourceId;
            lite.logoAuto = true;
            index.add(lite);
            result.newChannels++;
            result.newServers += servers.size();
            return;
        }

        boolean changed = false;
        String logo = existing.logoUrl;
        boolean noLogo = logo == null || logo.isEmpty();
        boolean sourceChanged = options.updateLogos && existing.logoAuto
                && !entry.logoUrl.isEmpty() && !entry.logoUrl.equals(logo);
        if ((noLogo && !entry.logoUrl.isEmpty()) || sourceChanged) {
            logo = entry.logoUrl;
            result.logosUpdated++;
            changed = true;
        }
        if (changed || existing.sourceId == null) {
            daos.channels.updateLogoAndSource(existing.id, logo, entry.sourceId);
            existing.logoUrl = logo;
            existing.sourceId = entry.sourceId;
            existing.logoAuto = true;
            index.bySource.put(entry.sourceId, existing);
        }

        Set<String> urls = urlIndex.get(existing.id);
        if (urls == null) urlIndex.put(existing.id, urls = new HashSet<>());
        int next = urls.size() + 1;
        for (Catalog.Stream stream : entry.streams) {
            if (!urls.add(stream.url)) continue;
            ChannelServer cs = toServer(stream, next++);
            cs.setChannelId(existing.id);
            daos.servers.insert(cs);
            result.newServers++;
            changed = true;
        }
        if (changed) result.updatedChannels++;
    }

    private static ChannelServer toServer(Catalog.Stream stream, int number) {
        String name = "Server " + number;
        if (stream.quality != null && !stream.quality.isEmpty()) name += " · " + stream.quality;
        ChannelServer server = new ChannelServer(0, name, stream.url, stream.userAgent, stream.referrer);
        if (stream.status != 0) {
            server.setStatus(stream.status);
            server.setLastChecked(System.currentTimeMillis());
        }
        return server;
    }

    // ───────────────────────── Logos ─────────────────────────

    /**
     * Fills in missing logos and replaces import-managed logos that changed at the source.
     * Works from one in-memory index of the channel table and a single transaction, and only
     * downloads channels.json when there are old rows that must be matched by name.
     */
    public static void refreshLogos(Daos daos, ImportCallback callback) {
        runInBackground(callback, progress -> {
            ImportOptions.Result result = new ImportOptions.Result();
            ChannelDAO.Index index = daos.channels.loadIndex();
            ProgressSink download = progress.scaled(0, 75);
            ProgressSink saving = progress.scaled(75, 100);
            Catalog.Progress dl = (status, fraction) -> download.update(status, (int) (fraction * 100));
            download.update("Downloading logos…", 0);

            Map<String, String> logoBySource;
            Map<String, Catalog.Entry> legacyMatches = new HashMap<>();
            if (index.hasRowsWithoutSource) {
                logoBySource = new HashMap<>();
                Resolver resolver = new Resolver(daos.countries, daos.categories);
                for (Catalog.Entry e : Catalog.loadLogosOnly(dl)) {
                    logoBySource.put(e.sourceId, e.logoUrl);
                    Country country = resolver.country(e.countryCode);
                    if (country != null) legacyMatches.put(ChannelDAO.Index.key(e.name, country.getId()), e);
                }
            } else {
                logoBySource = Catalog.loadLogoMap(dl);
            }

            saving.update("Saving logos…", 0);
            List<ChannelDAO.Lite> all = index.all;
            int[] seen = {0};
            daos.channels.runInTransaction(() -> {
                for (ChannelDAO.Lite ch : all) {
                    if (++seen[0] % 500 == 0) {
                        saving.update("Saving logos…", (int) (seen[0] * 100L / Math.max(1, all.size())));
                    }
                    String sourceId = ch.sourceId;
                    String logo = sourceId != null ? logoBySource.get(sourceId) : null;
                    if (sourceId == null) {
                        Catalog.Entry match = legacyMatches.get(ChannelDAO.Index.key(ch.name, ch.countryId));
                        if (match == null) continue;
                        sourceId = match.sourceId;
                        logo = match.logoUrl;
                    }
                    if (logo == null || logo.isEmpty()) continue;
                    boolean noLogo = ch.logoUrl == null || ch.logoUrl.isEmpty();
                    // Never overwrite a logo an admin set by hand
                    if (noLogo || (ch.logoAuto && !logo.equals(ch.logoUrl))) {
                        daos.channels.updateLogoAndSource(ch.id, logo, sourceId);
                        result.logosUpdated++;
                    } else if (ch.sourceId == null) {
                        daos.channels.updateLogoAndSource(ch.id, ch.logoUrl, sourceId);
                    }
                }
            });
            return result;
        });
    }

    // ───────────────────────── plumbing ─────────────────────────

    /** Progress of one phase, mapped into its share [from, to] of the overall bar. */
    private interface ProgressSink {
        /** @param percent 0..100 within this sink, or -1 when unknown */
        void update(String status, int percent);

        default ProgressSink scaled(int from, int to) {
            ProgressSink parent = this;
            return (status, percent) -> parent.update(status,
                    percent < 0 ? -1 : from + (to - from) * Math.min(100, percent) / 100);
        }
    }

    private interface Task {
        ImportOptions.Result run(ProgressSink progress) throws Exception;
    }

    private static void runInBackground(ImportCallback callback, Task task) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                // Drop updates that wouldn't change what is shown (thousands per second otherwise)
                String[] lastStatus = {null};
                int[] lastPercent = {-2};
                ImportOptions.Result result = task.run((status, percent) -> {
                    synchronized (lastStatus) {
                        if (status.equals(lastStatus[0]) && percent == lastPercent[0]) return;
                        lastStatus[0] = status;
                        lastPercent[0] = percent;
                    }
                    main.post(() -> callback.onProgress(status, percent));
                });
                main.post(() -> callback.onDone(result));
            } catch (Exception e) {
                Log.e("IPTV", "Admin task failed", e);
                String message = e instanceof InterruptedException ? "Stopped"
                        : e instanceof IOException ? "Network error: " + e.getMessage()
                        : (e.getMessage() != null ? e.getMessage() : e.toString());
                main.post(() -> callback.onError(message));
            }
        }).start();
    }

    /** Maps ISO country codes / iptv-org category ids to local rows, with caching. */
    private static class Resolver {
        private final Map<String, Country> countriesByName = new HashMap<>();
        private final Map<String, Category> categoriesByName = new HashMap<>();
        private final Map<String, Country> countryCache = new HashMap<>();

        Resolver(CountryDAO countryDao, CategoryDAO categoryDao) {
            for (Country c : countryDao.getAll()) countriesByName.put(c.getName().toLowerCase(Locale.ROOT), c);
            for (Category c : categoryDao.getAll()) categoriesByName.put(c.getName().toLowerCase(Locale.ROOT), c);
        }

        Country country(String code) {
            if (code == null || code.isEmpty()) return null;
            if (countryCache.containsKey(code)) return countryCache.get(code);
            // "UK" is common in playlists but isn't an ISO code
            String iso = code.equalsIgnoreCase("UK") ? "GB" : code;
            String name = new Locale("", iso).getDisplayCountry(Locale.ENGLISH);
            Country country = countriesByName.get(name.toLowerCase(Locale.ROOT));
            countryCache.put(code, country);
            return country;
        }

        /** iptv-org category ids are lower-case names ("news"); unknown ones fall back to General. */
        Category category(String id) {
            Category category = id == null ? null : categoriesByName.get(id.toLowerCase(Locale.ROOT));
            return category != null ? category : categoriesByName.get("general");
        }
    }
}
