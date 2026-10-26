package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.example.iptv.OOP.*;
import com.example.iptv.R;
import com.example.iptv.database.*;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.SearchableAdapter;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class activity_add_channel extends BaseActivity {
    private EditText nameEditText, logoUrlEditText, serverInput;
    private TextInputLayout nameField, logoUrlField, categoryField, countryField, serverUrlField;
    private ImageView logoPreview;
    private MaterialAutoCompleteTextView categoryDropdown, countryDropdown;
    private LinearLayout serverContainer;
    private TextView serverEmptyText;
    private View addServerButton, saveButton;
    private ChannelDAO channelDAO;
    private ChannelServerDAO serverDAO;
    private CategoryDAO categoryDAO;
    private CountryDAO countryDAO;
    private List<Category> categories;
    private List<Country> countries;
    private List<String> serverUrls = new ArrayList<>();
    private SearchableAdapter<Category> categoryAdapter;
    private SearchableAdapter<Country> countryAdapter;
    private Category selectedCategory;
    private Country selectedCountry;
    public static final String EXTRA_CHANNEL_ID = "channel_id";
    private int editingChannelId = -1;
    private Channel editingChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);
        UiUtils.applyInsets(findViewById(R.id.root), true, false);
        UiUtils.applyInsets(findViewById(R.id.bottomBar), false, true, true);

        DBHelper dbHelper = DBHelper.getInstance(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        serverDAO = new ChannelServerDAO(dbHelper.getWritableDatabase());
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        countryDAO = new CountryDAO(dbHelper.getWritableDatabase());

        nameEditText = findViewById(R.id.channelNameEditText);
        logoUrlEditText = findViewById(R.id.logoUrlEditText);
        nameField = findViewById(R.id.channelNameField);
        logoUrlField = findViewById(R.id.logoUrlField);
        categoryField = findViewById(R.id.categoryField);
        countryField = findViewById(R.id.countryField);
        serverUrlField = findViewById(R.id.serverUrlField);
        logoPreview = findViewById(R.id.logoPreviewImageView);
        categoryDropdown = findViewById(R.id.categoryDropdown);
        countryDropdown = findViewById(R.id.countryDropdown);
        serverInput = findViewById(R.id.serverUrlEditText);
        serverContainer = findViewById(R.id.serverListLayout);
        serverEmptyText = findViewById(R.id.serverEmptyText);
        addServerButton = findViewById(R.id.addServerButton);
        saveButton = findViewById(R.id.saveChannelButton);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        loadCategories();
        loadCountries();

        // Preview the logo when leaving the field or tapping the refresh icon
        logoUrlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) previewLogo();
        });
        logoUrlField.setEndIconOnClickListener(v -> previewLogo());

        addServerButton.setOnClickListener(v -> addServerFromInput());
        serverInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addServerFromInput();
                return true;
            }
            return false;
        });

        saveButton.setOnClickListener(v -> saveChannel());

        if (getIntent().hasExtra(EXTRA_CHANNEL_ID)) {
            editingChannelId = getIntent().getIntExtra(EXTRA_CHANNEL_ID, -1);
            if (editingChannelId != -1) {
                ((TextView) findViewById(R.id.AddChannelPageLable)).setText(R.string.edit_channel);
                loadChannelData(editingChannelId);
            }
        }
        updateServerEmptyState();
    }

    private void previewLogo() {
        String logoUrl = logoUrlEditText.getText().toString().trim();
        if (!logoUrl.isEmpty()) {
            Glide.with(this).load(logoUrl)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(logoPreview);
        }
    }

    private void loadCategories() {
        categories = categoryDAO.getAll();
        categoryAdapter = new SearchableAdapter<>(this, categories);
        categoryDropdown.setAdapter(categoryAdapter);
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categoryAdapter.getItem(position);
            categoryField.setError(null);
        });
        setupDropdownTouch(categoryDropdown);
    }

    private void loadCountries() {
        countries = countryDAO.getAll();
        countryAdapter = new SearchableAdapter<>(this, countries);
        countryDropdown.setAdapter(countryAdapter);
        countryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedCountry = countryAdapter.getItem(position);
            countryField.setError(null);
        });
        setupDropdownTouch(countryDropdown);
    }

    /** Opens the full list on focus / tap, and lets the user type to narrow it down. */
    private void setupDropdownTouch(MaterialAutoCompleteTextView dropdown) {
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dropdown.showDropDown();
        });
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
    }

    private void loadChannelData(int channelId) {
        Channel channel = channelDAO.getById(channelId);
        if (channel == null) return;
        editingChannel = channel;
        nameEditText.setText(channel.getName());
        logoUrlEditText.setText(channel.getLogoUrl());
        for (Category category : categories) {
            if (category.getId() == channel.getCategoryId()) {
                selectedCategory = category;
                categoryDropdown.setText(category.getName(), false);
            }
        }
        for (Country country : countries) {
            if (country.getId() == channel.getCountryId()) {
                selectedCountry = country;
                countryDropdown.setText(country.getName(), false);
            }
        }
        serverUrls.clear();
        for (ChannelServer server : channel.getServers()) {
            serverUrls.add(server.getStreamUrl());
            addServerToLayout(server.getStreamUrl());
        }
        previewLogo();
    }

    private void addServerFromInput() {
        String serverUrl = serverInput.getText().toString().trim();
        if (serverUrl.isEmpty()) {
            serverUrlField.setError(getString(R.string.error_required));
            return;
        }
        serverUrlField.setError(null);
        addServerToLayout(serverUrl);
        serverUrls.add(serverUrl);
        serverInput.setText("");
        updateServerEmptyState();
    }

    private void addServerToLayout(String url) {
        View view = getLayoutInflater().inflate(R.layout.item_server_url, serverContainer, false);
        TextView urlText = view.findViewById(R.id.serverUrlTextView);
        View deleteBtn = view.findViewById(R.id.deleteServerButton);
        urlText.setText(url);
        deleteBtn.setOnClickListener(v -> {
            serverContainer.removeView(view);
            serverUrls.remove(url);
            renumberServers();
            updateServerEmptyState();
        });
        serverContainer.addView(view);
        renumberServers();
    }

    /** Labels rows "Server 1", "Server 2", ... matching the names they are saved with. */
    private void renumberServers() {
        for (int i = 0; i < serverContainer.getChildCount(); i++) {
            TextView label = serverContainer.getChildAt(i).findViewById(R.id.serverLabelTextView);
            label.setText(getString(R.string.server_n, i + 1));
        }
    }

    private void updateServerEmptyState() {
        serverEmptyText.setVisibility(serverUrls.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveChannel() {
        String name = nameEditText.getText().toString().trim();
        String logoUrl = logoUrlEditText.getText().toString().trim();

        nameField.setError(TextUtils.isEmpty(name) ? getString(R.string.error_required) : null);
        logoUrlField.setError(TextUtils.isEmpty(logoUrl) ? getString(R.string.error_required) : null);

        // Accept a name typed exactly instead of picked from the list
        if (selectedCategory == null || !selectedCategory.getName().equalsIgnoreCase(categoryDropdown.getText().toString().trim())) {
            selectedCategory = categoryAdapter.findByLabel(categoryDropdown.getText().toString());
        }
        if (selectedCountry == null || !selectedCountry.getName().equalsIgnoreCase(countryDropdown.getText().toString().trim())) {
            selectedCountry = countryAdapter.findByLabel(countryDropdown.getText().toString());
        }
        categoryField.setError(selectedCategory == null ? getString(R.string.error_pick_from_list) : null);
        countryField.setError(selectedCountry == null ? getString(R.string.error_pick_from_list) : null);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(logoUrl)
                || selectedCategory == null || selectedCountry == null) {
            return;
        }

        if (categoryDAO.getById(selectedCategory.getId()) == null || countryDAO.getById(selectedCountry.getId()) == null) {
            Toast.makeText(this, R.string.adm_category_country_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverUrls.isEmpty()) {
            serverUrlField.setError(getString(R.string.adm_add_server_required));
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
            // A logo edited by hand is never replaced by later imports / logo refreshes
            updatedChannel.setLogoAuto(editingChannel != null && editingChannel.isLogoAuto()
                    && logoUrl.equals(editingChannel.getLogoUrl()));
            if (channelDAO.update(updatedChannel) == 0) {
                Toast.makeText(this, R.string.adm_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            serverDAO.deleteChannelServersByChannelID(editingChannelId);
            for (ChannelServer server : servers) {
                server.setChannelId(editingChannelId);
                serverDAO.insert(server);
            }
            Toast.makeText(this, R.string.adm_channel_updated, Toast.LENGTH_SHORT).show();
        } else {
            Channel newChannel = new Channel(name, logoUrl, countryId, categoryId, servers);
            long channelId = channelDAO.insert(newChannel);
            if (channelId != -1) {
                for (ChannelServer server : servers) {
                    server.setChannelId((int) channelId);
                    serverDAO.insert(server);
                }
                Toast.makeText(this, R.string.adm_channel_added, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.adm_channel_add_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        finish();
    }
}
