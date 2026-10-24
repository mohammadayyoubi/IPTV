package com.example.iptv;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Downloads and merges the channel catalogue from:
 * <ul>
 *   <li><b>iptv-org</b> (https://github.com/iptv-org/api): channels.json, streams.json and
 *       logos.json. Logos live in their own file; channels.json has no logo field any more.</li>
 *   <li><b>Free-TV/IPTV</b> (https://github.com/Free-TV/IPTV): a curated playlist of channels
 *       that broadcasters provide for free. Its tvg-id values are iptv-org channel ids, so its
 *       URLs are added as extra servers of the same channel.</li>
 * </ul>
 */
public class Catalog {

    private static final String TAG = "Catalog";
    static final String CHANNELS_URL = "https://iptv-org.github.io/api/channels.json";
    static final String STREAMS_URL = "https://iptv-org.github.io/api/streams.json";
    static final String LOGOS_URL = "https://iptv-org.github.io/api/logos.json";
    static final String FREE_TV_URL = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8";

    /** One playable URL plus the headers it needs. */
    public static class Stream {
        public final String url;
        public final String quality;
        public final String userAgent;
        public final String referrer;
        /** Set to ChannelServer.STATUS_WORKING when the import tested this stream. */
        public int status;

        Stream(String url, String quality, String userAgent, String referrer) {
            this.url = url;
            this.quality = quality;
            this.userAgent = userAgent;
            this.referrer = referrer;
        }
    }

    /** A channel ready to be saved: names are already resolved, streams de-duplicated. */
    public static class Entry {
        public String sourceId;      // iptv-org id, e.g. "BBCOne.uk"
        public String name;
        public String countryCode;   // ISO 3166-1 alpha-2, upper case
        public String categoryName;  // iptv-org category id, e.g. "news"
        public String logoUrl = "";
        public final List<Stream> streams = new ArrayList<>();
        private final Set<String> urls = new HashSet<>();

        void addStream(Stream stream) {
            if (urls.add(stream.url)) streams.add(stream);
        }
    }

    /** Download progress: {@code fraction} goes from 0 to 1 over all the files of a load. */
    public interface Progress {
        void onProgress(String status, double fraction);
    }

    private static final Progress NO_PROGRESS = (status, fraction) -> { };

    /** Splits one overall 0..1 progress over several files, by weight. */
    private static class Steps {
        private final Progress progress;
        private final double[] weights;
        private double done;
        private int index = -1;

        Steps(Progress progress, double... weights) {
            this.progress = progress;
            this.weights = weights;
        }

        /** Starts the next file; returns a reporter for that file's own 0..1. */
        FileProgress next(String status) {
            if (index >= 0) done += weights[index];
            index++;
            final double base = done;
            final double weight = weights[index];
            progress.onProgress(status, base);
            return f -> progress.onProgress(status, base + weight * Math.min(1, f));
        }

        void finish(String status) {
            progress.onProgress(status, 1);
        }
    }

    interface FileProgress {
        void onFraction(double fraction);
    }

    /** Fetches every source and returns the merged catalogue. Blocking: call off the main thread. */
    public static List<Entry> load() throws IOException {
        return load(NO_PROGRESS);
    }

    public static List<Entry> load(Progress progress) throws IOException {
        // Rough share of the download time per file
        Steps steps = new Steps(progress, 0.32, 0.22, 0.26, 0.20);
        Map<String, Entry> entries = loadIptvOrgChannels(steps.next("Downloading channels…"));
        Log.d(TAG, "iptv-org channels: " + entries.size());

        Map<String, String> logos = loadLogos(steps.next("Downloading logos…"));
        for (Entry entry : entries.values()) {
            String logo = logos.get(entry.sourceId);
            if (logo != null) entry.logoUrl = logo;
        }

        int linked = loadIptvOrgStreams(entries, steps.next("Downloading streams…"));
        Log.d(TAG, "iptv-org streams linked to channels: " + linked);

        try {
            int added = mergeFreeTv(entries, steps.next("Downloading Free-TV playlist…"));
            Log.d(TAG, "Free-TV streams merged: " + added);
        } catch (IOException e) {
            // The extra source is optional; iptv-org alone is still a full catalogue
            Log.w(TAG, "Free-TV playlist unavailable, continuing without it", e);
        }
        steps.finish("Catalogue downloaded");
        return new ArrayList<>(entries.values());
    }

    /** iptv-org channel id → best logo URL, without downloading channels or streams. */
    public static Map<String, String> loadLogoMap(Progress progress) throws IOException {
        Steps steps = new Steps(progress, 1.0);
        Map<String, String> logos = loadLogos(steps.next("Downloading logos…"));
        steps.finish("Logos downloaded");
        return logos;
    }

    /** Channels (without streams) and their logos only: enough to repair missing logos. */
    public static List<Entry> loadLogosOnly(Progress progress) throws IOException {
        Steps steps = new Steps(progress, 0.6, 0.4);
        Map<String, Entry> entries = loadIptvOrgChannels(steps.next("Downloading channels…"));
        Map<String, String> logos = loadLogos(steps.next("Downloading logos…"));
        steps.finish("Logos downloaded");
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries.values()) {
            String logo = logos.get(entry.sourceId);
            if (logo != null) {
                entry.logoUrl = logo;
                result.add(entry);
            }
        }
        return result;
    }

    // ───────────────────────── iptv-org ─────────────────────────

    private static Map<String, Entry> loadIptvOrgChannels(FileProgress fp) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (JsonReader reader = open(CHANNELS_URL, fp)) {
            reader.beginArray();
            while (reader.hasNext()) {
                Entry entry = new Entry();
                boolean nsfw = false, closed = false;
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                        case "id": entry.sourceId = nextStringOrNull(reader); break;
                        case "name": entry.name = nextStringOrNull(reader); break;
                        case "country": entry.countryCode = upper(nextStringOrNull(reader)); break;
                        case "categories": entry.categoryName = firstString(reader); break;
                        case "is_nsfw": nsfw = reader.peek() == JsonToken.BOOLEAN ? reader.nextBoolean() : skip(reader); break;
                        case "closed": closed = nextStringOrNull(reader) != null; break;
                        default: reader.skipValue();
                    }
                }
                reader.endObject();
                // Skip adult channels and channels that have shut down
                if (entry.sourceId == null || entry.name == null || nsfw || closed) continue;
                entries.put(entry.sourceId, entry);
            }
            reader.endArray();
        }
        return entries;
    }

    /** channel id → best logo URL: in use, channel-wide (no feed), raster format, largest. */
    private static Map<String, String> loadLogos(FileProgress fp) throws IOException {
        Map<String, String> best = new HashMap<>();
        Map<String, Integer> bestScore = new HashMap<>();
        try (JsonReader reader = open(LOGOS_URL, fp)) {
            reader.beginArray();
            while (reader.hasNext()) {
                String channel = null, feed = null, format = null, url = null;
                boolean inUse = true;
                int width = 0;
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                        case "channel": channel = nextStringOrNull(reader); break;
                        case "feed": feed = nextStringOrNull(reader); break;
                        case "format": format = nextStringOrNull(reader); break;
                        case "url": url = nextStringOrNull(reader); break;
                        case "in_use": inUse = reader.peek() != JsonToken.BOOLEAN || reader.nextBoolean(); break;
                        case "width": width = reader.peek() == JsonToken.NUMBER ? reader.nextInt() : skipInt(reader); break;
                        default: reader.skipValue();
                    }
                }
                reader.endObject();
                if (channel == null || url == null) continue;
                boolean svg = "SVG".equalsIgnoreCase(format) || url.toLowerCase(Locale.ROOT).endsWith(".svg");
                if (svg) continue; // Glide can't decode SVG

                int score = (inUse ? 100_000 : 0) + (feed == null ? 50_000 : 0) + Math.min(width, 4000);
                Integer current = bestScore.get(channel);
                if (current == null || score > current) {
                    bestScore.put(channel, score);
                    best.put(channel, url);
                }
            }
            reader.endArray();
        }
        return best;
    }

    private static int loadIptvOrgStreams(Map<String, Entry> entries, FileProgress fp) throws IOException {
        int linked = 0;
        try (JsonReader reader = open(STREAMS_URL, fp)) {
            reader.beginArray();
            while (reader.hasNext()) {
                String channel = null, url = null, quality = null, ua = null, referrer = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                        case "channel": channel = nextStringOrNull(reader); break;
                        case "url": url = nextStringOrNull(reader); break;
                        case "quality": quality = nextStringOrNull(reader); break;
                        case "user_agent": ua = nextStringOrNull(reader); break;
                        case "referrer": referrer = nextStringOrNull(reader); break;
                        default: reader.skipValue();
                    }
                }
                reader.endObject();
                // Streams with "channel": null can't be placed under a country / category
                Entry entry = channel == null ? null : entries.get(channel);
                if (entry == null || url == null || url.isEmpty()) continue;
                entry.addStream(new Stream(url, quality, ua, referrer));
                linked++;
            }
            reader.endArray();
        }
        return linked;
    }

    // ───────────────────────── Free-TV ─────────────────────────

    private static final Pattern ATTR = Pattern.compile("([a-z-]+)=\"([^\"]*)\"");
    // Free-TV marks entries with circled letters (Ⓖ geo-blocked, Ⓨ YouTube, Ⓣ Twitch, ...)
    private static final Pattern MARKERS = Pattern.compile("[\\u24B6-\\u24E9]");

    private static int mergeFreeTv(Map<String, Entry> entries, FileProgress fp) throws IOException {
        int added = 0;
        HttpURLConnection conn = connect(FREE_TV_URL);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body(conn, fp), StandardCharsets.UTF_8))) {
            String line;
            Map<String, String> attrs = null;
            String title = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF")) {
                    attrs = new HashMap<>();
                    Matcher m = ATTR.matcher(line);
                    while (m.find()) attrs.put(m.group(1), m.group(2));
                    int comma = line.lastIndexOf(',');
                    title = comma >= 0 ? MARKERS.matcher(line.substring(comma + 1)).replaceAll("").trim() : null;
                } else if (!line.isEmpty() && !line.startsWith("#") && attrs != null) {
                    if (isDirectStream(line)) {
                        added += addFreeTvStream(entries, attrs, title, line) ? 1 : 0;
                    }
                    attrs = null;
                    title = null;
                }
            }
        } finally {
            conn.disconnect();
        }
        return added;
    }

    private static boolean addFreeTvStream(Map<String, Entry> entries, Map<String, String> attrs,
                                           String title, String url) {
        String group = attrs.get("group-title");
        // "VOD ..." groups are on-demand videos, not live channels
        if (group != null && group.toUpperCase(Locale.ROOT).startsWith("VOD")) return false;
        String id = attrs.get("tvg-id");
        Entry entry = id == null || id.isEmpty() ? null : entries.get(id);
        if (entry == null) {
            // A channel iptv-org doesn't list: keep it if we know its country
            String country = upper(attrs.get("tvg-country"));
            // Some entries only name their country in the group ("Lebanon")
            if ((country == null || country.isEmpty()) && group != null) {
                country = isoForCountryName(group);
            }
            String name = attrs.get("tvg-name") != null && !attrs.get("tvg-name").isEmpty()
                    ? attrs.get("tvg-name") : title;
            if (country == null || country.length() != 2 || name == null || name.isEmpty()) return false;
            String key = id != null && !id.isEmpty() ? id : "freetv:" + country + ":" + name.toLowerCase(Locale.ROOT);
            entry = entries.get(key);
            if (entry == null) {
                entry = new Entry();
                entry.sourceId = key;
                entry.name = name;
                entry.countryCode = country;
                entry.categoryName = "general";
                entries.put(key, entry);
            }
        }
        String logo = attrs.get("tvg-logo");
        if (entry.logoUrl.isEmpty() && logo != null && !logo.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            entry.logoUrl = logo;
        }
        int before = entry.streams.size();
        entry.addStream(new Stream(url, null, null, null));
        return entry.streams.size() > before;
    }

    private static Map<String, String> isoByName;

    private static synchronized String isoForCountryName(String name) {
        if (isoByName == null) {
            isoByName = new HashMap<>();
            for (String iso : Locale.getISOCountries()) {
                isoByName.put(new Locale("", iso).getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT), iso);
            }
        }
        return isoByName.get(name.trim().toLowerCase(Locale.ROOT));
    }

    /** Free-TV also lists YouTube / Twitch pages, which ExoPlayer can't play. */
    private static boolean isDirectStream(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        if (!u.startsWith("http://") && !u.startsWith("https://")) return false;
        return !(u.contains("youtube.com") || u.contains("youtu.be") || u.contains("twitch.tv")
                || u.contains("dailymotion.com/video") || u.contains("facebook.com"));
    }

    // ───────────────────────── helpers ─────────────────────────

    private static HttpURLConnection connect(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        // Asking for gzip ourselves keeps Content-Length meaningful for the progress bar
        // (HttpURLConnection would otherwise decompress silently and hide the real size)
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(60_000);
        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        return conn;
    }

    private static JsonReader open(String url, FileProgress fp) throws IOException {
        return new JsonReader(new InputStreamReader(body(connect(url), fp), StandardCharsets.UTF_8));
    }

    /** Response body that reports how much of it has arrived, decompressed when gzip. */
    private static InputStream body(HttpURLConnection conn, FileProgress fp) throws IOException {
        long length = conn.getContentLengthLong();
        InputStream counted = new CountingInputStream(conn.getInputStream(), length, fp);
        if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
            return new GZIPInputStream(counted, 64 * 1024);
        }
        return counted;
    }

    /** Counts bytes as they are read and reports the fraction of Content-Length received. */
    private static class CountingInputStream extends FilterInputStream {
        private final long length;
        private final FileProgress fp;
        private long count;
        private int lastPermille = -1;

        CountingInputStream(InputStream in, long length, FileProgress fp) {
            super(in);
            this.length = length;
            this.fp = fp;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) advance(1);
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int len) throws IOException {
            int n = super.read(buffer, offset, len);
            if (n > 0) advance(n);
            return n;
        }

        private void advance(long n) {
            count += n;
            if (length <= 0 || fp == null) return;
            int permille = (int) (count * 1000 / length);
            if (permille - lastPermille >= 5) { // report every 0.5%
                lastPermille = permille;
                fp.onFraction(count / (double) length);
            }
        }
    }

    private static String nextStringOrNull(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.NUMBER) {
            return reader.nextString();
        }
        reader.skipValue();
        return null;
    }

    private static String firstString(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue();
            return null;
        }
        String first = null;
        reader.beginArray();
        while (reader.hasNext()) {
            String value = nextStringOrNull(reader);
            if (first == null) first = value;
        }
        reader.endArray();
        return first;
    }

    private static boolean skip(JsonReader reader) throws IOException {
        reader.skipValue();
        return false;
    }

    private static int skipInt(JsonReader reader) throws IOException {
        reader.skipValue();
        return 0;
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }
}
