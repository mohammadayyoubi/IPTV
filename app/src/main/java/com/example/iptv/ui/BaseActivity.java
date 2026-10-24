package com.example.iptv.ui;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Every screen draws edge-to-edge (required on Android 15 for targetSdk 35) with light
 * system-bar icons, because the app is always dark. Layouts add the bar insets back
 * as padding through {@link UiUtils#applyInsets}.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
    }
}
