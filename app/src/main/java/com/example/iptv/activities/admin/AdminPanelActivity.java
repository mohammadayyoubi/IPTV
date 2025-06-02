package com.example.iptv.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.iptv.Interfaces.CategoryCallback;
import com.example.iptv.OOP.Category;
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



        // Manage Categories
        CardView manageCategories = findViewById(R.id.manageCategoriesButton);
        manageCategories.setOnClickListener(v -> startActivity(new Intent(this, activity_category_management.class)));

        // Manage Channels
        CardView manageChannels = findViewById(R.id.manageChannelsButton);
        manageChannels.setOnClickListener(v -> startActivity(new Intent(this, activity_channel_management.class)));

        // Manage Countries
        CardView manageCountries = findViewById(R.id.manageCountriesButton);
        manageCountries.setOnClickListener(v -> startActivity(new Intent(this, activity_country_management.class)));

        // Manage Users
        CardView manageUsers = findViewById(R.id.manageUsersButton);
        manageUsers.setOnClickListener(v -> startActivity(new Intent(this, activity_user_management.class)));

        // Logout
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Fetch data from the internet if database is empty
        if (countryDao.count() == 0) {
            ArrayList<Country> countries = getAllCountries();
            for (Country country : countries) {
                countryDao.insert(country);
            }
            refreshDashboard(); // This is synchronous, okay to call here
        }

        if (categoryDao.count() == 0) {

            getFromInternet.getAllCategories(new CategoryCallback() {
                @Override
                public void onCategoriesLoaded(ArrayList<Category> categories) {
                    for (Category c : categories) {
                        categoryDao.insert(c);
                        Log.d("InsertedCategory", c.getName());
                    }

                    getFromInternet.getUserCountryCode(countryCode -> {
                        getFromInternet.getAllChannelsByCountry(
                                countryCode,
                                ChannelDao,
                                countryDao,
                                categoryDao,
                                ChannelServerDao,
                                channels -> {
                                    Log.d("Done", "Loaded and saved " + channels.size() + " channels");
                                    runOnUiThread(() -> {
                                        refreshDashboard();  // Refresh AFTER all data inserted
                                    });
                                }
                        );
                    });
                }
            });
        } else if (ChannelDao.count() == 0) {
            getFromInternet.getUserCountryCode(countryCode -> {
                getFromInternet.getAllChannelsByCountry(
                        countryCode,
                        ChannelDao,
                        countryDao,
                        categoryDao,
                        ChannelServerDao,
                        channels -> {
                            Log.d("Done", "Loaded and saved " + channels.size() + " channels");
                            runOnUiThread(() -> {
                                refreshDashboard();  // Refresh AFTER data insertion
                            });
                        }
                );
            });
        }

        // Clear Database Button (CardView)
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

        refreshDashboard();  // Initial refresh
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
