package com.example.iptv;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);

        // Init DAOs
        DBHelper dbHelper = new DBHelper(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        serverDAO = new ChannelServerDAO(dbHelper.getWritableDatabase());
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        // Init Views
        nameEditText = findViewById(R.id.channelNameEditText);
        logoUrlEditText = findViewById(R.id.logoUrlEditText);
        logoPreview = findViewById(R.id.logoPreviewImageView);
        categorySpinner = findViewById(R.id.categorySpinner);
        countrySpinner = findViewById(R.id.countrySpinner);
        serverInput = findViewById(R.id.serverUrlEditText);
        serverContainer = findViewById(R.id.serverListLayout);
        addServerButton = findViewById(R.id.addServerButton);
        saveButton = findViewById(R.id.saveChannelButton);

        // Load dropdowns
        loadCategories();
        loadCountries();

        logoUrlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String logoUrl = logoUrlEditText.getText().toString().trim();
                if (!logoUrl.isEmpty()) {
                    Glide.with(this).load(logoUrl)
                            .placeholder(R.drawable.placeholder)
                            .into(logoPreview);
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
    }

    private void loadCategories() {
        categories = categoryDAO.getAll();
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) {
            categoryNames.add(c.getName());
        }
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void loadCountries() {
        countries = countryDAO.getAll();
        List<String> countryNames = new ArrayList<>();
        for (Country c : countries) {
            countryNames.add(c.getName());
        }
        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countryNames);
        countrySpinner.setAdapter(countryAdapter);
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

        // Prepare ChannelServer objects
        List<ChannelServer> servers = new ArrayList<>();
        int count = 1;
        for (String serverUrl : serverUrls) {
            servers.add(new ChannelServer( 0, "Server " + count++, serverUrl));
        }

        // Create and insert Channel
        Channel channel = new Channel(name, logoUrl, categoryId, countryId, servers);

        for(ChannelServer server:channel.getServers()){
            server.setChannelId(channel.getId());
        }

        long channelId = channelDAO.insert(channel);

        // Insert each server with the correct channelId
        if (channelId != -1) {
            for (ChannelServer server : servers) {
                server.setChannelId((int) channelId);
                serverDAO.insert(server);
            }
            Toast.makeText(this, "Channel added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add channel", Toast.LENGTH_SHORT).show();
        }
    }

}
