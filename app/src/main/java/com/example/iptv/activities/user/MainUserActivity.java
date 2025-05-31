package com.example.iptv.activities.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.iptv.Fragments.user.AllChannelsFragment;
import com.example.iptv.Fragments.user.CategoriesFragment;
import com.example.iptv.Fragments.user.CountriesFragment;
import com.example.iptv.Fragments.user.FavoritesFragment;
import com.example.iptv.R;
import com.example.iptv.activities.admin.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainUserActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavView;
    private Button buttonAdminSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_user);

        bottomNavView = findViewById(R.id.bottom_nav_view);
        buttonAdminSignIn = findViewById(R.id.button_admin_signin);

        // Default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AllChannelsFragment())
                    .commit();
            bottomNavView.setSelectedItemId(R.id.nav_all_channels);
        }

        bottomNavView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_all_channels) {
                selectedFragment = new AllChannelsFragment();
            } else if (item.getItemId() == R.id.nav_categories) {
                selectedFragment = new CategoriesFragment();
            } else if (item.getItemId() == R.id.nav_countries) {
                selectedFragment = new CountriesFragment();
            } else if (item.getItemId() == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Admin Sign-In Button Click
        buttonAdminSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(MainUserActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}
