package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.iptv.OOP.*;
import com.example.iptv.R;
import com.example.iptv.database.*;

import java.util.ArrayList;
import java.util.List;

public class activity_add_channel extends AppCompatActivity {
    private EditText nameEditText, logoUrlEditText, serverInput;
    private ImageView logoPreview;
    private Spinner categorySpinner, countrySpinner;
    private LinearLayout serverContainer;
    private Button addServerButton, saveButton;
    private ChannelDAO channelDAO;
    private ChannelServerDAO serverDAO;
    private CategoryDAO categoryDAO;
    private CountryDAO countryDAO;
    private List<Category> categories;
    private List<Country> countries;
    private List<String> serverUrls = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;
    private ArrayAdapter<Country> countryAdapter;
    public static final String EXTRA_CHANNEL_ID = "channel_id";
    private int editingChannelId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);

        DBHelper dbHelper = new DBHelper(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        serverDAO = new ChannelServerDAO(dbHelper.getWritableDatabase());
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        nameEditText = findViewById(R.id.channelNameEditText);
        logoUrlEditText = findViewById(R.id.logoUrlEditText);
        logoPreview = findViewById(R.id.logoPreviewImageView);
        categorySpinner = findViewById(R.id.categorySpinner);
        countrySpinner = findViewById(R.id.countrySpinner);
        serverInput = findViewById(R.id.serverUrlEditText);
        serverContainer = findViewById(R.id.serverListLayout);
        addServerButton = findViewById(R.id.addServerButton);
        saveButton = findViewById(R.id.saveChannelButton);

        loadCategories();
        loadCountries();

        logoUrlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String logoUrl = logoUrlEditText.getText().toString().trim();
                if (!logoUrl.isEmpty()) {
                    Glide.with(this).load(logoUrl).placeholder(R.drawable.placeholder).into(logoPreview);
                }
            }
        });

        addServerButton.setOnClickListener(v -> {
            String serverUrl = serverInput.getText().toString().trim();
            if (!serverUrl.isEmpty()) {
                addServerToLayout(serverUrl);
                serverUrls.add(serverUrl);
                serverInput.setText("");
            }
        });

        saveButton.setOnClickListener(v -> saveChannel());

        if (getIntent().hasExtra(EXTRA_CHANNEL_ID)) {
            editingChannelId = getIntent().getIntExtra(EXTRA_CHANNEL_ID, -1);
            if (editingChannelId != -1) {
                loadChannelData(editingChannelId);
            }
        }
    }

    private void loadCategories() {
        categories = categoryDAO.getAll();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void loadCountries() {
        countries = countryDAO.getAll();
        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countries);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);
    }

    private void loadChannelData(int channelId) {
        Channel channel = channelDAO.getById(channelId);
        if (channel == null) return;
        nameEditText.setText(channel.getName());
        logoUrlEditText.setText(channel.getLogoUrl());
        for (int i = 0; i < categories.size(); i++)
            if (categories.get(i).getId() == channel.getCategoryId())
                categorySpinner.setSelection(i);
        for (int i = 0; i < countries.size(); i++)
            if (countries.get(i).getId() == channel.getCountryId())
                countrySpinner.setSelection(i);
        serverUrls.clear();
        for (ChannelServer server : channel.getServers()) {
            serverUrls.add(server.getStreamUrl());
            addServerToLayout(server.getStreamUrl());
        }
        Glide.with(this).load(channel.getLogoUrl()).placeholder(R.drawable.placeholder).into(logoPreview);
    }

    private void addServerToLayout(String url) {
        View view = getLayoutInflater().inflate(R.layout.item_server_url, null);
        TextView urlText = view.findViewById(R.id.serverUrlTextView);
        ImageButton deleteBtn = view.findViewById(R.id.deleteServerButton);
        urlText.setText(url);
        deleteBtn.setOnClickListener(v -> {
            serverContainer.removeView(view);
            serverUrls.remove(url);
        });
        serverContainer.addView(view);
    }

    private void saveChannel() {
        String name = nameEditText.getText().toString().trim();
        String logoUrl = logoUrlEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(logoUrl)) {
            Toast.makeText(this, "Name and Logo URL required", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCategory = (Category) categorySpinner.getSelectedItem();
        Country selectedCountry = (Country) countrySpinner.getSelectedItem();

        if (selectedCategory == null || selectedCountry == null) {
            Toast.makeText(this, "Please select a valid Category and Country", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryDAO.getById(selectedCategory.getId()) == null || countryDAO.getById(selectedCountry.getId()) == null) {
            Toast.makeText(this, "Selected Category or Country does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverUrls.isEmpty()) {
            Toast.makeText(this, "Please add at least one server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        int categoryId = selectedCategory.getId();
        int countryId = selectedCountry.getId();

        List<ChannelServer> servers = new ArrayList<>();
        int count = 1;
        for (String serverUrl : serverUrls) {
            servers.add(new ChannelServer(0, "Server " + count++, serverUrl));
        }

        if (editingChannelId != -1) {
            Channel updatedChannel = new Channel(name, logoUrl, countryId, categoryId, servers);
            updatedChannel.setId(editingChannelId);
            if (channelDAO.update(updatedChannel) == 0) {
                Toast.makeText(this, "Update failed due to constraints", Toast.LENGTH_SHORT).show();
                return;
            }
            serverDAO.deleteChannelServersByChannelID(editingChannelId);
            for (ChannelServer server : servers) {
                server.setChannelId(editingChannelId);
                serverDAO.insert(server);
            }
            Toast.makeText(this, "Channel updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Channel newChannel = new Channel(name, logoUrl, countryId, categoryId, servers);
            long channelId = channelDAO.insert(newChannel);
            if (channelId != -1) {
                for (ChannelServer server : servers) {
                    server.setChannelId((int) channelId);
                    serverDAO.insert(server);
                }
                Toast.makeText(this, "Channel added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add channel", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        finish();
    }
}
