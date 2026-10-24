package com.example.iptv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Tells "the stream is down" apart from "this phone is offline / on a bad connection".
 * <ul>
 *   <li>{@link #hasValidatedNetwork}: Android's own verdict. A network only becomes VALIDATED
 *       once the system has reached the internet through it (captive portals don't count).</li>
 *   <li>{@link #probeInternet}: our own active check against highly available endpoints that
 *       answer HTTP 204. It also measures latency to detect a connection that is up but too slow
 *       to judge streams fairly.</li>
 *   <li>{@link #watch}: callbacks the moment the default network is lost or loses validation.</li>
 * </ul>
 */
public final class NetworkMonitor {

    /** A health probe slower than this means the connection is too poor to judge streams. */
    public static final long POOR_LATENCY_MS = 3_000;

    private static final String[] PROBE_URLS = {
            "https://www.gstatic.com/generate_204",
            "https://cp.cloudflare.com/generate_204",
            "http://connectivitycheck.gstatic.com/generate_204",
    };
    private static final int PROBE_TIMEOUT_MS = 4_000;

    private NetworkMonitor() {}

    public static class Health {
        public final boolean online;
        public final long latencyMs;

        Health(boolean online, long latencyMs) {
            this.online = online;
            this.latencyMs = latencyMs;
        }

        /** Online and fast enough that a stream timing out really is the stream's fault. */
        public boolean isGood() {
            return online && latencyMs <= POOR_LATENCY_MS;
        }
    }

    /** True when the default network has internet access verified by Android. */
    public static boolean hasValidatedNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** Blocking: tries the probe endpoints in turn; online if any answers 204. Off the main thread. */
    public static Health probeInternet() {
        for (String url : PROBE_URLS) {
            long start = System.currentTimeMillis();
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(PROBE_TIMEOUT_MS);
                conn.setReadTimeout(PROBE_TIMEOUT_MS);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false); // a captive portal answers with a redirect
                if (conn.getResponseCode() == 204) {
                    return new Health(true, System.currentTimeMillis() - start);
                }
            } catch (Exception ignored) {
                // try the next endpoint
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return new Health(false, Long.MAX_VALUE);
    }

    public interface Listener {
        /** The default network went away or lost internet validation. */
        void onConnectionLost();
        /** A network with validated internet access is available again. */
        void onConnectionAvailable();
    }

    /** Watches the default network until {@link Watch#stop()} is called. Callbacks come on a binder thread. */
    public static Watch watch(Context context, Listener listener) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                listener.onConnectionLost();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    listener.onConnectionAvailable();
                } else {
                    listener.onConnectionLost();
                }
            }
        };
        try {
            if (cm != null) cm.registerDefaultNetworkCallback(callback);
        } catch (RuntimeException e) {
            // Too many callbacks registered, etc.: the periodic health probes still protect the run
        }
        return () -> {
            try {
                if (cm != null) cm.unregisterNetworkCallback(callback);
            } catch (RuntimeException ignored) {
            }
        };
    }

    public interface Watch {
        void stop();
    }
}
