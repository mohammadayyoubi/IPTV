package com.example.iptv.Fragments.user;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iptv.R;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CountryAdapter;

import java.util.List;

public class CountriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CountryAdapter adapter;
    private List<Country> countryList;
    private DBHelper dbHelper;

    public CountriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_countries, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_countries);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // 3-column grid like channels

        // Initialize database helper and DAO
        dbHelper = new DBHelper(requireContext());
        CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        // Fetch all countries
        countryList = countryDAO.getAll(); // Ensure getAllCountries() is implemented

        // Initialize adapter
        adapter = new CountryAdapter(requireContext(), countryList, country -> {
            // Handle country click: e.g., open filtered channel list by country
        });

        recyclerView.setAdapter(adapter);

        return view;
    }
}
