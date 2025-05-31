package com.example.iptv.Fragments.user;

import android.os.Bundle;
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

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> favoriteChannels;
    private DBHelper dbHelper;

    public FavoritesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recycler_favorites);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // 3-column grid

        dbHelper = new DBHelper(requireContext());
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        FavoriteDAO favoriteDAO = new FavoriteDAO(dbHelper.getWritableDatabase());

        // Get favorite channel IDs
        List<Integer> favoriteChannelIds = favoriteDAO.getAllFavoriteChannelIds(); // Ensure this method exists

        // Fetch channels by IDs
        favoriteChannels = new ArrayList<>();
        for (int channelId : favoriteChannelIds) {
            Channel channel = channelDAO.getById(channelId); // Ensure getChannelById() is implemented
            if (channel != null) {
                favoriteChannels.add(channel);
            }
        }

        // Initialize adapter
        adapter = new ChannelUserAdapter(requireContext(), favoriteChannels);

        recyclerView.setAdapter(adapter);

        return view;
    }
}
