package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.adapters.admin.CountryAdapter;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

public class activity_country_management extends AppCompatActivity {

    private EditText searchEditText;
    private CountryAdapter adapter;
    private CountryDAO countryDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_management);

        searchEditText = findViewById(R.id.searchEditText);
        Button addButton = findViewById(R.id.addCountryButton);
        RecyclerView recyclerView = findViewById(R.id.countryRecyclerView);

        DBHelper dbHelper = new DBHelper(this);
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CountryAdapter(this, countryDAO.getAll(), new CountryAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Country country) {
                showCountryDialog(country);
            }

            @Override
            public void onDelete(Country country) {
                new AlertDialog.Builder(activity_country_management.this)
                        .setTitle("Delete Country")
                        .setMessage("Are you sure you want to delete \"" + country.getName() + "\"?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            countryDAO.delete(country.getId());
                            loadCountries();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

        });
        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(v -> showCountryDialog(null));
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
        });

        Button deleteAllButton = findViewById(R.id.deleteAllCountriesButton);
        deleteAllButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete All Countries")
                    .setMessage("Are you sure you want to delete all countries?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        countryDAO.deleteAllCountries(); // Your DAO method
                        loadCountries(); // Reload list in RecyclerView
                        Toast.makeText(this, "All countries deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    private void loadCountries() {
        adapter.updateData(countryDAO.getAll());
    }

    private void showCountryDialog(Country countryToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(countryToEdit == null ? "Add Country" : "Edit Country");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Country Name");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        final EditText flagUrlInput = new EditText(this);
        flagUrlInput.setHint("Flag Image URL");
        flagUrlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        layout.addView(flagUrlInput);

        if (countryToEdit != null) {
            nameInput.setText(countryToEdit.getName());
            flagUrlInput.setText(countryToEdit.getFlagUrl());
        }

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String flagUrl = flagUrlInput.getText().toString().trim();
            if (name.isEmpty()) return;

            if (countryToEdit == null) {
                countryDAO.insert(new Country( name, flagUrl));
            } else {
                countryToEdit.setName(name);
                countryToEdit.setFlagUrl(flagUrl);
                countryDAO.update(countryToEdit);
            }
            loadCountries();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
