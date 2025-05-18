package com.example.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

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
    private List<Channel> allChannels;

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

        searchEditText = findViewById(R.id.searchEditText);
        Button addButton = findViewById(R.id.addChannelButton);

        allChannels = channelDAO.getAll();
        adapter = new ChannelAdapter(this, allChannels, countryDAO, categoryDAO, this::refreshChannelList);
        recyclerView.setAdapter(adapter);

        // Filter as you type
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity_channel_management.this, activity_add_channel.class);
            startActivity(intent);
        });
    }

    private void refreshChannelList() {
        allChannels = channelDAO.getAll();
        filter(searchEditText.getText().toString());
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
        refreshChannelList(); // Refresh when returning from add/edit screen
    }
}
