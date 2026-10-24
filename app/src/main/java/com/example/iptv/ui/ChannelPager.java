package com.example.iptv.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelQuery;
import com.example.iptv.database.DBHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads a channel list page by page from SQLite as the user scrolls, so screens stay fast
 * even with every channel in the world imported. Search and filters are SQL queries
 * ({@link ChannelDAO#queryPage}); nothing holds the full list in memory.
 */
public class ChannelPager {

    public interface Sink {
        /** First page of a new query (replace the list). */
        void onFirstPage(List<Channel> channels);
        /** Following pages (append). */
        void onNextPage(List<Channel> channels);
        /** Totals for the current search/scope, per availability. */
        void onCounts(ChannelQuery.Counts counts);
    }

    private static final int PAGE_SIZE = 60;
    private static final int PREFETCH_DISTANCE = 24;
    private static final long SEARCH_DEBOUNCE_MS = 250;

    // One background thread per pager keeps queries in order
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ChannelDAO dao;
    private final Sink sink;

    private ChannelQuery query = new ChannelQuery();
    private int generation;       // bumps on every new query; stale results are dropped
    private int loadedCount;
    private boolean loading;
    private boolean endReached;
    private boolean destroyed;
    private final Runnable debouncedReload = this::reload;

    public ChannelPager(Context context, RecyclerView recyclerView, Sink sink) {
        this.dao = new ChannelDAO(new DBHelper(context.getApplicationContext()).getWritableDatabase());
        this.sink = sink;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || loading || endReached) return;
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                int last = RecyclerView.NO_POSITION;
                if (lm instanceof GridLayoutManager) last = ((GridLayoutManager) lm).findLastVisibleItemPosition();
                else if (lm instanceof LinearLayoutManager) last = ((LinearLayoutManager) lm).findLastVisibleItemPosition();
                if (last != RecyclerView.NO_POSITION && last >= loadedCount - PREFETCH_DISTANCE) loadNext();
            }
        });
    }

    public ChannelQuery getQuery() {
        return query;
    }

    /** Replaces the query and loads its first page. */
    public void setQuery(ChannelQuery newQuery) {
        query = newQuery.copy();
        main.removeCallbacks(debouncedReload);
        reload();
    }

    /** Same as {@link #setQuery} but waits for typing to pause first. */
    public void setSearchDebounced(String search) {
        query.search = search;
        main.removeCallbacks(debouncedReload);
        main.postDelayed(debouncedReload, SEARCH_DEBOUNCE_MS);
    }

    /** Reloads the current query from the first page (e.g. when the screen resumes). */
    public void reload() {
        final int gen = ++generation;
        final ChannelQuery q = query.copy();
        loadedCount = 0;
        endReached = false;
        loading = true;
        executor.execute(() -> {
            List<Channel> page = dao.queryPage(q, PAGE_SIZE, 0);
            ChannelQuery.Counts counts = dao.countByAvailability(q);
            main.post(() -> {
                if (destroyed || gen != generation) return;
                loading = false;
                loadedCount = page.size();
                endReached = page.size() < PAGE_SIZE;
                sink.onFirstPage(page);
                sink.onCounts(counts);
            });
        });
    }

    private void loadNext() {
        final int gen = generation;
        final ChannelQuery q = query.copy();
        final int offset = loadedCount;
        loading = true;
        executor.execute(() -> {
            List<Channel> page = dao.queryPage(q, PAGE_SIZE, offset);
            main.post(() -> {
                if (destroyed || gen != generation) return;
                loading = false;
                loadedCount += page.size();
                endReached = page.size() < PAGE_SIZE;
                if (!page.isEmpty()) sink.onNextPage(page);
            });
        });
    }

    /** Call from onDestroyView / onDestroy. */
    public void destroy() {
        destroyed = true;
        main.removeCallbacks(debouncedReload);
        executor.shutdownNow();
    }
}
