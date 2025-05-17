package com.example.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

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
            // TODO: Replace with CountryManagementActivity
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
    }
}
