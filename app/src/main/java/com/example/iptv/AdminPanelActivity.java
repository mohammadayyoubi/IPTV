package com.example.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.google.firebase.auth.FirebaseAuth;

public class AdminPanelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        // Categories
        LinearLayout manageCategories = findViewById(R.id.manageCategoriesButton);
        manageCategories.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, activity_category_management.class);
            startActivity(intent);
        });

        // Channels (placeholder)
        LinearLayout manageChannels = findViewById(R.id.manageChannelsButton);
        manageChannels.setOnClickListener(v -> {
            // TODO: Replace with ChannelManagementActivity
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
            // TODO: Replace with AdminUserManagementActivity
        });

        // Logout
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminPanelActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
