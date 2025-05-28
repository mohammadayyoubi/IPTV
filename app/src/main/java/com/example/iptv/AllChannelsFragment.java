package com.example.iptv;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Channel;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.DBHelper;
import com.google.android.material.navigation.NavigationView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class AllChannelsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChannelAdapter adapter;
    private ChannelDAO channelDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_channels, container, false);
        recyclerView = view.findViewById(R.id.recycler_all_channels);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        channelDAO = new ChannelDAO(new DBHelper(getContext()).getWritableDatabase());
        List<Channel> channelList = channelDAO.getAll();

       // adapter = new ChannelAdapter(getContext(), channelList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
