package com.example.iptv.Fragments.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.HomeCountry;
import com.example.iptv.R;
import com.example.iptv.activities.admin.LoginActivity;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelQuery;
import com.example.iptv.database.DBHelper;
import com.example.iptv.database.HistoryDAO;
import com.example.iptv.ui.AvailabilityFilter;
import com.example.iptv.ui.ChannelPager;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class AllChannelsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private EditText searchEditText;
    private TextView countText;
    private View emptyState;
    private AvailabilityFilter availabilityFilter;
    private ChannelPager pager;

    public AllChannelsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_channels, container, false);

        recyclerView = view.findViewById(R.id.recycler_all_channels);
        searchEditText = view.findViewById(R.id.AllChannelsSearchEditText);
        countText = view.findViewById(R.id.countText);
        emptyState = view.findViewById(R.id.emptyState);

        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_channel);
        adapter = new ChannelUserAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Channels are paged from SQLite as the user scrolls (see ChannelPager)
        pager = new ChannelPager(requireContext(), recyclerView, new ChannelPager.Sink() {
            @Override
            public void onFirstPage(List<Channel> channels, boolean newQuery) {
                adapter.updateList(channels);
                if (newQuery) recyclerView.scrollToPosition(0);
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

        availabilityFilter = new AvailabilityFilter(view.findViewById(R.id.availabilityFilter),
                view.findViewById(R.id.filterButton), availability -> {
            ChannelQuery q = pager.getQuery().copy();
            q.availability = availability;
            pager.setQuery(q);
        });
        pager.getQuery().availability = availabilityFilter.getAvailability();
        availabilityFilter.setOnClearHistory(() -> {
            new HistoryDAO(DBHelper.getInstance(requireContext()).getWritableDatabase()).clear();
            pager.reload();
        });
        // Channels of the viewer's own country are listed first
        pager.getQuery().priorityCountryId = HomeCountry.countryId(requireContext());
        HomeCountry.refreshAsync(requireContext(), () -> {
            if (!isAdded()) return;
            ChannelQuery q = pager.getQuery().copy();
            q.priorityCountryId = HomeCountry.countryId(requireContext());
            pager.setQuery(q);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pager.setSearchDebounced(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        view.findViewById(R.id.btn_play_link).setOnClickListener(v ->
                com.example.iptv.ui.PlayLinkDialog.show(requireActivity()));
        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.example.iptv.activities.user.SettingsActivity.class)));
        view.findViewById(R.id.btn_admin_login).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));

        return view;
    }

    private void showCounts(ChannelQuery.Counts counts) {
        availabilityFilter.updateCounts(counts);
        int shown = counts.of(pager.getQuery().availability);
        String text = UiUtils.quantity(requireContext(), R.plurals.count_channels, shown);
        if (pager.getQuery().availability != ChannelQuery.Availability.WATCHABLE) {
            text += " · " + availabilityFilter.label();
        }
        countText.setText(text);
        boolean searching = !pager.getQuery().search.trim().isEmpty();
        if (counts.all == 0 && !searching) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_live_tv,
                    R.string.empty_channels_title, R.string.empty_channels_body);
        } else if (shown == 0) {
            availabilityFilter.showEmpty(emptyState, searching);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Picks up favorites / statuses changed on the player screen, keeping the scroll position
        pager.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pager.destroy();
    }
}
