package com.example.iptv;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Saves the stack trace of a crash so it can be shared the next time the app opens
 * (MainUserActivity shows it). Nothing leaves the phone unless the user chooses to share.
 */
public final class CrashReporter {

    private static final String FILE = "last_crash.txt";

    private CrashReporter() {}

    public static void install(Context context) {
        Context app = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                write(app, thread, error);
            } catch (Throwable ignored) {
                // Never let the reporter itself hide the original crash
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    private static void write(Context context, Thread thread, Throwable error) throws Exception {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String report = "TechWrap TV crash report\n"
                + "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()) + "\n"
                + "App: " + BuildConfigInfo.versionName(context) + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Thread: " + thread.getName() + "\n\n"
                + trace;
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), FILE))) {
            out.write(report.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The report of the last crash, or null if the app didn't crash since it was last shown. */
    public static String pendingReport(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) return null;
        try {
            byte[] bytes = new byte[(int) Math.min(file.length(), 64 * 1024)];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int read = in.read(bytes);
                return read > 0 ? new String(bytes, 0, read, StandardCharsets.UTF_8) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        new File(context.getFilesDir(), FILE).delete();
    }

    /** Version name without depending on the generated BuildConfig class. */
    static final class BuildConfigInfo {
        static String versionName(Context context) {
            try {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName
                        + " (" + context.getPackageName() + ")";
            } catch (Exception e) {
                return "?";
            }
        }
    }
}
