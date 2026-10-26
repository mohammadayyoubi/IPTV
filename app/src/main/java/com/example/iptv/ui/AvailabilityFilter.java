package com.example.iptv.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.iptv.R;
import com.example.iptv.database.ChannelQuery;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * "Watchable / Not working / No servers / All" filter for channel grids.
 * A small filter button sits beside the search field; tapping it drops the options down
 * (view_availability_filter.xml) and tapping it again closes them. The button turns red
 * while a filter other than the default (Watchable) is applied.
 */
public class AvailabilityFilter {

    public interface Listener {
        void onAvailabilityChanged(ChannelQuery.Availability availability);
    }

    private final Chip chipWatchable, chipNotWorking, chipNoServers, chipAll, chipRecent;
    private final View options, clearHistoryButton;
    private Runnable onClearHistory;
    private int recentCount;
    private final MaterialButton button;
    private ChannelQuery.Availability availability = ChannelQuery.Availability.WATCHABLE;
    private boolean open;

    /**
     * @param options the included view_availability_filter (the drop-down area)
     * @param button  the filter button beside the search field
     */
    public AvailabilityFilter(View options, MaterialButton button, Listener listener) {
        this.options = options;
        this.button = button;
        ChipGroup group = options.findViewById(R.id.availabilityChips);
        chipWatchable = options.findViewById(R.id.chipWatchable);
        chipNotWorking = options.findViewById(R.id.chipNotWorking);
        chipNoServers = options.findViewById(R.id.chipNoServers);
        chipAll = options.findViewById(R.id.chipAll);
        chipRecent = options.findViewById(R.id.chipRecent);
        clearHistoryButton = options.findViewById(R.id.clearHistoryButton);
        clearHistoryButton.setOnClickListener(v -> confirmClearHistory());

        chipWatchable.setChecked(true);
        group.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipNotWorking) availability = ChannelQuery.Availability.NOT_WORKING;
            else if (id == R.id.chipNoServers) availability = ChannelQuery.Availability.NO_SERVERS;
            else if (id == R.id.chipAll) availability = ChannelQuery.Availability.ALL;
            else if (id == R.id.chipRecent) availability = ChannelQuery.Availability.RECENT;
            else availability = ChannelQuery.Availability.WATCHABLE;
            renderButton();
            renderClearHistory();
            listener.onAvailabilityChanged(availability);
        });

        button.setOnClickListener(v -> setOpen(!open));
        setOpen(false);
    }

    public ChannelQuery.Availability getAvailability() {
        return availability;
    }

    /** What to do after the viewer confirms "Clear history" (empty the table, reload the list). */
    public void setOnClearHistory(Runnable action) {
        onClearHistory = action;
    }

    private void confirmClearHistory() {
        Context ctx = options.getContext();
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setIcon(R.drawable.ic_history)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_history, (d, w) -> {
                    if (onClearHistory != null) onClearHistory.run();
                    android.widget.Toast.makeText(ctx, R.string.history_cleared, android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void renderClearHistory() {
        boolean show = availability == ChannelQuery.Availability.RECENT && recentCount > 0 && onClearHistory != null;
        clearHistoryButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setOpen(boolean value) {
        open = value;
        options.setVisibility(value ? View.VISIBLE : View.GONE);
        renderButton();
    }

    /** Open: tinted background. Non-default filter: red icon, so it's visible while closed. */
    private void renderButton() {
        Context ctx = button.getContext();
        boolean filtered = availability != ChannelQuery.Availability.WATCHABLE;
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                open ? R.color.accent_container : R.color.surface_high)));
        button.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                filtered || open ? R.color.accent : R.color.text_primary)));
        button.setContentDescription(ctx.getString(open ? R.string.filter_hide : R.string.filter));
    }

    /** Shows the totals on every chip. */
    public void updateCounts(ChannelQuery.Counts counts) {
        Context ctx = chipAll.getContext();
        chipWatchable.setText(ctx.getString(R.string.filter_watchable_count, counts.watchable));
        chipNotWorking.setText(ctx.getString(R.string.filter_not_working_count, counts.notWorking));
        chipNoServers.setText(ctx.getString(R.string.filter_no_servers_count, counts.noServers));
        chipAll.setText(ctx.getString(R.string.filter_all_count, counts.all));
        chipRecent.setText(ctx.getString(R.string.filter_recent_count, counts.recent));
        recentCount = counts.recent;
        renderClearHistory();
    }

    /** Short name of the applied filter, e.g. "Not working". */
    public String label() {
        Context ctx = chipAll.getContext();
        switch (availability) {
            case NOT_WORKING: return ctx.getString(R.string.filter_not_working);
            case NO_SERVERS: return ctx.getString(R.string.filter_no_servers);
            case ALL: return ctx.getString(R.string.filter_all);
            case RECENT: return ctx.getString(R.string.filter_recent);
            default: return ctx.getString(R.string.filter_watchable);
        }
    }

    /** Empty-state copy for the selected filter when nothing matches it. */
    public void showEmpty(View emptyState, boolean searching) {
        if (searching) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
            return;
        }
        switch (availability) {
            case NOT_WORKING:
                UiUtils.showEmptyState(emptyState, true, R.drawable.ic_play,
                        R.string.empty_not_working_title, R.string.empty_not_working_body);
                break;
            case NO_SERVERS:
                UiUtils.showEmptyState(emptyState, true, R.drawable.ic_dns,
                        R.string.empty_no_servers_title, R.string.empty_no_servers_body);
                break;
            case RECENT:
                UiUtils.showEmptyState(emptyState, true, R.drawable.ic_history,
                        R.string.empty_recent_title, R.string.empty_recent_body);
                break;
            default:
                UiUtils.showEmptyState(emptyState, true, R.drawable.ic_live_tv,
                        R.string.empty_watchable_title, R.string.empty_watchable_body);
        }
    }
}
