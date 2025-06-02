package com.example.iptv.Fragments.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

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

public class AllChannelsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> channelList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView pageTitle;

    public AllChannelsFragment() {
        // Required empty public constructor
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

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        dbHelper = new DBHelper(requireContext());
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        channelList = channelDAO.getAll();
        //print all chennelservers
        for (Channel channel : channelList) {
            Log.d("AllChannelsFragment", "Channel: " + channel.getName());
            for (int i = 0; i < channel.getServers().size(); i++) {
                Log.d("AllChannelsFragment", "Server: " + channel.getServers().get(i).getStreamUrl());
            }
        }

        adapter = new ChannelUserAdapter(requireContext(), new ArrayList<>(channelList));
        recyclerView.setAdapter(adapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChannels(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        ImageButton adminLoginButton = view.findViewById(R.id.btn_admin_login);
        adminLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void filterChannels(String query) {
        List<Channel> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Channel channel : channelList) {
            if (channel.getName().toLowerCase().contains(lowerQuery)) {
                filteredList.add(channel);
            }
        }
        adapter.updateList(filteredList);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChannels();
    }

    public void refreshChannels() {
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        channelList = channelDAO.getAll();
        adapter.updateList(new ArrayList<>(channelList));
    }
}
