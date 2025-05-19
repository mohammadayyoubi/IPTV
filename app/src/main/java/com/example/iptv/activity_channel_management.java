package com.example.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Channel;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class activity_channel_management extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChannelAdapter adapter;
    private EditText searchEditText;
    private ChannelDAO channelDAO;
    private CountryDAO countryDAO;
    private CategoryDAO categoryDAO;
    private List<Channel> allChannels = new ArrayList<>();
    private ProgressBar progressBar; // NEW: show loading spinner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_management);

        DBHelper dbHelper = new DBHelper(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());

        recyclerView = findViewById(R.id.channelRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.GONE); // Hide until data is loaded

        progressBar = findViewById(R.id.progressBar); // Add this in your XML layout
        progressBar.setVisibility(View.VISIBLE);

        searchEditText = findViewById(R.id.searchEditText);
        Button addButton = findViewById(R.id.addChannelButton);

        // Initial adapter with empty list to avoid "no adapter" error
        adapter = new ChannelAdapter(this, new ArrayList<>(), countryDAO, categoryDAO, this::refreshChannelList);
        recyclerView.setAdapter(adapter);

        // Load channels from DB in background
        loadChannelsFromDatabase();

        // Filter on search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Add new channel
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity_channel_management.this, activity_add_channel.class);
            startActivity(intent);
        });

        Button deleteAllButton = findViewById(R.id.deleteAllChannelsButton);
        deleteAllButton.setOnClickListener(v -> {
            // Confirm deletion (optional)
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete all channels?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
                        channelDAO.deleteAllChannels(); // Add this method to ChannelDAO if not present
                        Toast.makeText(this, "All channels deleted", Toast.LENGTH_SHORT).show();
                        // Refresh RecyclerView
                        refreshChannelList(); // Assuming this method reloads your channel list
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    private void loadChannelsFromDatabase() {
        new Thread(() -> {
            List<Channel> loaded = channelDAO.getAll(); // Load safely in background
            runOnUiThread(() -> {
                allChannels.clear();
                allChannels.addAll(loaded);
                adapter.updateData(allChannels);
                progressBar.setVisibility(View.GONE); // Hide spinner
                recyclerView.setVisibility(View.VISIBLE); // Show list
            });
        }).start();
    }

    private void refreshChannelList() {
        loadChannelsFromDatabase(); // Use thread-safe loading again
    }

    private void filter(String text) {
        List<Channel> filteredList = new ArrayList<>();
        for (Channel channel : allChannels) {
            if (channel.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(channel);
            }
        }
        adapter.updateData(filteredList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChannelList(); // Refresh when returning
    }
}
