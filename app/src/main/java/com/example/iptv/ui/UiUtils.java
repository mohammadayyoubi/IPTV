package com.example.iptv.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigationrail.NavigationRailView;

import java.util.Locale;

/** Small helpers shared by every screen of the redesigned UI. */
public final class UiUtils {

    private static final int[] AVATAR_TINTS = {
            R.color.tint_1, R.color.tint_2, R.color.tint_3, R.color.tint_4,
            R.color.tint_5, R.color.tint_6, R.color.tint_7, R.color.tint_8
    };

    private UiUtils() {}

    /**
     * Adds the system bar (and display cutout) insets on top of the view's own padding.
     * With {@code includeIme} the keyboard height is used for the bottom edge as well, so
     * forms scroll above the keyboard.
     */
    public static void applyInsets(View view, boolean top, boolean bottom, boolean includeIme) {
        final int startLeft = view.getPaddingLeft();
        final int startTop = view.getPaddingTop();
        final int startRight = view.getPaddingRight();
        final int startBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int bottomInset = bars.bottom;
            if (includeIme) {
                bottomInset = Math.max(bottomInset,
                        windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            }
            v.setPadding(startLeft + bars.left,
                    startTop + (top ? bars.top : 0),
                    startRight + bars.right,
                    startBottom + (bottom ? bottomInset : 0));
            return windowInsets;
        });
    }

    public static void applyInsets(View view, boolean top, boolean bottom) {
        applyInsets(view, top, bottom, false);
    }

    /** How many columns of at least {@code minTileWidth} fit in the current window width. */
    public static int spanCount(Context context, @DimenRes int minTileWidth) {
        float density = context.getResources().getDisplayMetrics().density;
        float widthDp = context.getResources().getConfiguration().screenWidthDp;
        float gutterDp = context.getResources().getDimension(R.dimen.page_gutter) / density;
        float minDp = context.getResources().getDimension(minTileWidth) / density;
        // The navigation rail (main screen on wide windows) takes ~80dp of the width
        Activity activity = findActivity(context);
        if (activity != null && activity.findViewById(R.id.bottom_nav_view) instanceof NavigationRailView) {
            widthDp -= 80;
        }
        int span = (int) ((widthDp - 2 * gutterDp) / minDp);
        return Math.max(1, span);
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** Sets up a responsive grid with even gaps between tiles. */
    public static void setupGrid(RecyclerView recyclerView, @DimenRes int minTileWidth) {
        Context context = recyclerView.getContext();
        int span = spanCount(context, minTileWidth);
        recyclerView.setLayoutManager(new GridLayoutManager(context, span));
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(
                context.getResources().getDimensionPixelSize(R.dimen.grid_gap)));
    }

    /** Shows a circular avatar with the first letter of {@code name}, tinted from the name. */
    public static void applyAvatar(TextView avatar, String name) {
        Context context = avatar.getContext();
        String safe = name == null ? "" : name.trim();
        avatar.setText(safe.isEmpty() ? "?" : safe.substring(0, 1).toUpperCase(Locale.ROOT));
        int tint = ContextCompat.getColor(context,
                AVATAR_TINTS[Math.abs(safe.toLowerCase(Locale.ROOT).hashCode()) % AVATAR_TINTS.length]);
        // A dark, desaturated version of the tint for the circle, the tint itself for the letter
        int fill = ColorUtils.blendARGB(tint, ContextCompat.getColor(context, R.color.surface), 0.78f);
        avatar.setBackgroundTintList(ColorStateList.valueOf(fill));
        avatar.setTextColor(tint);
    }

    /** Icon for an iptv-org category name ("News", "Sports", ...); a generic icon otherwise. */
    @DrawableRes
    public static int categoryIcon(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "news": return R.drawable.ic_cat_news;
            case "sports": return R.drawable.ic_cat_sports;
            case "music": return R.drawable.ic_cat_music;
            case "movies": return R.drawable.ic_cat_movies;
            case "series": return R.drawable.ic_cat_series;
            case "kids": return R.drawable.ic_cat_kids;
            case "animation": return R.drawable.ic_cat_animation;
            case "family": return R.drawable.ic_group;
            case "business": return R.drawable.ic_cat_business;
            case "auto": return R.drawable.ic_cat_auto;
            case "cooking": return R.drawable.ic_cat_cooking;
            case "education": return R.drawable.ic_cat_education;
            case "science": return R.drawable.ic_cat_science;
            case "documentary": return R.drawable.ic_cat_documentary;
            case "weather": return R.drawable.ic_cat_weather;
            case "travel": return R.drawable.ic_cat_travel;
            case "outdoor": return R.drawable.ic_cat_outdoor;
            case "shop": return R.drawable.ic_cat_shop;
            case "legislative": return R.drawable.ic_cat_legislative;
            case "public": return R.drawable.ic_public;
            case "comedy": return R.drawable.ic_cat_comedy;
            case "entertainment": return R.drawable.ic_cat_movies;
            case "classic": return R.drawable.ic_cat_classic;
            case "relax": return R.drawable.ic_cat_relax;
            case "lifestyle": return R.drawable.ic_favorite;
            case "religious": return R.drawable.ic_cat_kids;
            case "interactive": return R.drawable.ic_cat_interactive;
            case "culture": return R.drawable.ic_cat_culture;
            case "general": return R.drawable.ic_live_tv;
            case "xxx": return R.drawable.ic_lock;
            default: return R.drawable.ic_category;
        }
    }

    /** Category icon in a tinted circle (tint is stable per name, like the avatars). */
    public static void applyCategoryIcon(android.widget.ImageView view, String name) {
        Context context = view.getContext();
        String safe = name == null ? "" : name.trim();
        int tint = ContextCompat.getColor(context,
                AVATAR_TINTS[Math.abs(safe.toLowerCase(Locale.ROOT).hashCode()) % AVATAR_TINTS.length]);
        int fill = ColorUtils.blendARGB(tint, ContextCompat.getColor(context, R.color.surface), 0.78f);
        view.setImageResource(categoryIcon(safe));
        view.setImageTintList(ColorStateList.valueOf(tint));
        view.setBackgroundTintList(ColorStateList.valueOf(fill));
    }

    /** Toggles the shared empty-state view ({@code view_empty_state.xml}). */
    public static void showEmptyState(View emptyView, boolean show, @DrawableRes int icon,
                                      @StringRes int title, @StringRes int body) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        android.widget.ImageView iconView = emptyView.findViewById(R.id.emptyIcon);
        TextView titleView = emptyView.findViewById(R.id.emptyTitle);
        TextView bodyView = emptyView.findViewById(R.id.emptyBody);
        iconView.setImageResource(icon);
        titleView.setText(title);
        bodyView.setText(body);
    }

    public static String quantity(Context context, int pluralRes, int count) {
        return context.getResources().getQuantityString(pluralRes, count, count);
    }

    /** Runs {@code onChange} whenever the adapter's contents change (including async filters). */
    public static void onAdapterChanged(RecyclerView.Adapter<?> adapter, Runnable onChange) {
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { onChange.run(); }
            @Override public void onItemRangeInserted(int positionStart, int itemCount) { onChange.run(); }
            @Override public void onItemRangeRemoved(int positionStart, int itemCount) { onChange.run(); }
        });
    }

    /** Collapses an extended FAB to its icon while the list scrolls down, expands it going up. */
    public static void shrinkFabOnScroll(RecyclerView recyclerView, ExtendedFloatingActionButton fab) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 6 && fab.isExtended()) fab.shrink();
                else if (dy < -6 && !fab.isExtended()) fab.extend();
            }
        });
    }
}
