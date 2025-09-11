package com.example.iptv.activities.user;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.Fragments.user.SpacesItemDecoration;
import com.example.iptv.OOP.Channel;
import com.example.iptv.R;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class activity_user_filtered_channels extends AppCompatActivity {

    private static final int PAGE_SIZE = 40;

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> filteredChannels;
    private EditText searchEditText;
    private TextView pageTitle;
    private DBHelper dbHelper;
    private ChannelDAO channelDAO;

    private String filterType;  // "country" or "category"
    private int filterId;       // countryId or categoryId
    
    // Pagination variables
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentSearchQuery = "";
    private boolean isSearchMode = false;
    
    // Background thread handling
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_filtered_channels);

        recyclerView = findViewById(R.id.recycler_filtered_channels);
        searchEditText = findViewById(R.id.FilteredChannelsSearchEditText);
        pageTitle = findViewById(R.id.filteredChannelsPageLable);

        // Get filter data
        filterType = getIntent().getStringExtra("filterType");
        filterId = getIntent().getIntExtra("filterId", -1);
        String filterName = getIntent().getStringExtra("filterName");

        pageTitle.setText(filterType.equals("country") ? "Channels in Country" : "Channels in Category");

        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Loading item spans all columns
                return adapter.getItemViewType(position) == 2 ? 3 : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        dbHelper = new DBHelper(this);
        channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize adapter and RecyclerView
        filteredChannels = new ArrayList<>();
        adapter = new ChannelUserAdapter(this, filteredChannels);
        recyclerView.setAdapter(adapter);
        
        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreChannels();
                    }
                }
            }
        });

        // Setup page title with category/country name
        setupPageTitle();

        // Search functionality with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            private Handler searchHandler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;
            
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (!query.equals(currentSearchQuery)) {
                        currentSearchQuery = query;
                        resetPagination();
                        if (query.isEmpty()) {
                            isSearchMode = false;
                            loadChannels();
                        } else {
                            isSearchMode = true;
                            searchChannels(query);
                        }
                    }
                };
                
                // Debounce search for 300ms
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        
        // Load initial data
        loadChannels();
    }
    
    private void setupPageTitle() {
        executor.execute(() -> {
            try {
                String titleText;
                if ("country".equals(filterType)) {
                    CountryDAO countryDAO = new CountryDAO(dbHelper.getWritableDatabase());
                    String countryName = countryDAO.getById(filterId).getName();
                    titleText = countryName + "'s Channels";
                } else if ("category".equals(filterType)) {
                    CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
                    String categoryName = categoryDAO.getById(filterId).getName();
                    titleText = categoryName + "'s Channels";
                } else {
                    titleText = "Filtered Channels";
                }
                
                mainHandler.post(() -> pageTitle.setText(titleText));
            } catch (Exception e) {
                mainHandler.post(() -> pageTitle.setText("Filtered Channels"));
            }
        });
    }

    private void resetPagination() {
        currentPage = 0;
        isLastPage = false;
        isLoading = false;
        filteredChannels.clear();
        adapter.updateList(filteredChannels);
    }
    
    private void loadChannels() {
        if (isLoading) return;
        
        isLoading = true;
        adapter.setLoading(true);
        
        executor.execute(() -> {
            try {
                List<Channel> newChannels;
                if ("country".equals(filterType)) {
                    newChannels = channelDAO.getAllByCountryIdPaginated(filterId, PAGE_SIZE, currentPage * PAGE_SIZE);
                } else if ("category".equals(filterType)) {
                    newChannels = channelDAO.getAllByCategoryIdPaginated(filterId, PAGE_SIZE, currentPage * PAGE_SIZE);
                } else {
                    newChannels = new ArrayList<>();
                }
                
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                    
                    if (newChannels.isEmpty()) {
                        isLastPage = true;
                    } else {
                        if (currentPage == 0) {
                            filteredChannels.clear();
                            filteredChannels.addAll(newChannels);
                            adapter.updateList(filteredChannels);
                        } else {
                            adapter.addChannels(newChannels);
                        }
                        currentPage++;
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        });
    }
    
    private void loadMoreChannels() {
        if (isSearchMode) {
            searchChannels(currentSearchQuery);
        } else {
            loadChannels();
        }
    }
    
    private void searchChannels(String query) {
        if (isLoading) return;
        
        isLoading = true;
        adapter.setLoading(true);
        
        executor.execute(() -> {
            try {
                List<Channel> searchResults;
                if ("country".equals(filterType)) {
                    searchResults = channelDAO.searchChannelsByCountryPaginated(filterId, query, PAGE_SIZE, currentPage * PAGE_SIZE);
                } else if ("category".equals(filterType)) {
                    searchResults = channelDAO.searchChannelsByCategoryPaginated(filterId, query, PAGE_SIZE, currentPage * PAGE_SIZE);
                } else {
                    searchResults = new ArrayList<>();
                }
                
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                    
                    if (searchResults.isEmpty()) {
                        isLastPage = true;
                    } else {
                        if (currentPage == 0) {
                            filteredChannels.clear();
                            filteredChannels.addAll(searchResults);
                            adapter.updateList(filteredChannels);
                        } else {
                            adapter.addChannels(searchResults);
                        }
                        currentPage++;
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
