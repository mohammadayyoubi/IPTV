package com.example.iptv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** Viewer settings (Settings screen + player). */
public final class Prefs {

    public static final String SUBTITLES_OFF = "off";
    public static final String SUBTITLES_AUTO = "auto";

    private static final String FILE = "settings";
    private static final String KEY_AUTO_SWITCH = "auto_switch_servers";
    private static final String KEY_AUTO_PIP = "auto_pip";
    private static final String KEY_SUBTITLES = "subtitles_mode";
    private static final String KEY_SUBTITLE_LANGUAGE = "subtitles_language";
    // Kept in the player's own file so the choice made before Settings existed is preserved
    private static final String PLAYER_FILE = "player";
    private static final String KEY_MAX_HEIGHT = "max_height";
    private static final String KEY_ONBOARDED = "onboarding_done";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Jump to the next server automatically when one fails. */
    public static boolean autoSwitchServers(Context c) { return sp(c).getBoolean(KEY_AUTO_SWITCH, true); }
    public static void setAutoSwitchServers(Context c, boolean on) { sp(c).edit().putBoolean(KEY_AUTO_SWITCH, on).apply(); }

    /** Go to picture-in-picture when leaving the app while a channel plays. */
    public static boolean autoPip(Context c) { return sp(c).getBoolean(KEY_AUTO_PIP, true); }
    public static void setAutoPip(Context c, boolean on) { sp(c).edit().putBoolean(KEY_AUTO_PIP, on).apply(); }

    /** {@link #SUBTITLES_AUTO}: show subtitles whenever a stream has them; {@link #SUBTITLES_OFF}: never. */
    public static String subtitlesMode(Context c) { return sp(c).getString(KEY_SUBTITLES, SUBTITLES_AUTO); }
    public static void setSubtitlesMode(Context c, String mode) { sp(c).edit().putString(KEY_SUBTITLES, mode).apply(); }

    /** Preferred subtitle language (ISO 639-1, e.g. "ar"); defaults to the phone's language. */
    public static String subtitleLanguage(Context c) {
        return sp(c).getString(KEY_SUBTITLE_LANGUAGE, Locale.getDefault().getLanguage());
    }
    public static void setSubtitleLanguage(Context c, String lang) { sp(c).edit().putString(KEY_SUBTITLE_LANGUAGE, lang).apply(); }

    /** Video quality cap: 0 = auto, -1 = data saver (lowest), otherwise a max height like 720. */
    public static int maxVideoHeight(Context c) {
        return c.getApplicationContext().getSharedPreferences(PLAYER_FILE, Context.MODE_PRIVATE).getInt(KEY_MAX_HEIGHT, 0);
    }
    public static void setMaxVideoHeight(Context c, int height) {
        c.getApplicationContext().getSharedPreferences(PLAYER_FILE, Context.MODE_PRIVATE).edit().putInt(KEY_MAX_HEIGHT, height).apply();
    }

    public static boolean onboardingDone(Context c) { return sp(c).getBoolean(KEY_ONBOARDED, false); }
    public static void setOnboardingDone(Context c) { sp(c).edit().putBoolean(KEY_ONBOARDED, true).apply(); }
}
