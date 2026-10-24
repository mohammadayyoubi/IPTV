package com.example.iptv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.iptv.OOP.Country;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * The viewer's country, so its channels can be listed first. Best signal first:
 * <ol>
 *   <li>the IP country from ipinfo.io: looked up once in the background, then cached;</li>
 *   <li>the mobile network / SIM country (no permission needed);</li>
 *   <li>the phone's region setting.</li>
 * </ol>
 */
public final class HomeCountry {

    private static final String PREFS = "home_country";
    private static final String KEY_IP_ISO = "ip_iso";
    private static final String KEY_CHECKED_AT = "checked_at";
    private static final long RECHECK_MS = 7L * 24 * 60 * 60 * 1000; // people rarely move country
    private static boolean lookupRunning;

    private HomeCountry() {}

    /** Best current guess, as an ISO 3166 code like "LB" (may be empty). */
    public static String iso(Context context) {
        String ip = prefs(context).getString(KEY_IP_ISO, "");
        if (!ip.isEmpty()) return ip;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            String network = tm.getNetworkCountryIso();
            if (network != null && network.length() == 2) return network.toUpperCase(Locale.ROOT);
            String sim = tm.getSimCountryIso();
            if (sim != null && sim.length() == 2) return sim.toUpperCase(Locale.ROOT);
        }
        return Locale.getDefault().getCountry().toUpperCase(Locale.ROOT);
    }

    /** Local Country row id for {@link #iso}, or 0 when unknown / not in the database. */
    public static int countryId(Context context) {
        String iso = iso(context);
        if (iso.length() != 2) return 0;
        String name = new Locale("", iso).getDisplayCountry(Locale.ENGLISH);
        Country country = new CountryDAO(new DBHelper(context.getApplicationContext()).getWritableDatabase()).getByName(name);
        return country != null ? country.getId() : 0;
    }

    /**
     * Refreshes the IP country in the background (at most weekly). {@code onChanged} runs on the
     * main thread if the country changed, so a list can re-sort.
     */
    public static void refreshAsync(Context context, Runnable onChanged) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = prefs(app);
        if (lookupRunning || System.currentTimeMillis() - prefs.getLong(KEY_CHECKED_AT, 0) < RECHECK_MS) return;
        if (!NetworkMonitor.hasValidatedNetwork(app)) return;
        lookupRunning = true;
        new Thread(() -> {
            String found = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL("https://ipinfo.io/json").openConnection();
                conn.setConnectTimeout(8_000);
                conn.setReadTimeout(8_000);
                if (conn.getResponseCode() == 200) {
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) body.append(line);
                    }
                    String iso = new JSONObject(body.toString()).optString("country", "");
                    if (iso.length() == 2) found = iso.toUpperCase(Locale.ROOT);
                }
            } catch (Exception e) {
                Log.w("HomeCountry", "IP country lookup failed", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
            String result = found;
            new Handler(Looper.getMainLooper()).post(() -> {
                lookupRunning = false;
                if (result == null) return; // keep the old value; try again next time
                String before = prefs.getString(KEY_IP_ISO, "");
                prefs.edit().putString(KEY_IP_ISO, result).putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply();
                if (!result.equals(before) && onChanged != null) onChanged.run();
            });
        }).start();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
