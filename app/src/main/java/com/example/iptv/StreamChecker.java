package com.example.iptv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.DBHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;

/**
 * Probes stream URLs and records whether they answer like a playable stream.
 * <ul>
 *   <li>Runs on {@link ProbeRunner}: a stream is only marked broken once the phone's own
 *       connection is proven fine, and the run pauses / resumes with the connection.</li>
 *   <li>32 probes in parallel, short timeouts, Range requests (first bytes only).</li>
 *   <li>A URL shared by several channels is probed once; results are written in batches.</li>
 *   <li>"Stale only" mode skips servers checked in the last 24 hours.</li>
 * </ul>
 * The player also records a server's status whenever it actually plays or fails.
 */
public class StreamChecker {

    public interface Listener {
        void onProgress(int done, int total);
        /** The run is waiting for the connection to come back (or get faster). */
        void onPaused(String reason);
        void onResumed();
        /** @param cancelled the admin pressed Stop; results so far are saved */
        void onComplete(int working, int broken, boolean cancelled);
    }

    /** Progress of a blocking check (called on the worker thread). */
    public interface Progress {
        void onProgress(int done, int total);
        void onPaused(String reason);
    }

    /** Servers checked within this window are skipped by a "stale only" check. */
    public static final long STALE_AFTER_MS = TimeUnit.HOURS.toMillis(24);

    private static final String TAG = "StreamChecker";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 6_000;
    private static final int MAX_REDIRECTS = 5;
    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36";

    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static boolean isRunning() {
        return running.get();
    }

    /** Stops the server check or the import's stream test that is running. */
    public static void cancel() {
        ProbeRunner.cancelActive();
    }

    /**
     * Checks stored servers in the background; callbacks arrive on the main thread.
     *
     * @param staleOnly true to skip servers checked in the last {@link #STALE_AFTER_MS}
     * @return false if a check is already running
     */
    public static boolean checkAll(Context context, boolean staleOnly, Listener listener) {
        if (!running.compareAndSet(false, true)) return false;
        Handler main = new Handler(Looper.getMainLooper());
        Context app = context.getApplicationContext();

        new Thread(() -> {
            int[] totals = new int[2];
            boolean[] cancelled = new boolean[1];
            try {
                ChannelServerDAO dao = new ChannelServerDAO(new DBHelper(app).getWritableDatabase());
                long olderThan = staleOnly ? System.currentTimeMillis() - STALE_AFTER_MS : 0;
                List<ChannelServer> servers = dao.getForCheck(olderThan);

                // Same URL + headers → probe once, apply to every server that uses it
                Map<String, List<ChannelServer>> byKey = new LinkedHashMap<>();
                for (ChannelServer s : servers) {
                    String key = key(s.getStreamUrl(), s.getUserAgent(), s.getReferrer());
                    List<ChannelServer> group = byKey.get(key);
                    if (group == null) byKey.put(key, group = new ArrayList<>());
                    group.add(s);
                }
                int totalServers = servers.size();
                List<int[]> batch = new ArrayList<>();
                int[] doneServers = new int[1];
                main.post(() -> listener.onProgress(0, totalServers));

                ProbeRunner<List<ChannelServer>> runner = new ProbeRunner<>(app,
                        group -> classify(group.get(0).getStreamUrl(), group.get(0).getUserAgent(), group.get(0).getReferrer()),
                        new ProbeRunner.Sink<List<ChannelServer>>() {
                            @Override
                            public void onResult(List<ChannelServer> group, boolean working) {
                                int status = working ? ChannelServer.STATUS_WORKING : ChannelServer.STATUS_BROKEN;
                                for (ChannelServer s : group) batch.add(new int[]{s.getId(), status});
                                totals[working ? 0 : 1] += group.size();
                                doneServers[0] += group.size();
                            }

                            @Override
                            public void onBatch(int done, int total) {
                                dao.updateStatuses(batch, System.currentTimeMillis());
                                batch.clear();
                                int d = doneServers[0];
                                main.post(() -> listener.onProgress(d, totalServers));
                            }

                            @Override
                            public void onPaused(String reason) {
                                main.post(() -> listener.onPaused(reason));
                            }

                            @Override
                            public void onResumed() {
                                main.post(listener::onResumed);
                            }
                        });
                runner.run(new ArrayList<>(byKey.values()));
                cancelled[0] = runner.isCancelled();
                Log.i(TAG, "Checked " + doneServers[0] + "/" + totalServers + " servers (" + byKey.size()
                        + " unique URLs): " + totals[0] + " working, " + totals[1] + " broken"
                        + (cancelled[0] ? " (stopped)" : ""));
            } catch (Exception e) {
                Log.e(TAG, "Server check failed", e);
            } finally {
                running.set(false);
                main.post(() -> listener.onComplete(totals[0], totals[1], cancelled[0]));
            }
        }).start();
        return true;
    }

    /**
     * Blocking check of catalogue streams, used by "import working channels only".
     * Returns the URLs that answered. Call off the main thread.
     *
     * @throws InterruptedException when the admin pressed Stop
     */
    public static Set<String> checkStreams(Context context, List<Catalog.Stream> streams, Progress progress)
            throws InterruptedException {
        Map<String, Catalog.Stream> unique = new LinkedHashMap<>();
        for (Catalog.Stream s : streams) unique.put(key(s.url, s.userAgent, s.referrer), s);
        Set<String> working = Collections.synchronizedSet(new HashSet<>());

        ProbeRunner<Catalog.Stream> runner = new ProbeRunner<>(context,
                s -> classify(s.url, s.userAgent, s.referrer),
                new ProbeRunner.Sink<Catalog.Stream>() {
                    @Override
                    public void onResult(Catalog.Stream s, boolean ok) {
                        if (ok) working.add(s.url);
                    }

                    @Override
                    public void onBatch(int done, int total) {
                        if (progress != null) progress.onProgress(done, total);
                    }

                    @Override
                    public void onPaused(String reason) {
                        if (progress != null) progress.onPaused(reason);
                    }

                    @Override
                    public void onResumed() {
                    }
                });
        runner.run(new ArrayList<>(unique.values()));
        if (runner.isCancelled()) throw new InterruptedException("Stopped");
        return working;
    }

    private static String key(String url, String ua, String referrer) {
        return url + '\n' + (ua == null ? "" : ua) + '\n' + (referrer == null ? "" : referrer);
    }

    /**
     * OK when the URL answers with an HLS playlist or media; BROKEN when the server answered
     * with something else; NETWORK_ERROR when there was no answer at all (that last case may be
     * our own connection, so {@link ProbeRunner} confirms it before blaming the stream).
     */
    static ProbeRunner.Outcome classify(String url, String userAgent, String referrer) {
        if (url == null || url.isEmpty()) return ProbeRunner.Outcome.BROKEN;
        HttpURLConnection conn = null;
        try {
            // Follow redirects by hand: HttpURLConnection won't switch between http and https
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent",
                        userAgent != null && !userAgent.isEmpty() ? userAgent : DEFAULT_USER_AGENT);
                if (referrer != null && !referrer.isEmpty()) conn.setRequestProperty("Referer", referrer);
                // Only the first bytes are needed to recognise a stream
                conn.setRequestProperty("Range", "bytes=0-1023");
                int code = conn.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = null;
                    if (location == null) return ProbeRunner.Outcome.BROKEN;
                    url = new URL(new URL(url), location).toString();
                    continue;
                }
                if (code < 200 || code >= 300) return ProbeRunner.Outcome.BROKEN;

                String type = conn.getContentType() == null ? "" : conn.getContentType().toLowerCase(Locale.ROOT);
                if (type.startsWith("video/") || type.startsWith("audio/")
                        || type.contains("mp2t") || type.contains("dash+xml")) {
                    return ProbeRunner.Outcome.OK;
                }
                // HLS playlists (often served as text/plain or octet-stream) start with #EXTM3U
                byte[] buffer = new byte[512];
                int read;
                try (InputStream in = conn.getInputStream()) {
                    read = in.read(buffer);
                }
                if (read <= 0) return ProbeRunner.Outcome.BROKEN;
                String head = new String(buffer, 0, read, StandardCharsets.UTF_8).trim();
                if (head.startsWith("﻿")) head = head.substring(1);
                return head.startsWith("#EXTM3U") || type.contains("octet-stream")
                        ? ProbeRunner.Outcome.OK : ProbeRunner.Outcome.BROKEN;
            }
            return ProbeRunner.Outcome.BROKEN; // redirect loop
        } catch (SSLException e) {
            // The server answered with a bad certificate: its fault, not our connection's
            return ProbeRunner.Outcome.BROKEN;
        } catch (IOException e) {
            // Timeout, DNS failure, refused, reset... could be the phone's connection
            return ProbeRunner.Outcome.NETWORK_ERROR;
        } catch (Exception e) {
            return ProbeRunner.Outcome.BROKEN; // malformed URL etc.
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
