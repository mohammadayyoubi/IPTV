package com.example.iptv.activities.user;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.iptv.Prefs;
import com.example.iptv.R;
import com.example.iptv.database.DBHelper;
import com.example.iptv.database.HistoryDAO;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

/** Viewer settings: language, playback, history, and About (TechWrap LB Team). */
public class SettingsActivity extends BaseActivity {

    /** Subtitle languages offered (ISO 639-1); the phone's language is added if missing. */
    private static final String[] SUBTITLE_LANGUAGES =
            {"ar", "en", "fr", "es", "de", "tr", "fa", "ur", "hi", "ru", "pt", "it"};
    private static final int[] QUALITY_VALUES = {0, 1080, 720, 480, 360, -1};
    private static final String PHONE = "+96170080214";

    private TextView languageValue, qualityValue, subtitleLanguageValue, historyValue;
    private MaterialSwitch switchAutoSwitch, switchAutoPip, switchSubtitles;
    private HistoryDAO historyDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        UiUtils.applyInsets(findViewById(R.id.root), true, false);
        UiUtils.applyInsets(findViewById(R.id.scroll), false, true);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        languageValue = findViewById(R.id.languageValue);
        qualityValue = findViewById(R.id.qualityValue);
        subtitleLanguageValue = findViewById(R.id.subtitleLanguageValue);
        historyValue = findViewById(R.id.historyValue);
        switchAutoSwitch = findViewById(R.id.switchAutoSwitch);
        switchAutoPip = findViewById(R.id.switchAutoPip);
        switchSubtitles = findViewById(R.id.switchSubtitles);
        historyDAO = new HistoryDAO(DBHelper.getInstance(this).getWritableDatabase());

        findViewById(R.id.rowLanguage).setOnClickListener(v -> chooseLanguage());
        findViewById(R.id.rowAutoSwitch).setOnClickListener(v -> {
            Prefs.setAutoSwitchServers(this, !Prefs.autoSwitchServers(this));
            render();
        });
        findViewById(R.id.rowAutoPip).setOnClickListener(v -> {
            Prefs.setAutoPip(this, !Prefs.autoPip(this));
            render();
        });
        findViewById(R.id.rowQuality).setOnClickListener(v -> chooseQuality());
        findViewById(R.id.rowSubtitles).setOnClickListener(v -> {
            boolean on = Prefs.SUBTITLES_AUTO.equals(Prefs.subtitlesMode(this));
            Prefs.setSubtitlesMode(this, on ? Prefs.SUBTITLES_OFF : Prefs.SUBTITLES_AUTO);
            render();
        });
        findViewById(R.id.rowSubtitleLanguage).setOnClickListener(v -> chooseSubtitleLanguage());
        findViewById(R.id.rowClearHistory).setOnClickListener(v -> clearHistory());

        TextView version = findViewById(R.id.versionText);
        try {
            version.setText(getString(R.string.about_version,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception ignored) {
        }
        findViewById(R.id.phoneButton).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + PHONE))));
        findViewById(R.id.phoneButton).setOnLongClickListener(v -> {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("TechWrap LB", getString(R.string.about_phone)));
            Toast.makeText(this, R.string.about_copied, Toast.LENGTH_SHORT).show();
            return true;
        });

        render();
    }

    private void render() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String lang = locales.isEmpty() ? "" : locales.get(0).getLanguage();
        languageValue.setText("ar".equals(lang) ? R.string.language_arabic
                : "en".equals(lang) ? R.string.language_english : R.string.language_system);

        switchAutoSwitch.setChecked(Prefs.autoSwitchServers(this));
        switchAutoPip.setChecked(Prefs.autoPip(this));
        boolean subtitlesOn = Prefs.SUBTITLES_AUTO.equals(Prefs.subtitlesMode(this));
        switchSubtitles.setChecked(subtitlesOn);
        subtitleLanguageValue.setText(languageName(Prefs.subtitleLanguage(this)));
        findViewById(R.id.rowSubtitleLanguage).setAlpha(subtitlesOn ? 1f : 0.45f);
        qualityValue.setText(qualityLabel(Prefs.maxVideoHeight(this)));
        historyValue.setText(getString(R.string.history_count, historyDAO.count()));
    }

    private void chooseLanguage() {
        String[] tags = {"", "en", "ar"};
        CharSequence[] labels = {getString(R.string.language_system),
                getString(R.string.language_english), getString(R.string.language_arabic)};
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        String lang = current.isEmpty() ? "" : current.get(0).getLanguage();
        int checked = "ar".equals(lang) ? 2 : "en".equals(lang) ? 1 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_language)
                .setIcon(R.drawable.ic_language)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    d.dismiss();
                    // Recreates every open screen in the new language (and right-to-left for Arabic)
                    AppCompatDelegate.setApplicationLocales(which == 0
                            ? LocaleListCompat.getEmptyLocaleList()
                            : LocaleListCompat.forLanguageTags(tags[which]));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseQuality() {
        CharSequence[] labels = new CharSequence[QUALITY_VALUES.length];
        int checked = 0;
        for (int i = 0; i < QUALITY_VALUES.length; i++) {
            labels[i] = qualityOption(QUALITY_VALUES[i]);
            if (QUALITY_VALUES[i] == Prefs.maxVideoHeight(this)) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.video_quality)
                .setIcon(R.drawable.ic_hd)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    Prefs.setMaxVideoHeight(this, QUALITY_VALUES[which]);
                    render();
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseSubtitleLanguage() {
        java.util.List<String> codes = new java.util.ArrayList<>(java.util.Arrays.asList(SUBTITLE_LANGUAGES));
        String phone = Locale.getDefault().getLanguage();
        if (!codes.contains(phone)) codes.add(0, phone);
        CharSequence[] labels = new CharSequence[codes.size()];
        int checked = 0;
        for (int i = 0; i < codes.size(); i++) {
            labels[i] = languageName(codes.get(i));
            if (codes.get(i).equals(Prefs.subtitleLanguage(this))) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.subtitle_language)
                .setIcon(R.drawable.ic_cc)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    Prefs.setSubtitleLanguage(this, codes.get(which));
                    render();
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearHistory() {
        if (historyDAO.count() == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_history)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_history, (d, w) -> {
                    historyDAO.clear();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }

    private String qualityOption(int value) {
        if (value == 0) return getString(R.string.quality_option_auto);
        if (value < 0) return getString(R.string.quality_option_lowest);
        return getString(R.string.quality_option_up_to, value);
    }

    private String qualityLabel(int value) {
        if (value == 0) return getString(R.string.quality_short_auto);
        if (value < 0) return getString(R.string.quality_short_lowest);
        return getString(R.string.quality_option_up_to, value);
    }

    private static String languageName(String code) {
        Locale locale = new Locale(code);
        // Each language in its own script ("العربية", "Français") so it's recognisable
        String name = locale.getDisplayLanguage(locale);
        if (name.isEmpty()) return code;
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }
}
