package com.example.iptv.activities.admin;

import static com.example.iptv.getFromInternet.getAllCountries;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
        dbHelper= new DBHelper(this);
         countryDao=new CountryDAO(dbHelper.getWritableDatabase());
        categoryDao=new CategoryDAO(dbHelper.getWritableDatabase());
        ChannelDao=new ChannelDAO(dbHelper.getWritableDatabase());
        ChannelServerDao=new ChannelServerDAO(dbHelper.getWritableDatabase());

        // Categories
        LinearLayout manageCategories = findViewById(R.id.manageCategoriesButton);
        manageCategories.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, activity_category_management.class);
            startActivity(intent);
        });


        // Channels (placeholder)
        LinearLayout manageChannels = findViewById(R.id.manageChannelsButton);
        manageChannels.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, activity_channel_management.class);
            startActivity(intent);

        });

        // Countries (placeholder)
        LinearLayout manageCountries = findViewById(R.id.manageCountriesButton);
        manageCountries.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, activity_country_management.class);
            startActivity(intent);
        });

        // Admin Users (placeholder)
        LinearLayout manageUsers = findViewById(R.id.manageUsersButton);
        manageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, activity_user_management.class);
            startActivity(intent);

        });

        // Logout
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminPanelActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        /// /////////////////// get data if not already inserted - get from internet (iptv github api) when run//////////////////////////
        if(countryDao.count()==0){
            ArrayList<Country> countries=getAllCountries();
            for( Country country : countries){
                countryDao.insert(country) ;
            }
        }
        if (categoryDao.count()==0) { // Check if the local database has no categories yet
            getFromInternet.getAllCategories(new CategoryCallback() {
                @Override
                public void onCategoriesLoaded(ArrayList<Category> categories) {
                    // This is called after categories are fetched from the internet (on the main thread)

                    for (Category c : categories) {
                        categoryDao.insert(c); // Insert each category into the local Room database
                        Log.d("InsertedCategory", c.getName()); //log for debugging
                    }
                    // Now that categories are inserted, fetch channels and streams

//                    Log.d("FetchingChannelsAndStreams", "Fetching channels and streams...");
//                    getFromInternet.getAllChannelsAndStreams(ChannelDao, countryDao, categoryDao, ChannelServerDao);

                    //fetch channels and streems accourding to user country
                    Log.d("FetchingChannelsAndStreams", "Fetching channels and streams...");
                    getFromInternet.getUserCountryCode(countryCode -> {
                        Log.d("CountryCode", "User is from: " + countryCode);

                        getFromInternet.getAllChannelsByCountry(
                                countryCode, // Or from getUserCountryCode(...)
                                ChannelDao,
                                countryDao,
                                categoryDao,
                                ChannelServerDao,
                                channels -> Log.d("Done", "Loaded and saved " + channels.size() + " channels")
                        );

                    });


                }
            });
        }else{
            if(ChannelDao.count()==0) {
//                Log.d("FetchingChannelsAndStreams", "Fetching channels and streams...");
//                getFromInternet.getAllChannelsAndStreams(ChannelDao, countryDao, categoryDao, ChannelServerDao);

                getFromInternet.getUserCountryCode(countryCode -> {
                    Log.d("CountryCode", "User is from: " + countryCode);

                    getFromInternet.getAllChannelsByCountry(
                            countryCode, // Or from getUserCountryCode(...)
                            ChannelDao,
                            countryDao,
                            categoryDao,
                            ChannelServerDao,
                            channels -> Log.d("Done", "Loaded and saved " + channels.size() + " channels")
                    );

                });

            }
        }

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
//mohamadayoubi050@gmail.com