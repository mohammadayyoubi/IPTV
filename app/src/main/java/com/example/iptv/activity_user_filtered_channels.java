package com.example.iptv;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.Fragments.user.SpacesItemDecoration;
import com.example.iptv.OOP.Channel;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class activity_user_filtered_channels extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> filteredChannels;
    private EditText searchEditText;
    private TextView pageTitle;
    private DBHelper dbHelper;
    private ChannelDAO channelDAO;

    private String filterType;  // "country" or "category"
    private int filterId;       // countryId or categoryId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_filtered_channels);

        recyclerView = findViewById(R.id.recycler_filtered_channels);
        searchEditText = findViewById(R.id.FilteredChannelsSearchEditText);
        pageTitle = findViewById(R.id.filteredChannelsPageLable);

        // Get filter data
        filterType = getIntent().getStringExtra("filterType");
        filterId = getIntent().getIntExtra("filterId", -1);  // Assuming we pass id
        String filterName = getIntent().getStringExtra("filterName");

        pageTitle.setText(filterType.equals("country") ? "Channels in Country" : "Channels in Category");

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        dbHelper = new DBHelper(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());

        // Fetch filtered channels
        filteredChannels = new ArrayList<>();
        if ("country".equals(filterType)) {
            filteredChannels = channelDAO.getAllByCountryId(filterId);
            CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
            String countryName = countryDAO.getById(filterId).getName(); // Ensure getById() exists
            pageTitle.setText(countryName+"'s Channels");
        } else if ("category".equals(filterType)) {
            filteredChannels = channelDAO.getAllByCategoryId(filterId);
            CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
            String categoryName = categoryDAO.getById(filterId).getName();  // Ensure getById() exists
            pageTitle.setText(categoryName + "'s Channels");
        }

        adapter = new ChannelUserAdapter(this, filteredChannels);
        recyclerView.setAdapter(adapter);

        // Search filter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void filterSearch(String query) {
        List<Channel> tempFiltered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Channel channel : filteredChannels) {
            if (channel.getName().toLowerCase().contains(lowerQuery)) {
                tempFiltered.add(channel);
            }
        }
        adapter.updateList(tempFiltered); // Make sure your adapter supports this method
    }
}
