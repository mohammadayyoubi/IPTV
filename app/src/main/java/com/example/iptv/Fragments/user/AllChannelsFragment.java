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
import com.example.iptv.database.FavoriteDAO;

import java.util.ArrayList;
import java.util.List;

public class AllChannelsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChannelUserAdapter adapter;
    private List<Channel> channelList;
    private List<Channel> filteredList;
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

        // Initialize views correctly from the correct layout
        recyclerView = view.findViewById(R.id.recycler_all_channels);
        searchEditText = view.findViewById(R.id.AllChannelsSearchEditText);

        if (pageTitle != null) {
            pageTitle.setText("All Channels List");
        } else {
            // Fallback or log an error if TextView not found
            System.err.println("TextView AllChannelsListUserPageLabel not found in layout!");
        }

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        // Initialize DB and DAO
        dbHelper = new DBHelper(requireContext());
        ChannelDAO channelDAO = new ChannelDAO(dbHelper.getWritableDatabase());
        channelList = channelDAO.getAll(); // Fetch channels from DB
        filteredList = new ArrayList<>(channelList);

        // Initialize adapter after fetching data
        adapter = new ChannelUserAdapter(requireContext(), filteredList);
        recyclerView.setAdapter(adapter);

        // Add TextWatcher to EditText for filtering
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
            // Navigate to admin login activity
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void filterChannels(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(channelList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Channel channel : channelList) {
                if (channel.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(channel);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
