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

import com.example.iptv.R;
import com.example.iptv.OOP.Country;
import com.example.iptv.activities.user.activity_user_filtered_channels;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CountryAdapter;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class CountriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CountryAdapter adapter;
    private List<Country> countryList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView countText;
    private View emptyState;

    public CountriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_countries, container, false);

        recyclerView = view.findViewById(R.id.recycler_countries);
        searchEditText = view.findViewById(R.id.CountriesUserSearchEditText);
        countText = view.findViewById(R.id.countText);
        emptyState = view.findViewById(R.id.emptyState);

        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_row);

        dbHelper = DBHelper.getInstance(requireContext());
        CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
        countryList = countryDAO.getAll();

        adapter = new CountryAdapter(requireContext(), new ArrayList<>(countryList), country -> {
            Intent intent = new Intent(requireContext(), activity_user_filtered_channels.class);
            intent.putExtra("filterType", "country");
            intent.putExtra("filterId", country.getId());
            intent.putExtra("filterName", country.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCountries(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        filterCountries("");
        return view;
    }

    private void filterCountries(String query) {
        List<Country> tempList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        for (Country country : countryList) {
            // Match the English name and the translated one (e.g. "Lebanon" / "لبنان")
            String label = UiUtils.countryLabel(country.getName()).toLowerCase();
            if (country.getName().toLowerCase().contains(lowerQuery) || label.contains(lowerQuery)) {
                tempList.add(country);
            }
        }
        adapter.updateList(tempList);

        countText.setText(UiUtils.quantity(requireContext(), R.plurals.count_countries, tempList.size()));
        if (countryList.isEmpty()) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_public,
                    R.string.empty_countries_title, R.string.empty_countries_body);
        } else {
            UiUtils.showEmptyState(emptyState, tempList.isEmpty(), R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }
}
