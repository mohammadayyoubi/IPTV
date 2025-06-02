package com.example.iptv.activities.user;

import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.iptv.R;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.database.FavoriteDAO;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class ChannelDetailActivity extends AppCompatActivity {

    private ImageView imageChannelLogo;
    private TextView textChannelName, textCategoryName, textCountryName;
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private Channel channel;
    private DBHelper dbHelper;
    private LinearLayout rootLayout;
    private FavoriteDAO favoriteDAO;
    private boolean isFavorite;
    private Spinner serverSpinner;
    private List<ChannelServer> servers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_channel_detail);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        imageChannelLogo = findViewById(R.id.image_channel_logo);
        textChannelName = findViewById(R.id.text_channel_name);
        textCategoryName = findViewById(R.id.text_category_name);
        textCountryName = findViewById(R.id.text_country_name);
        playerView = findViewById(R.id.player_view);
        rootLayout = findViewById(R.id.root_layout);
        serverSpinner = findViewById(R.id.server_spinner);

        channel = (Channel) getIntent().getSerializableExtra("channel");
        if (channel == null) {
            Toast.makeText(this, "Channel data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textChannelName.setText(channel.getName());
        Glide.with(this).load(channel.getLogoUrl()).placeholder(R.drawable.placeholder).into(imageChannelLogo);

        dbHelper = new DBHelper(this);
        String categoryName = "Unknown";
        String countryName = "Unknown";
        if (channel.getCategoryId() != 0) {
            categoryName = new CategoryDAO(dbHelper.getWritableDatabase()).getById(channel.getCategoryId()).getName();
        }
        if (channel.getCountryId() != 0) {
            countryName = new CountryDAO(dbHelper.getWritableDatabase()).getById(channel.getCountryId()).getName();
        }
        textCategoryName.setText("Category: " + categoryName);
        textCountryName.setText("Country: " + countryName);

        favoriteDAO = new FavoriteDAO(dbHelper.getWritableDatabase());
        Button favoriteButton = findViewById(R.id.button_favorite);
        isFavorite = favoriteDAO.isFavorite(channel.getId());
        int channelId = channel.getId();
        favoriteButton.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
        favoriteButton.setOnClickListener(v -> {
            if (isFavorite) {
                favoriteDAO.removeFromFavorites(channelId);
                favoriteButton.setText("Add to Favorites");
            } else {
                favoriteDAO.addToFavorites(channelId);
                favoriteButton.setText("Remove from Favorites");
            }
            isFavorite = !isFavorite;
        });
/////////server spinner/////////
        servers = channel.getServers();
        if (servers != null && !servers.isEmpty()) {
            List<String> serverNames = new ArrayList<>();
            serverNames.add("Select a Server"); // Hint/placeholder
            for (ChannelServer server : servers) {
                serverNames.add(server.getServerName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serverNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            serverSpinner.setAdapter(adapter);

            serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) return; // Ignore hint
                    String selectedUrl = servers.get(position - 1).getStreamUrl(); // Adjust index
                    if (selectedUrl != null && !selectedUrl.isEmpty()) {
                        initializePlayer(selectedUrl);
                    } else {
                        Toast.makeText(ChannelDetailActivity.this, "No stream URL for this server", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        } else {
            String s = "No servers available";
            //create adapter for no server spinner
            ArrayAdapter<String> noServerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{s});
            //set layout for no server spinner
            noServerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            serverSpinner.setAdapter(noServerAdapter);
            Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String streamUrl) {
        if (player != null) player.release();
        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this))
                .build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) enterFullScreen();
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) exitFullScreen();
    }

    private void enterFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
//        hide the elements
        imageChannelLogo.setVisibility(View.GONE);
        textChannelName.setVisibility(View.GONE);
        textCategoryName.setVisibility(View.GONE);
        textCountryName.setVisibility(View.GONE);
        serverSpinner.setVisibility(View.GONE);

        rootLayout.setBackgroundColor(Color.BLACK);
        rootLayout.setPadding(0, 0, 0, 0);
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(params);
        playerView.setBackgroundColor(Color.BLACK);
    }

    private void exitFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().show(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
//        re-set visible
        imageChannelLogo.setVisibility(View.VISIBLE);
        textChannelName.setVisibility(View.VISIBLE);
        textCategoryName.setVisibility(View.VISIBLE);
        textCountryName.setVisibility(View.VISIBLE);
        serverSpinner.setVisibility(View.VISIBLE);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        rootLayout.setPadding(padding, padding, padding, padding);
        rootLayout.setBackgroundColor(Color.parseColor("#1A1B2E"));
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = (int) (200 * getResources().getDisplayMetrics().density);
        playerView.setLayoutParams(params);
        playerView.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
