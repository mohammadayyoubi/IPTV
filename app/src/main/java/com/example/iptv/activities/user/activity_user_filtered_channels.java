package com.example.iptv.activities.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.Country;
import com.example.iptv.HomeCountry;
import com.example.iptv.R;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelQuery;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.ui.AvailabilityFilter;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.ChannelPager;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class activity_user_filtered_channels extends BaseActivity {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private EditText searchEditText;
    private TextView pageTitle;
    private TextView countText;
    private View emptyState;
    private AvailabilityFilter availabilityFilter;
    private ChannelPager pager;

    private String filterType;  // "country" or "category"
    private int filterId;       // countryId or categoryId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_filtered_channels);
        UiUtils.applyInsets(findViewById(R.id.root), true, false);
        UiUtils.applyInsets(findViewById(R.id.recycler_filtered_channels), false, true);

        recyclerView = findViewById(R.id.recycler_filtered_channels);
        searchEditText = findViewById(R.id.FilteredChannelsSearchEditText);
        pageTitle = findViewById(R.id.filteredChannelsPageLable);
        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        filterType = getIntent().getStringExtra("filterType");
        filterId = getIntent().getIntExtra("filterId", -1);
        String filterName = getIntent().getStringExtra("filterName");

        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_channel);
        adapter = new ChannelUserAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        DBHelper dbHelper = new DBHelper(this);
        ChannelQuery query = new ChannelQuery();
        String title = filterName;
        if ("country".equals(filterType)) {
            query.countryId = filterId;
            Country country = new CountryDAO(dbHelper.getWritableDatabase()).getById(filterId);
            if (country != null) {
                title = country.getName();
                ImageView flag = findViewById(R.id.headerFlag);
                flag.setVisibility(View.VISIBLE);
                Glide.with(this).load(country.getFlagUrl())
                        .placeholder(R.drawable.ic_flag_placeholder)
                        .error(R.drawable.ic_flag_placeholder)
                        .into(flag);
            }
        } else if ("category".equals(filterType)) {
            query.categoryId = filterId;
            Category category = new CategoryDAO(dbHelper.getWritableDatabase()).getById(filterId);
            if (category != null) {
                title = category.getName();
                ImageView icon = findViewById(R.id.headerCategoryIcon);
                icon.setVisibility(View.VISIBLE);
                UiUtils.applyCategoryIcon(icon, category.getName());
            }
        }
        pageTitle.setText(title != null ? title : getString(R.string.nav_home));

        pager = new ChannelPager(this, recyclerView, new ChannelPager.Sink() {
            @Override
            public void onFirstPage(List<Channel> channels) {
                adapter.updateList(channels);
                recyclerView.scrollToPosition(0);
            }

            @Override
            public void onNextPage(List<Channel> channels) {
                adapter.appendList(channels);
            }

            @Override
            public void onCounts(ChannelQuery.Counts counts) {
                showCounts(counts);
            }
        });

        availabilityFilter = new AvailabilityFilter(findViewById(R.id.availabilityFilter),
                findViewById(R.id.filterButton), availability -> {
            ChannelQuery q = pager.getQuery().copy();
            q.availability = availability;
            pager.setQuery(q);
        });
        query.availability = availabilityFilter.getAvailability();
        // In a category, the viewer's own country comes first
        if ("category".equals(filterType)) query.priorityCountryId = HomeCountry.countryId(this);
        pager.setQuery(query);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pager.setSearchDebounced(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void showCounts(ChannelQuery.Counts counts) {
        availabilityFilter.updateCounts(counts);
        int shown = counts.of(pager.getQuery().availability);
        String text = UiUtils.quantity(this, R.plurals.count_channels, shown);
        if (pager.getQuery().availability != ChannelQuery.Availability.WATCHABLE) {
            text += " · " + availabilityFilter.label();
        }
        countText.setText(text);
        boolean searching = !pager.getQuery().search.trim().isEmpty();
        if (counts.all == 0 && !searching) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_live_tv,
                    R.string.empty_channels_title, R.string.empty_filtered_body);
        } else if (shown == 0) {
            availabilityFilter.showEmpty(emptyState, searching);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Back from the player: statuses may have changed
        pager.reload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pager.destroy();
    }
}
