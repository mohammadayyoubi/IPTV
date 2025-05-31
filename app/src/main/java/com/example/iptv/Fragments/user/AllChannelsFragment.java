package com.example.iptv.Fragments.user;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iptv.R;
import com.example.iptv.adapters.user.ChannelUserAdapter;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.DBHelper;

import java.util.List;

public class AllChannelsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> channelList;
    private DBHelper dbHelper;

    public AllChannelsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_channels, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_all_channels);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.addItemDecoration(new SpacesItemDecoration(12)); // 12dp spacing
        recyclerView.setAdapter(adapter);

        // Initialize database helper and DAO
        dbHelper = new DBHelper(requireContext());
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());

        // Fetch all channels
        channelList = channelDAO.getAll(); // Adjust if you have a custom method

        // Initialize adapter (updated version without OnChannelClickListener)
        adapter = new ChannelUserAdapter(requireContext(), channelList);


        recyclerView.setAdapter(adapter);

        return view;
    }
}
