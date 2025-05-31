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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CountryAdapter;

import java.util.ArrayList;
import java.util.List;

public class CountriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CountryAdapter adapter;
    private List<Country> countryList;
    private List<Country> filteredList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView pageTitle;

    public CountriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_countries, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_countries);
        searchEditText = view.findViewById(R.id.CountriesUserSearchEditText);

        if (pageTitle != null) {
            pageTitle.setText("All Countries List");
        } else {
            System.err.println("CountriesListUserPageLabel not found in layout.");
        }

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        // Initialize database helper and DAO
        dbHelper = new DBHelper(requireContext());
        CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
        countryList = countryDAO.getAll(); // Fetch all countries
        filteredList = new ArrayList<>(countryList); // Initialize filtered list

        // Initialize adapter
        adapter = new CountryAdapter(requireContext(), filteredList, country -> {
            // Handle country click: e.g., open filtered channels by country
        });

        recyclerView.setAdapter(adapter);

        // Add search functionality
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

        return view;
    }

    private void filterCountries(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(countryList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Country country : countryList) {
                if (country.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(country);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
