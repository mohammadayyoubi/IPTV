package com.example.iptv;

import android.app.Application;

/** App entry point: installs the crash reporter before anything else runs. */
public class TechWrapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReporter.install(this);
    }
}
