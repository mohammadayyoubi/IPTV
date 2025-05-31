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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.iptv.R;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar and set fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_channel_detail);

        // Hide action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        imageChannelLogo = findViewById(R.id.image_channel_logo);
        textChannelName = findViewById(R.id.text_channel_name);
        textCategoryName = findViewById(R.id.text_category_name);
        textCountryName = findViewById(R.id.text_country_name);
        playerView = findViewById(R.id.player_view);
        rootLayout = findViewById(R.id.root_layout);

        channel = (Channel) getIntent().getSerializableExtra("channel");
        if (channel == null) {
            Toast.makeText(this, "Channel data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textChannelName.setText(channel.getName());
        Glide.with(this)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.placeholder)
                .into(imageChannelLogo);

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

        if (channel.getServers() != null && !channel.getServers().isEmpty()) {
            String streamUrl = channel.getServers().get(0).getStreamUrl();
            if (streamUrl != null && !streamUrl.isEmpty()) {
                initializePlayer(streamUrl);
            } else {
                Toast.makeText(this, "No stream URL found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String streamUrl) {
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
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            exitFullScreen();
        }
    }

    private void enterFullScreen() {
        // Hide system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        // Hide all UI elements
        imageChannelLogo.setVisibility(View.GONE);
        textChannelName.setVisibility(View.GONE);
        textCategoryName.setVisibility(View.GONE);
        textCountryName.setVisibility(View.GONE);

        // Adjust root layout for fullscreen
        rootLayout.setBackgroundColor(Color.BLACK);
        rootLayout.setPadding(0, 0, 0, 0);

        // Make PlayerView fullscreen
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(params);
        playerView.setBackgroundColor(Color.BLACK);
    }

    private void exitFullScreen() {
        // Show system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().show(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        // Show all UI elements
        imageChannelLogo.setVisibility(View.VISIBLE);
        textChannelName.setVisibility(View.VISIBLE);
        textCategoryName.setVisibility(View.VISIBLE);
        textCountryName.setVisibility(View.VISIBLE);

        // Restore root layout
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        rootLayout.setPadding(padding, padding, padding, padding);
        rootLayout.setBackgroundColor(Color.parseColor("#1A1B2E"));

        // Restore PlayerView size
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