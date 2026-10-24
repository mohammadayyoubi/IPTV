package com.example.iptv.activities.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iptv.AdminTasks;
import com.example.iptv.ImportOptions;
import com.example.iptv.NetworkMonitor;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;
import com.example.iptv.StreamChecker;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.getFromInternet;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.InputDialog;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;

import static com.example.iptv.getFromInternet.getAllCountries;

public class AdminPanelActivity extends BaseActivity {

    private enum ImportMode { MY_COUNTRY, ALL_COUNTRIES, UPDATE }

    DBHelper dbHelper;
    private CountryDAO countryDao;
    private CategoryDAO categoryDao;
    private ChannelDAO ChannelDao;
    private ChannelServerDAO ChannelServerDao;
    private getFromInternet.Daos daos;
    private View importProgress;
    private TextView importStatusText;
    private View importStatusBar, importStatusIcon, stopTaskButton;
    private TextView importPercentText;
    private TextView tvTotalChannels, tvTotalCategories, tvTotalCountries;
    // Rows that start a long task: locked while any task runs
    private View[] taskRows;
    private boolean wasRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);
        UiUtils.applyInsets(findViewById(R.id.root), true, false);
        UiUtils.applyInsets(findViewById(R.id.scroll), false, true);

        tvTotalChannels = findViewById(R.id.tvTotalChannels);
        tvTotalCategories = findViewById(R.id.tvTotalCategories);
        tvTotalCountries = findViewById(R.id.tvTotalCountries);
        importProgress = findViewById(R.id.importProgress);
        importStatusText = findViewById(R.id.importStatusText);
        importStatusBar = findViewById(R.id.importStatusBar);
        importStatusIcon = findViewById(R.id.importStatusIcon);
        stopTaskButton = findViewById(R.id.stopTaskButton);
        importPercentText = findViewById(R.id.importPercentText);
        stopTaskButton.setOnClickListener(v -> AdminTasks.stop());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView signedInText = findViewById(R.id.signedInText);
        if (user != null && user.getEmail() != null) {
            signedInText.setText(user.getEmail());
        } else {
            signedInText.setVisibility(View.GONE);
        }

        dbHelper = new DBHelper(this);
        countryDao = new CountryDAO(dbHelper.getWritableDatabase());
        categoryDao = new CategoryDAO(dbHelper.getWritableDatabase());
        ChannelDao = new ChannelDAO(dbHelper.getWritableDatabase());
        ChannelServerDao = new ChannelServerDAO(dbHelper.getWritableDatabase());
        daos = new getFromInternet.Daos(this, ChannelDao, countryDao, categoryDao, ChannelServerDao);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Manage rows
        findViewById(R.id.manageCategoriesButton).setOnClickListener(v -> startActivity(new Intent(this, activity_category_management.class)));
        findViewById(R.id.manageChannelsButton).setOnClickListener(v -> startActivity(new Intent(this, activity_channel_management.class)));
        findViewById(R.id.manageCountriesButton).setOnClickListener(v -> startActivity(new Intent(this, activity_country_management.class)));

        findViewById(R.id.logoutButton).setOnClickListener(v ->
                InputDialog.confirm(this, getString(R.string.log_out),
                        "You'll need to sign in again to manage the app.",
                        getString(R.string.log_out), false, () -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }));

        taskRows = new View[]{
                findViewById(R.id.updateDatabaseButton),
                findViewById(R.id.getMyCountryChannelsButton),
                findViewById(R.id.getAllCountriesChannelsButton),
                findViewById(R.id.checkServersButton),
                findViewById(R.id.refreshLogosButton),
                findViewById(R.id.clearDatabaseButton),
        };

        findViewById(R.id.updateDatabaseButton).setOnClickListener(v -> showImportDialog(ImportMode.UPDATE));
        findViewById(R.id.getMyCountryChannelsButton).setOnClickListener(v -> showImportDialog(ImportMode.MY_COUNTRY));
        findViewById(R.id.getAllCountriesChannelsButton).setOnClickListener(v -> showImportDialog(ImportMode.ALL_COUNTRIES));
        findViewById(R.id.checkServersButton).setOnClickListener(v -> showCheckServersDialog());
        findViewById(R.id.refreshLogosButton).setOnClickListener(v -> {
            if (!requireOnline(R.string.offline_body)) return;
            if (!AdminTasks.start("Preparing logo refresh…")) return;
            getFromInternet.refreshLogos(daos, taskCallback("Logos refreshed"));
        });
        findViewById(R.id.clearDatabaseButton).setOnClickListener(v ->
                InputDialog.confirm(this, getString(R.string.clear_database),
                        "This deletes every channel, server, category, country and favorite. It can't be undone.",
                        getString(R.string.delete_all), true, () -> {
                            if (AdminTasks.isRunning()) return;
                            Log.d("ClearDatabase", "Clearing database...");
                            dbHelper.resetDatabase();
                            Toast.makeText(this, "All data cleared!", Toast.LENGTH_SHORT).show();
                            seedIfEmpty();
                            refreshDashboard();
                        }));

        seedIfEmpty();
        refreshDashboard();
    }

    /** First run (or after clearing): countries come from the device, categories from iptv-org. */
    private void seedIfEmpty() {
        if (countryDao.count() == 0) {
            Log.d("AutoLoad", "Loading countries...");
            for (Country country : getAllCountries()) countryDao.insert(country);
        }
        // Offline: wait for the next visit (or Update database) instead of failing now
        if (categoryDao.count() == 0 && NetworkMonitor.hasValidatedNetwork(this)
                && AdminTasks.start("Loading categories…")) {
            getFromInternet.getAllCategories(categories -> {
                for (Category c : categories) categoryDao.insert(c);
                AdminTasks.finish();
                if (categories.isEmpty()) {
                    toast(getApplicationContext(), "Couldn't load categories. Check the connection and use Update database.");
                }
            });
        }
    }

    // ───────────── Import / update ─────────────

    private void showImportDialog(ImportMode mode) {
        if (AdminTasks.isRunning()) {
            Toast.makeText(this, R.string.task_running, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!requireOnline(R.string.offline_body)) return;
        View body = getLayoutInflater().inflate(R.layout.dialog_import, null, false);
        MaterialButtonToggleGroup scope = body.findViewById(R.id.scopeToggle);
        MaterialButtonToggleGroup filter = body.findViewById(R.id.serverFilterToggle);
        TextView help = body.findViewById(R.id.filterHelp);
        TextInputEditText capInput = body.findViewById(R.id.capInput);

        if (mode == ImportMode.UPDATE) {
            body.findViewById(R.id.scopeLabel).setVisibility(View.VISIBLE);
            scope.setVisibility(View.VISIBLE);
        }
        if (mode == ImportMode.ALL_COUNTRIES) {
            body.findViewById(R.id.capField).setVisibility(View.VISIBLE);
        }
        filter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.filterAll) help.setText(R.string.import_filter_all_help);
            else if (checkedId == R.id.filterWorking) help.setText(R.string.import_filter_working_help);
            else help.setText(R.string.import_filter_servers_help);
        });

        int title = mode == ImportMode.UPDATE ? R.string.update_database
                : mode == ImportMode.MY_COUNTRY ? R.string.import_my_country : R.string.import_all;
        int icon = mode == ImportMode.UPDATE ? R.drawable.ic_sync
                : mode == ImportMode.MY_COUNTRY ? R.drawable.ic_my_location : R.drawable.ic_cloud_download;

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setIcon(icon)
                .setView(body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(mode == ImportMode.UPDATE ? "Update" : "Import", (d, w) -> {
                    ImportOptions options = new ImportOptions();
                    int checked = filter.getCheckedButtonId();
                    options.serverFilter = checked == R.id.filterAll ? ImportOptions.ServerFilter.ALL
                            : checked == R.id.filterWorking ? ImportOptions.ServerFilter.WORKING
                            : ImportOptions.ServerFilter.WITH_SERVERS;

                    if (mode == ImportMode.ALL_COUNTRIES) {
                        String cap = capInput.getText() == null ? "" : capInput.getText().toString().trim();
                        if (!TextUtils.isEmpty(cap)) {
                            try {
                                options.maxPerCountry = Integer.parseInt(cap);
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                    startImport(mode, options, scope.getCheckedButtonId() == R.id.scopeMine);
                })
                .show();
    }

    private void startImport(ImportMode mode, ImportOptions options, boolean myCountriesOnly) {
        boolean testsStreams = options.serverFilter == ImportOptions.ServerFilter.WORKING;
        switch (mode) {
            case MY_COUNTRY:
                if (!AdminTasks.start("Detecting your country…")) return;
                getFromInternet.getUserCountryCode(countryCode -> {
                    options.countryCode = countryCode;
                    getFromInternet.importChannels(options, daos, taskCallback("Import for " + countryCode + " done"));
                });
                if (testsStreams) AdminTasks.setStopAction(StreamChecker::cancel);
                break;
            case ALL_COUNTRIES:
                if (!AdminTasks.start("Starting import…")) return;
                getFromInternet.importChannels(options, daos, taskCallback("Import done"));
                if (testsStreams) AdminTasks.setStopAction(StreamChecker::cancel);
                break;
            case UPDATE:
                if (myCountriesOnly) {
                    Set<String> codes = getFromInternet.countryCodesInUse(daos);
                    if (codes.isEmpty()) {
                        Toast.makeText(this, "No channels yet: only categories and countries will be updated. Choose All countries to import channels.",
                                Toast.LENGTH_LONG).show();
                    }
                    options.countryCodes = codes;
                }
                if (!AdminTasks.start("Starting update…")) return;
                getFromInternet.updateDatabase(options, daos, taskCallback("Database updated"));
                if (testsStreams) AdminTasks.setStopAction(StreamChecker::cancel);
                break;
        }
    }

    /** Callback shared by long tasks: mirrors progress into AdminTasks and reports the result. */
    private getFromInternet.ImportCallback taskCallback(String doneTitle) {
        Context app = getApplicationContext();
        return new getFromInternet.ImportCallback() {
            @Override
            public void onProgress(String status, int percent) {
                AdminTasks.update(status, status.startsWith("Paused"), percent);
            }

            @Override
            public void onDone(ImportOptions.Result result) {
                AdminTasks.finish();
                toast(app, doneTitle + ": " + result.summary());
            }

            @Override
            public void onError(String message) {
                AdminTasks.finish();
                toast(app, "Failed: " + message);
            }
        };
    }

    // ───────────── Server check ─────────────

    private void showCheckServersDialog() {
        if (AdminTasks.isRunning() || StreamChecker.isRunning()) {
            Toast.makeText(this, R.string.task_running, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!requireOnline(R.string.offline_check_body)) return;
        int total = ChannelServerDao.count();
        if (total == 0) {
            Toast.makeText(this, "There are no servers to check yet", Toast.LENGTH_SHORT).show();
            return;
        }
        final int[] choice = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.check_servers)
                .setIcon(R.drawable.ic_dns)
                .setSingleChoiceItems(new CharSequence[]{
                        getString(R.string.check_servers_stale),
                        getString(R.string.check_servers_all) + " (" + NumberFormat.getInstance().format(total) + ")"
                }, 0, (d, which) -> choice[0] = which)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Check", (d, w) -> startServerCheck(choice[0] == 0))
                .show();
    }

    private void startServerCheck(boolean staleOnly) {
        if (!AdminTasks.start("Preparing server check…")) return;
        Context app = getApplicationContext();
        NumberFormat nf = NumberFormat.getInstance();
        boolean started = StreamChecker.checkAll(this, staleOnly, new StreamChecker.Listener() {
            private int lastDone, lastTotal;

            @Override
            public void onProgress(int done, int total) {
                lastDone = done;
                lastTotal = total;
                AdminTasks.update(app.getString(R.string.check_servers_progress, nf.format(done), nf.format(total)),
                        false, total == 0 ? 0 : (int) (done * 100L / total));
            }

            @Override
            public void onPaused(String reason) {
                AdminTasks.update("Paused: " + reason + ". Resumes by itself when the connection is back ("
                        + nf.format(lastDone) + " / " + nf.format(lastTotal) + ")", true);
            }

            @Override
            public void onResumed() {
                onProgress(lastDone, lastTotal);
            }

            @Override
            public void onComplete(int working, int broken, boolean cancelled) {
                AdminTasks.finish();
                toast(app, (cancelled ? "Check stopped. " : "") + nf.format(working) + " servers working, "
                        + nf.format(broken) + " not responding");
            }
        });
        if (!started) AdminTasks.finish();
        else AdminTasks.setStopAction(StreamChecker::cancel);
    }

    // ───────────── Task state ─────────────

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
        AdminTasks.observe(this::renderTaskState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AdminTasks.observe(null);
    }

    /** Network tasks refuse to start offline: a check would otherwise blame every server. */
    private boolean requireOnline(int messageRes) {
        if (NetworkMonitor.hasValidatedNetwork(this)) return true;
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_wifi_off)
                .setTitle(R.string.offline_title)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        return false;
    }

    private void renderTaskState(boolean running, String status, boolean paused, boolean canStop, int percent) {
        LinearProgressIndicator bar = (LinearProgressIndicator) importProgress;
        boolean known = percent >= 0;
        if (bar.isIndeterminate() == known) {
            // Material requires the bar to be hidden while switching modes
            bar.setVisibility(View.INVISIBLE);
            bar.setIndeterminate(!known);
        }
        if (known) bar.setProgressCompat(percent, true);
        importProgress.setVisibility(running && !paused ? View.VISIBLE : View.INVISIBLE);
        importPercentText.setVisibility(running && known ? View.VISIBLE : View.GONE);
        importPercentText.setText(percent + "%");
        importStatusBar.setVisibility(running && !status.isEmpty() ? View.VISIBLE : View.GONE);
        importStatusText.setText(status);
        importStatusText.setTextColor(getColor(paused ? R.color.warning : R.color.text_secondary));
        importStatusIcon.setVisibility(paused ? View.VISIBLE : View.GONE);
        stopTaskButton.setVisibility(running && canStop ? View.VISIBLE : View.GONE);
        for (View row : taskRows) {
            row.setEnabled(!running);
            row.setAlpha(running ? 0.45f : 1f);
        }
        if (wasRunning && !running) refreshDashboard();
        wasRunning = running;
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void refreshDashboard() {
        NumberFormat nf = NumberFormat.getInstance();
        tvTotalChannels.setText(nf.format(ChannelDao.count()));
        tvTotalCategories.setText(nf.format(categoryDao.count()));
        tvTotalCountries.setText(nf.format(countryDao.count()));
    }
}
