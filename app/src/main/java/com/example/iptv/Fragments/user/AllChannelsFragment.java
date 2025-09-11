package com.example.iptv.Fragments.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;
import com.example.iptv.activities.admin.LoginActivity;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllChannelsFragment extends Fragment {

    private static final int PAGE_SIZE = 40;
    
    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> channelList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView pageTitle;
    
    // Pagination variables
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentSearchQuery = "";
    private boolean isSearchMode = false;
    
    // Background thread handling
    private ExecutorService executor;
    private Handler mainHandler;

    public AllChannelsFragment() {
        // Required empty public constructor
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_channels, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_all_channels);
        searchEditText = view.findViewById(R.id.AllChannelsSearchEditText);

        if (pageTitle != null) {
            pageTitle.setText("All Channels List");
        } else {
            Log.e("AllChannelsFragment", "TextView AllChannelsListUserPageLabel not found in layout!");
        }

        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Loading item spans all columns
                return adapter.getItemViewType(position) == 2 ? 3 : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        dbHelper = new DBHelper(requireContext());
        channelList = new ArrayList<>();
        adapter = new ChannelUserAdapter(requireContext(), channelList);
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

        // Search functionality with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            private Handler searchHandler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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

            @Override
            public void afterTextChanged(Editable s) { }
        });

        ImageButton adminLoginButton = view.findViewById(R.id.btn_admin_login);
        adminLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        // Load initial data
        loadChannels();

        return view;
    }

    private void resetPagination() {
        currentPage = 0;
        isLastPage = false;
        isLoading = false;
        channelList.clear();
        adapter.updateList(channelList);
    }
    
    private void loadChannels() {
        if (isLoading) return;
        
        isLoading = true;
        adapter.setLoading(true);
        
        executor.execute(() -> {
            try {
                ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
                List<Channel> newChannels = channelDAO.getChannelsPaginated(PAGE_SIZE, currentPage * PAGE_SIZE);
                
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                    
                    if (newChannels.isEmpty()) {
                        isLastPage = true;
                    } else {
                        if (currentPage == 0) {
                            channelList.clear();
                            channelList.addAll(newChannels);
                            adapter.updateList(channelList);
                        } else {
                            adapter.addChannels(newChannels);
                        }
                        currentPage++;
                    }
                });
            } catch (Exception e) {
                Log.e("AllChannelsFragment", "Error loading channels", e);
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
                ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
                List<Channel> searchResults = channelDAO.searchChannelsPaginated(query, PAGE_SIZE, currentPage * PAGE_SIZE);
                
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                    
                    if (searchResults.isEmpty()) {
                        isLastPage = true;
                    } else {
                        if (currentPage == 0) {
                            channelList.clear();
                            channelList.addAll(searchResults);
                            adapter.updateList(channelList);
                        } else {
                            adapter.addChannels(searchResults);
                        }
                        currentPage++;
                    }
                });
            } catch (Exception e) {
                Log.e("AllChannelsFragment", "Error searching channels", e);
                mainHandler.post(() -> {
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChannels();
    }

    public void refreshChannels() {
        resetPagination();
        currentSearchQuery = "";
        isSearchMode = false;
        searchEditText.setText("");
        loadChannels();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
