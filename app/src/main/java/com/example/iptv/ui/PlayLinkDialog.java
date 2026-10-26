package com.example.iptv.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;

import com.example.iptv.M3uLink;
import com.example.iptv.NetworkMonitor;
import com.example.iptv.R;
import com.example.iptv.activities.user.ChannelDetailActivity;
import com.example.iptv.activities.user.PlaylistActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/**
 * "Play a link": paste an M3U / M3U8 URL. A stream plays straight away; a playlist of
 * channels opens {@link PlaylistActivity}.
 */
public final class PlayLinkDialog {

    private PlayLinkDialog() {}

    public static void show(Activity activity) {
        View body = activity.getLayoutInflater().inflate(R.layout.dialog_play_link, null, false);
        TextInputLayout field = body.findViewById(R.id.linkField);
        MaterialAutoCompleteTextView input = body.findViewById(R.id.linkInput);
        View progress = body.findViewById(R.id.linkProgress);
        input.setAdapter(new ArrayAdapter<>(activity, R.layout.item_dropdown, M3uLink.recentLinks(activity)));

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.play_link)
                .setIcon(R.drawable.ic_link)
                .setView(body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.play, null) // set below so the dialog stays open while loading
                .create();

        Runnable go = () -> {
            String link = input.getText() == null ? "" : input.getText().toString().trim();
            String lower = link.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                field.setError(activity.getString(R.string.play_link_invalid));
                return;
            }
            if (!NetworkMonitor.hasValidatedNetwork(activity)) {
                field.setError(activity.getString(R.string.offline_title));
                return;
            }
            field.setError(null);
            progress.setVisibility(View.VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            open(activity, link, dialog, field, progress);
        };

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> go.run()));
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                go.run();
                return true;
            }
            return false;
        });
        dialog.show();
    }

    private static void open(Activity activity, String link, AlertDialog dialog,
                             TextInputLayout field, View progress) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            M3uLink.Result result = null;
            String error = null;
            try {
                result = M3uLink.open(link);
                if (!result.isStream && result.entries.size() > 1) M3uLink.save(activity, result.entries);
            } catch (Exception e) {
                error = e.getMessage();
            }
            M3uLink.Result r = result;
            String err = error;
            main.post(() -> {
                if (activity.isFinishing()) return;
                progress.setVisibility(View.INVISIBLE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                if (r == null) {
                    field.setError(activity.getString(R.string.play_link_failed, err == null ? "" : err));
                    return;
                }
                M3uLink.remember(activity, link);
                String host = Uri.parse(link).getHost();
                Intent intent;
                if (r.isStream) {
                    intent = ChannelDetailActivity.forLink(activity, link, host, null);
                } else if (r.entries.size() == 1) {
                    M3uLink.Entry e = r.entries.get(0);
                    intent = ChannelDetailActivity.forLink(activity, e.url, e.name, e.logo, e.userAgent, e.referrer);
                } else {
                    intent = new Intent(activity, PlaylistActivity.class).putExtra(PlaylistActivity.EXTRA_TITLE, host);
                }
                dialog.dismiss();
                activity.startActivity(intent);
            });
        }).start();
    }
}
