package com.example.iptv;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Play a link": works out whether a URL is a single stream (HLS playlist, .ts, .mp4 ...)
 * or an M3U playlist of channels, and parses the playlist.
 */
public final class M3uLink {

    /** One channel from an M3U playlist. */
    public static class Entry {
        public String name, logo, group, url, userAgent, referrer;
    }

    /** Result of opening a link. */
    public static class Result {
        /** The link itself is a stream: play it directly. */
        public boolean isStream;
        /** Channels when the link is a playlist. */
        public final List<Entry> entries = new ArrayList<>();
    }

    private static final Pattern ATTR = Pattern.compile("([a-zA-Z-]+)=\"([^\"]*)\"");
    private static final int PEEK_BYTES = 64 * 1024;
    private static final int MAX_ENTRIES = 50_000;
    private static final String PREFS = "play_link";
    private static final String KEY_RECENT = "recent";

    private M3uLink() {}

    /** Blocking: off the main thread. */
    public static Result open(String link) throws IOException {
        Result result = new Result();
        String lower = link.toLowerCase(Locale.ROOT);
        // Obvious media files: no need to download anything first
        if (lower.matches(".*\\.(ts|mp4|mkv|webm|mpd|mp3|aac)(\\?.*)?$")) {
            result.isStream = true;
            return result;
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(link).openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        try {
            int code = conn.getResponseCode();
            if (code >= 400) throw new IOException("HTTP " + code);
            String type = conn.getContentType() == null ? "" : conn.getContentType().toLowerCase(Locale.ROOT);
            if (type.startsWith("video/") || type.startsWith("audio/") || type.contains("mp2t") || type.contains("dash")) {
                result.isStream = true;
                return result;
            }
            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), PEEK_BYTES)) {
                parse(reader, link, result);
            }
        } finally {
            conn.disconnect();
        }
        return result;
    }

    /**
     * HLS media / master playlists (#EXT-X-… tags) are streams; anything with #EXTINF entries
     * pointing to URLs is a channel playlist. Unknown content is tried as a stream.
     */
    static void parse(BufferedReader reader, String baseUrl, Result result) throws IOException {
        String line;
        Entry pending = null;
        int lines = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.charAt(0) == '﻿') line = line.substring(1);
            lines++;
            if (line.startsWith("#EXT-X-")) {
                // HLS: the link itself is the stream
                result.isStream = true;
                result.entries.clear();
                return;
            }
            if (line.startsWith("#EXTINF")) {
                pending = new Entry();
                Map<String, String> attrs = new HashMap<>();
                Matcher m = ATTR.matcher(line);
                while (m.find()) attrs.put(m.group(1).toLowerCase(Locale.ROOT), m.group(2));
                int comma = line.lastIndexOf(',');
                pending.name = comma >= 0 ? line.substring(comma + 1).trim() : "";
                if (pending.name.isEmpty()) pending.name = attrs.get("tvg-name");
                pending.logo = attrs.get("tvg-logo");
                pending.group = attrs.get("group-title");
            } else if (line.startsWith("#EXTVLCOPT:") && pending != null) {
                // VLC-style headers some playlists need
                String opt = line.substring("#EXTVLCOPT:".length());
                if (opt.startsWith("http-user-agent=")) pending.userAgent = opt.substring(16);
                else if (opt.startsWith("http-referrer=")) pending.referrer = opt.substring(14);
            } else if (!line.startsWith("#")) {
                if (pending != null) {
                    pending.url = resolve(baseUrl, line);
                    if (pending.name == null || pending.name.isEmpty()) pending.name = pending.url;
                    result.entries.add(pending);
                    pending = null;
                    if (result.entries.size() >= MAX_ENTRIES) break;
                } else if (lines <= 2 && looksLikeUrl(line)) {
                    // A bare URL list: treat the first URL as the entry
                    Entry e = new Entry();
                    e.url = resolve(baseUrl, line);
                    e.name = e.url;
                    result.entries.add(e);
                }
            }
        }
        if (result.entries.isEmpty()) result.isStream = true;
    }

    private static boolean looksLikeUrl(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return (l.startsWith("http://") || l.startsWith("https://") || l.startsWith("rtmp://"))
                && !line.contains(" ") && !line.contains("<");
    }

    private static String resolve(String base, String url) {
        try {
            return new URL(new URL(base), url).toString();
        } catch (Exception e) {
            return url;
        }
    }

    // ───────────── Recent links ─────────────

    public static List<String> recentLinks(Context context) {
        String raw = prefs(context).getString(KEY_RECENT, "");
        List<String> list = new ArrayList<>();
        for (String s : raw.split("\n")) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    public static void remember(Context context, String link) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(link);
        set.addAll(recentLinks(context));
        List<String> list = new ArrayList<>(set);
        if (list.size() > 8) list = list.subList(0, 8);
        prefs(context).edit().putString(KEY_RECENT, android.text.TextUtils.join("\n", list)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Characters that make the playlist cache file ambiguous. */
    static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /** Tab-separated cache of a parsed playlist (too large for an Intent). */
    public static void save(Context context, List<Entry> entries) throws IOException {
        java.io.File file = new java.io.File(context.getCacheDir(), "playlist.tsv");
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), StandardCharsets.UTF_8)) {
            for (Entry e : entries) {
                w.write(clean(e.name) + '\t' + clean(e.logo) + '\t' + clean(e.group) + '\t' + clean(e.url)
                        + '\t' + clean(e.userAgent) + '\t' + clean(e.referrer) + '\n');
            }
        }
    }

    public static List<Entry> load(Context context) {
        List<Entry> entries = new ArrayList<>();
        java.io.File file = new java.io.File(context.getCacheDir(), "playlist.tsv");
        if (!file.exists()) return entries;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = Arrays.copyOf(line.split("\t", -1), 6);
                Entry e = new Entry();
                e.name = p[0];
                e.logo = p[1] == null || p[1].isEmpty() ? null : p[1];
                e.group = p[2];
                e.url = p[3];
                e.userAgent = p[4] == null || p[4].isEmpty() ? null : p[4];
                e.referrer = p[5] == null || p[5].isEmpty() ? null : p[5];
                if (e.url != null && !e.url.isEmpty()) entries.add(e);
            }
        } catch (IOException ignored) {
        }
        return entries;
    }
}
