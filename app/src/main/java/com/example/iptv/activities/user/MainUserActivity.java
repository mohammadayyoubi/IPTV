package com.example.iptv.activities.user;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.iptv.Fragments.user.AllChannelsFragment;
import com.example.iptv.Fragments.user.CategoriesFragment;
import com.example.iptv.Fragments.user.CountriesFragment;
import com.example.iptv.Fragments.user.FavoritesFragment;
import com.example.iptv.R;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;

public class MainUserActivity extends BaseActivity {

    // A BottomNavigationView on phones, a NavigationRailView on wide screens (layout-w600dp)
    private NavigationBarView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_user);

        navView = findViewById(R.id.bottom_nav_view);
        View container = findViewById(R.id.fragment_container);

        // The nav views pad themselves for the system bars; the content only needs the
        // status bar on top, plus the navigation bar at the bottom when there is no bottom nav.
        UiUtils.applyInsets(container, true, navView instanceof NavigationRailView);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AllChannelsFragment())
                    .commit();
            navView.setSelectedItemId(R.id.nav_all_channels);
        }

        navView.setOnItemSelectedListener(item -> {
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
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Re-selecting the current tab does nothing (instead of rebuilding the page)
        navView.setOnItemReselectedListener(item -> { });
    }
}
