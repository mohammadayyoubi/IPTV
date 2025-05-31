package com.example.iptv.activities.user;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.iptv.R;
import com.example.iptv.OOP.Channel;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

public class ChannelDetailActivity extends AppCompatActivity {

    private ImageView imageChannelLogo;
    private TextView textChannelName, textCategoryName, textCountryName;
    private PlayerView playerView;
    private ExoPlayer player;
    private Channel channel;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);

        // Initialize views
        imageChannelLogo = findViewById(R.id.image_channel_logo);
        textChannelName = findViewById(R.id.text_channel_name);
        textCategoryName = findViewById(R.id.text_category_name);
        textCountryName = findViewById(R.id.text_country_name);
        playerView = findViewById(R.id.player_view);

        // Retrieve channel
        channel = (Channel) getIntent().getSerializableExtra("channel");
        if (channel == null) {
            Toast.makeText(this, "Channel data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (channel != null) {
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
            } else {
                Toast.makeText(this, "No servers available for this channel", Toast.LENGTH_SHORT).show();
            }

            // Setup ExoPlayer for first server

        }
    }

    private void initializePlayer(String streamUrl) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
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
