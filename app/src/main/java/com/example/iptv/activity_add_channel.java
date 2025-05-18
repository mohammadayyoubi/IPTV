package com.example.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

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

    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> countryAdapter;

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

        // Check if this is an edit operation
        if (getIntent().hasExtra(EXTRA_CHANNEL_ID)) {
            editingChannelId = getIntent().getIntExtra(EXTRA_CHANNEL_ID, -1);

            if (editingChannelId != -1) {
                //you are in edit mode
                Toast toast = Toast.makeText(this, "Edit Mode", Toast.LENGTH_SHORT);
                toast.show();
                TextView t = findViewById(R.id.AddChannelPageLable);
                t.setText("Edit Channel");
                saveButton.setText("Update");
                loadChannelData(editingChannelId);
            }
        }else{
            //you are in add mode
            Toast toast = Toast.makeText(this, "Add Mode", Toast.LENGTH_SHORT);
            toast.show();
            TextView t = findViewById(R.id.AddChannelPageLable);
            t.setText("Add Channel");
            saveButton.setText("Save");
        }
    }

    private void loadCategories() {
        categories = categoryDAO.getAll();
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) categoryNames.add(c.getName());
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void loadCountries() {
        countries = countryDAO.getAll();
        List<String> countryNames = new ArrayList<>();
        for (Country c : countries) countryNames.add(c.getName());
        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countryNames);
        countrySpinner.setAdapter(countryAdapter);
    }

    private void loadChannelData(int channelId) {
        Channel channel = channelDAO.getById(channelId);
        if (channel == null) return;

        nameEditText.setText(channel.getName());
        logoUrlEditText.setText(channel.getLogoUrl());

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == channel.getCategoryId()) {
                categorySpinner.setSelection(i);
                break;
            }
        }

        for (int i = 0; i < countries.size(); i++) {
            if (countries.get(i).getId() == channel.getCountryId()) {
                countrySpinner.setSelection(i);
                break;
            }
        }

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

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(logoUrl)) {
            logoUrlEditText.setError("Required");
            return;
        }

        int categoryIndex = categorySpinner.getSelectedItemPosition();
        int countryIndex = countrySpinner.getSelectedItemPosition();

        if (categoryIndex < 0 || countryIndex < 0) {
            Toast.makeText(this, "Please select category and country", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverUrls.isEmpty()) {
            Toast.makeText(this, "Please add at least one server", Toast.LENGTH_SHORT).show();
            return;
        }

        int categoryId = categories.get(categoryIndex).getId();
        int countryId = countries.get(countryIndex).getId();

        List<ChannelServer> servers = new ArrayList<>();
        int count = 1;
        for (String serverUrl : serverUrls) {
            servers.add(new ChannelServer(0, "Server " + count++, serverUrl));
        }

        if (editingChannelId != -1) {
            // EDIT
            Channel updatedChannel = new Channel(name, logoUrl, categoryId, countryId, servers);
            updatedChannel.setId(editingChannelId); // Set ID manually
            channelDAO.update(updatedChannel);

            serverDAO.deleteChannelServersByChannelID(editingChannelId);
            for (ChannelServer server : servers) {
                server.setChannelId(editingChannelId);
                serverDAO.insert(server);
            }

            Toast.makeText(this, "Channel updated", Toast.LENGTH_SHORT).show();
        } else {
            // ADD
            Channel channel = new Channel(name, logoUrl, categoryId, countryId, servers);
            long channelId = channelDAO.insert(channel);
            if (channelId != -1) {
                for (ChannelServer server : servers) {
                    server.setChannelId((int) channelId);
                    serverDAO.insert(server);
                }
                Toast.makeText(this, "Channel added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add channel", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        finish();
    }
}
