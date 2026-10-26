package com.example.iptv.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.adapters.admin.ChannelAdapter;
import com.example.iptv.OOP.Channel;
import com.example.iptv.R;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelQuery;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.ChannelPager;
import com.example.iptv.ui.InputDialog;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class activity_channel_management extends BaseActivity {

    private RecyclerView recyclerView;
    private ChannelAdapter adapter;
    private EditText searchEditText;
    private View progressBar;
    private TextView countText;
    private View emptyState;
    private ChannelPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_management);
        UiUtils.applyInsets(findViewById(R.id.root), true, true);

        DBHelper dbHelper = DBHelper.getInstance(this);
        CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
        CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());

        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.channelRecyclerView);
        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_admin_row);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        searchEditText = findViewById(R.id.AllChannelsSearchEditText);

        adapter = new ChannelAdapter(this, new ArrayList<>(), countryDAO, categoryDAO, () -> pager.refresh());
        recyclerView.setAdapter(adapter);

        // Paged from SQLite: the list stays responsive with every channel in the world imported
        pager = new ChannelPager(this, recyclerView, new ChannelPager.Sink() {
            @Override
            public void onFirstPage(List<Channel> channels, boolean newQuery) {
                progressBar.setVisibility(View.GONE);
                adapter.updateData(channels);
                if (newQuery) recyclerView.scrollToPosition(0);
            }

            @Override
            public void onNextPage(List<Channel> channels) {
                adapter.appendData(channels);
            }

            @Override
            public void onCounts(ChannelQuery.Counts counts) {
                countText.setText(UiUtils.quantity(activity_channel_management.this, R.plurals.count_channels, counts.all));
                boolean searching = !pager.getQuery().search.trim().isEmpty();
                if (counts.all == 0 && !searching) {
                    UiUtils.showEmptyState(emptyState, true, R.drawable.ic_live_tv,
                            R.string.empty_channels_title, R.string.empty_channels_body);
                } else {
                    UiUtils.showEmptyState(emptyState, counts.all == 0, R.drawable.ic_search,
                            R.string.empty_search_title, R.string.empty_search_body);
                }
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pager.setSearchDebounced(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ExtendedFloatingActionButton addButton = findViewById(R.id.addChannelButton);
        addButton.setOnClickListener(v ->
                startActivity(new Intent(activity_channel_management.this, activity_add_channel.class)));
        UiUtils.shrinkFabOnScroll(recyclerView, addButton);

        findViewById(R.id.deleteAllChannelsButton).setOnClickListener(v ->
                InputDialog.confirm(this, getString(R.string.adm_delete_all_channels),
                        getString(R.string.adm_delete_all_channels_body),
                        getString(R.string.delete_all), true, () -> {
                            new ChannelDAO(dbHelper.getWritableDatabase()).deleteAllChannels();
                            Toast.makeText(this, R.string.adm_all_channels_deleted, Toast.LENGTH_SHORT).show();
                            pager.reload();
                        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        pager.refresh(); // Refresh in place when returning from add / edit
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pager.destroy();
    }
}
