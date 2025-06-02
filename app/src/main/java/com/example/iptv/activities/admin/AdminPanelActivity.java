package com.example.iptv.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.iptv.Interfaces.CategoryCallback;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.getFromInternet;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import static com.example.iptv.getFromInternet.getAllCountries;

public class AdminPanelActivity extends AppCompatActivity {
    DBHelper dbHelper;
    private CountryDAO countryDao;
    private CategoryDAO categoryDao;
    private ChannelDAO ChannelDao;
    private ChannelServerDAO ChannelServerDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        dbHelper = new DBHelper(this);
        countryDao = new CountryDAO(dbHelper.getWritableDatabase());
        categoryDao = new CategoryDAO(dbHelper.getWritableDatabase());
        ChannelDao = new ChannelDAO(dbHelper.getWritableDatabase());
        ChannelServerDao = new ChannelServerDAO(dbHelper.getWritableDatabase());

        // Manage buttons
        CardView manageCategories = findViewById(R.id.manageCategoriesButton);
        manageCategories.setOnClickListener(v -> startActivity(new Intent(this, activity_category_management.class)));

        CardView manageChannels = findViewById(R.id.manageChannelsButton);
        manageChannels.setOnClickListener(v -> startActivity(new Intent(this, activity_channel_management.class)));

        CardView manageCountries = findViewById(R.id.manageCountriesButton);
        manageCountries.setOnClickListener(v -> startActivity(new Intent(this, activity_country_management.class)));

        CardView manageUsers = findViewById(R.id.manageUsersButton);
        manageUsers.setOnClickListener(v -> startActivity(new Intent(this, activity_user_management.class)));

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Auto-load countries if needed
        if (countryDao.count() == 0) {
            ArrayList<Country> countries = getAllCountries();
            for (Country country : countries) {
                countryDao.insert(country);
            }
            refreshDashboard();
        }

        // Auto-load categories if needed
        if (categoryDao.count() == 0) {
            getFromInternet.getAllCategories(new CategoryCallback() {
                @Override
                public void onCategoriesLoaded(ArrayList<Category> categories) {
                    for (Category c : categories) {
                        categoryDao.insert(c);
                        Log.d("InsertedCategory", c.getName());
                    }
                    runOnUiThread(() -> refreshDashboard());
                }
            });
        }

        // Clear Database Button
        CardView clearDbCard = findViewById(R.id.clearDatabaseButton);
        clearDbCard.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Data")
                    .setMessage("Are you sure you want to delete all data? This cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.resetDatabase();
                        Toast.makeText(this, "All data cleared!", Toast.LENGTH_SHORT).show();
                        refreshDashboard();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // My Country Channels Button
        CardView getMyCountryChannelsButton = findViewById(R.id.getMyCountryChannelsButton);
        getMyCountryChannelsButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Get My Country Channels")
                    .setMessage("Do you want to load channels for your country?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        getFromInternet.getUserCountryCode(countryCode -> {
                            getFromInternet.getAllChannelsByCountry(
                                    countryCode,
                                    ChannelDao,
                                    countryDao,
                                    categoryDao,
                                    ChannelServerDao,
                                    channels -> {
                                        Toast.makeText(this, "Loaded " + channels.size() + " channels for " + countryCode, Toast.LENGTH_SHORT).show();
                                        refreshDashboard();
                                    }
                            );
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // All Countries Channels Button with input dialog
        CardView getAllCountriesChannelsButton = findViewById(R.id.getAllCountriesChannelsButton);
        getAllCountriesChannelsButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Get All Countries Channels");

            final EditText input = new EditText(this);
            input.setHint("nb of channels per country (or leave empty for all)");
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton("Load", (dialog, which) -> {
                String inputText = input.getText().toString().trim();
                final int maxChannelsPerCountry;
                if (inputText.isEmpty()) {
                    maxChannelsPerCountry = -1;
                } else {
                    try {
                        maxChannelsPerCountry = Integer.parseInt(inputText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number, loading all channels", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Use new optimized method
                getFromInternet.getAllChannelsOnceAndFilter(
                        ChannelDao,
                        countryDao,
                        categoryDao,
                        ChannelServerDao,
                        maxChannelsPerCountry,
                        () -> {
                            Toast.makeText(this, "All countries channels loaded", Toast.LENGTH_SHORT).show();
                            refreshDashboard();
                        });
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        });


        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    private void refreshDashboard() {
        TextView tvTotalChannels = findViewById(R.id.tvTotalChannels);
        TextView tvTotalCategories = findViewById(R.id.tvTotalCategories);
        TextView tvTotalCountries = findViewById(R.id.tvTotalCountries);

        DBHelper dbHelper = new DBHelper(this);
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getReadableDatabase());
        CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getReadableDatabase());
        CountryDAO countryDAO = new CountryDAO(dbHelper.getReadableDatabase());

        tvTotalChannels.setText(String.valueOf(channelDAO.count()));
        tvTotalCategories.setText(String.valueOf(categoryDAO.count()));
        tvTotalCountries.setText(String.valueOf(countryDAO.count()));
    }
}
