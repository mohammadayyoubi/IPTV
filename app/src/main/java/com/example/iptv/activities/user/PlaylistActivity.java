package com.example.iptv.activities.user;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.M3uLink;
import com.example.iptv.R;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Channels of an M3U playlist opened with "Play a link". Tap one to play it. */
public class PlaylistActivity extends BaseActivity {

    public static final String EXTRA_TITLE = "title";

    private final List<M3uLink.Entry> all = new ArrayList<>();
    private final List<M3uLink.Entry> shown = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private RecyclerView recycler;
    private TextView countText;
    private View emptyState;
    private String query = "";
    private final Runnable applySearch = this::filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        UiUtils.applyInsets(findViewById(R.id.root), true, false);
        recycler = findViewById(R.id.recycler);
        UiUtils.applyInsets(recycler, false, true);
        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) ((TextView) findViewById(R.id.titleText)).setText(title);

        UiUtils.setupGrid(recycler, R.dimen.tile_min_admin_row);
        recycler.setAdapter(new EntryAdapter());

        EditText search = findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                main.removeCallbacks(applySearch);
                main.postDelayed(applySearch, 200);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Parsed playlist is cached on disk (too large for an Intent); read it off the main thread
        new Thread(() -> {
            List<M3uLink.Entry> loaded = M3uLink.load(this);
            main.post(() -> {
                if (isFinishing()) return;
                all.clear();
                all.addAll(loaded);
                filter();
            });
        }).start();
    }

    private void filter() {
        shown.clear();
        for (M3uLink.Entry e : all) {
            if (query.isEmpty()
                    || (e.name != null && e.name.toLowerCase(Locale.ROOT).contains(query))
                    || (e.group != null && e.group.toLowerCase(Locale.ROOT).contains(query))) {
                shown.add(e);
            }
        }
        //noinspection NotifyDataSetChanged
        recycler.getAdapter().notifyDataSetChanged();
        countText.setText(UiUtils.quantity(this, R.plurals.count_channels, shown.size()));
        if (all.isEmpty()) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_link,
                    R.string.playlist_empty_title, R.string.playlist_empty_body);
        } else {
            UiUtils.showEmptyState(emptyState, shown.isEmpty(), R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }

    private class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            final ImageView logo;
            final TextView name, group;

            Holder(View v) {
                super(v);
                logo = v.findViewById(R.id.logo);
                name = v.findViewById(R.id.name);
                group = v.findViewById(R.id.group);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            M3uLink.Entry e = shown.get(position);
            h.name.setText(e.name);
            h.group.setText(e.group == null || e.group.isEmpty() ? "" : e.group);
            h.group.setVisibility(e.group == null || e.group.isEmpty() ? View.GONE : View.VISIBLE);
            Glide.with(h.logo).load(e.logo)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(h.logo);
            h.itemView.setOnClickListener(v -> startActivity(ChannelDetailActivity.forLink(
                    PlaylistActivity.this, e.url, e.name, e.logo, e.userAgent, e.referrer)));
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }
    }
}
