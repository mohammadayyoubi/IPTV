package com.example.iptv.Fragments.user;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iptv.R;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.FavoriteDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.ChannelUserAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesFragment extends Fragment {

    private static final int PAGE_SIZE = 40;
    
    private RecyclerView recyclerView;
    public static ChannelUserAdapter adapter;
    private List<Channel> favoriteChannels;
    private List<Integer> allFavoriteIds;
    private DBHelper dbHelper;
    
    // Pagination variables
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    
    // Background thread handling
    private ExecutorService executor;
    private Handler mainHandler;

    public FavoritesFragment() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static void updateFavorites() {
        if (adapter != null) {
            // This static method can be called to trigger a refresh
            // The actual refresh will be handled by the instance methods
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recycler_favorites);
        
        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Loading item spans all columns
                return adapter != null && adapter.getItemViewType(position) == 2 ? 3 : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);

        dbHelper = new DBHelper(requireContext());
        favoriteChannels = new ArrayList<>();
        allFavoriteIds = new ArrayList<>();
        
        adapter = new ChannelUserAdapter(requireContext(), favoriteChannels);
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
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        loadMoreFavorites();
                    }
                }
            }
        });

        // Load initial favorites
        loadFavorites();

        return view;
    }

    private void resetPagination() {
        currentPage = 0;
        isLastPage = false;
        isLoading = false;
        favoriteChannels.clear();
        if (adapter != null) {
            adapter.updateList(favoriteChannels);
        }
    }

    private void loadFavorites() {
        if (isLoading) return;
        
        isLoading = true;
        if (adapter != null) {
            adapter.setLoading(true);
        }
        
        executor.execute(() -> {
            try {
                FavoriteDAO favoriteDAO = new FavoriteDAO(dbHelper.getWritableDatabase());
                
                if (currentPage == 0) {
                    // Load all favorite IDs on first page
                    allFavoriteIds = favoriteDAO.getAllFavoriteChannelIds();
                    if (allFavoriteIds.isEmpty()) {
                        mainHandler.post(() -> {
                            isLoading = false;
                            isLastPage = true;
                            if (adapter != null) {
                                adapter.setLoading(false);
                                favoriteChannels.clear();
                                adapter.updateList(favoriteChannels);
                            }
                        });
                        return;
                    }
                }
                
                ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
                List<Channel> newFavorites = channelDAO.getFavoriteChannelsPaginated(
                    allFavoriteIds, PAGE_SIZE, currentPage * PAGE_SIZE
                );
                
                mainHandler.post(() -> {
                    isLoading = false;
                    if (adapter != null) {
                        adapter.setLoading(false);
                    }
                    
                    if (newFavorites.isEmpty()) {
                        isLastPage = true;
                    } else {
                        if (currentPage == 0) {
                            favoriteChannels.clear();
                            favoriteChannels.addAll(newFavorites);
                            if (adapter != null) {
                                adapter.updateList(favoriteChannels);
                            }
                        } else {
                            if (adapter != null) {
                                adapter.addChannels(newFavorites);
                            }
                        }
                        currentPage++;
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    isLoading = false;
                    if (adapter != null) {
                        adapter.setLoading(false);
                    }
                });
            }
        });
    }
    
    private void loadMoreFavorites() {
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChannels();
    }

    private void refreshChannels() {
        resetPagination();
        loadFavorites();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
