package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.adapters.admin.CountryAdapter;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.InputDialog;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class activity_country_management extends BaseActivity {

    private EditText searchEditText;
    private CountryAdapter adapter;
    private CountryDAO countryDAO;
    private TextView countText;
    private View emptyState;
    private int totalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_management);
        UiUtils.applyInsets(findViewById(R.id.root), true, true);

        searchEditText = findViewById(R.id.AllChannelsSearchEditText);
        RecyclerView recyclerView = findViewById(R.id.countryRecyclerView);
        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        DBHelper dbHelper = new DBHelper(this);
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_admin_row);
        List<Country> initial = countryDAO.getAll();
        totalCount = initial.size();
        adapter = new CountryAdapter(this, initial, new CountryAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Country country) {
                showCountryDialog(country);
            }

            @Override
            public void onDelete(Country country) {
                InputDialog.confirm(activity_country_management.this, "Delete country",
                        "Delete \"" + country.getName() + "\"? Its channels will be deleted too.",
                        getString(R.string.delete), true, () -> {
                            countryDAO.delete(country.getId());
                            loadCountries();
                        });
            }
        });
        recyclerView.setAdapter(adapter);
        UiUtils.onAdapterChanged(adapter, this::updateState);
        updateState();

        ExtendedFloatingActionButton addButton = findViewById(R.id.addCountryButton);
        addButton.setOnClickListener(v -> showCountryDialog(null));
        UiUtils.shrinkFabOnScroll(recyclerView, addButton);

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
        });

        // Adds every country the phone knows (ISO 3166) that isn't here yet, with its flag
        findViewById(R.id.loadAllCountriesButton).setOnClickListener(v -> {
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
            int added = com.example.iptv.getFromInternet.loadAllCountries(new com.example.iptv.getFromInternet.Daos(this,
                    new com.example.iptv.database.ChannelDAO(db), countryDAO,
                    new com.example.iptv.database.CategoryDAO(db), new com.example.iptv.database.ChannelServerDAO(db)));
            loadCountries();
            Toast.makeText(this, added == 0 ? "All countries are already here" : added + " countries added",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.deleteAllCountriesButton).setOnClickListener(v ->
                InputDialog.confirm(this, "Delete all countries",
                        "This also deletes every channel that belongs to a country. It can't be undone.",
                        getString(R.string.delete_all), true, () -> {
                            countryDAO.deleteAllCountries();
                            loadCountries();
                            Toast.makeText(this, "All countries deleted", Toast.LENGTH_SHORT).show();
                        }));
    }

    private void loadCountries() {
        List<Country> countries = countryDAO.getAll();
        totalCount = countries.size();
        adapter.updateData(countries);
        // Keep the current search applied
        if (searchEditText.getText() != null && searchEditText.getText().length() > 0) {
            adapter.filter(searchEditText.getText().toString());
        }
    }

    private void updateState() {
        countText.setText(UiUtils.quantity(this, R.plurals.count_countries, totalCount));
        if (totalCount == 0) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_public,
                    R.string.empty_countries_title, R.string.empty_countries_body);
        } else {
            UiUtils.showEmptyState(emptyState, adapter.getItemCount() == 0, R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }

    private void showCountryDialog(Country countryToEdit) {
        new InputDialog(this)
                .title(countryToEdit == null ? getString(R.string.add_country) : "Edit country")
                .field(getString(R.string.country_name), countryToEdit != null ? countryToEdit.getName() : null)
                .secondField(getString(R.string.flag_url), countryToEdit != null ? countryToEdit.getFlagUrl() : null)
                .onSubmit((name, flagUrl) -> {
                    if (name.isEmpty()) return;

                    if (countryToEdit == null) {
                        countryDAO.insert(new Country(name, flagUrl));
                    } else {
                        countryToEdit.setName(name);
                        countryToEdit.setFlagUrl(flagUrl);
                        countryDAO.update(countryToEdit);
                    }
                    loadCountries();
                })
                .show();
    }
}
