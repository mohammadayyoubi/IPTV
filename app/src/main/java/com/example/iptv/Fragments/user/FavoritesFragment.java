package com.example.iptv.Fragments.user;

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

import com.example.iptv.R;
import com.example.iptv.OOP.Channel;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.database.ChannelQuery;
import com.example.iptv.ui.ChannelPager;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private TextView countText;
    private View emptyState;
    private ChannelPager pager;

    public FavoritesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recycler_favorites);
        countText = view.findViewById(R.id.countText);
        emptyState = view.findViewById(R.id.emptyState);
        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_channel);

        adapter = new ChannelUserAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        pager = new ChannelPager(requireContext(), recyclerView, new ChannelPager.Sink() {
            @Override
            public void onFirstPage(List<Channel> channels, boolean newQuery) {
                adapter.updateList(channels);
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
        pager.getQuery().favoritesOnly = true;
        pager.getQuery().priorityCountryId = com.example.iptv.HomeCountry.countryId(requireContext());

        EditText search = view.findViewById(R.id.FavoritesSearchEditText);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pager.setSearchDebounced(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    private void showCounts(ChannelQuery.Counts counts) {
        countText.setText(UiUtils.quantity(requireContext(), R.plurals.count_channels, counts.all));
        boolean searching = !pager.getQuery().search.trim().isEmpty();
        if (counts.all == 0 && searching) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        } else {
            UiUtils.showEmptyState(emptyState, counts.all == 0, R.drawable.ic_favorite_border,
                    R.string.empty_favorites_title, R.string.empty_favorites_body);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        pager.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pager.destroy();
    }
}
