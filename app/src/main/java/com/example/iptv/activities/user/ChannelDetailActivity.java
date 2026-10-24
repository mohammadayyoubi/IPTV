package com.example.iptv.activities.user;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.example.iptv.NetworkMonitor;
import com.example.iptv.R;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.ChannelServer;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.ChannelServerDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.database.FavoriteDAO;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class ChannelDetailActivity extends BaseActivity {

    public static final String EXTRA_CHANNEL_ID = "channel_id";

    private ImageView imageChannelLogo;
    private TextView textChannelName, textCategoryName, textCountryName, textServerCount;
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private Channel channel;
    private DBHelper dbHelper;
    private View rootLayout, playerContainer, detailsScroll, backButton, topScrim;
    private View playerMessage, playerRetry;
    private TextView playerMessageTitle, playerMessageBody;
    private ImageView playerMessageIcon;
    private ChipGroup serverChipGroup;
    private MaterialButton favoriteButton;
    private FavoriteDAO favoriteDAO;
    private boolean isFavorite;
    private List<ChannelServer> servers;
    private ChannelServer currentServer;
    // Video quality: 0 = auto, -1 = lowest (data saver), otherwise a max height like 720
    private static final String PREFS_PLAYER = "player";
    private static final String KEY_MAX_HEIGHT = "max_height";
    private final java.util.TreeSet<Integer> availableHeights = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
    private int playingHeight;
    private MaterialButton qualityButton, qualityDetailsButton;
    private ChannelServerDAO serverDAO;
    private OnBackPressedCallback exitFullScreenOnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);

        rootLayout = findViewById(R.id.root_layout);
        playerContainer = findViewById(R.id.player_container);
        detailsScroll = findViewById(R.id.details_scroll);
        imageChannelLogo = findViewById(R.id.image_channel_logo);
        textChannelName = findViewById(R.id.text_channel_name);
        textCategoryName = findViewById(R.id.text_category_name);
        textCountryName = findViewById(R.id.text_country_name);
        textServerCount = findViewById(R.id.text_server_count);
        playerView = findViewById(R.id.player_view);
        serverChipGroup = findViewById(R.id.server_chip_group);
        favoriteButton = findViewById(R.id.button_favorite);
        backButton = findViewById(R.id.button_back);
        topScrim = findViewById(R.id.player_top_scrim);
        playerMessage = findViewById(R.id.player_message);
        playerMessageIcon = findViewById(R.id.player_message_icon);
        playerMessageTitle = findViewById(R.id.player_message_title);
        playerMessageBody = findViewById(R.id.player_message_body);
        playerRetry = findViewById(R.id.player_retry);

        UiUtils.applyInsets(rootLayout, true, false);
        UiUtils.applyInsets(findViewById(R.id.details_content), false, true);

        int channelId = getIntent().getIntExtra(EXTRA_CHANNEL_ID, -1);
        channel = channelId > 0
                ? new ChannelDAO(new DBHelper(this).getWritableDatabase()).getById(channelId)
                : (Channel) getIntent().getSerializableExtra("channel");
        if (channel == null) {
            Toast.makeText(this, "Channel data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        qualityButton = findViewById(R.id.button_quality);
        qualityDetailsButton = findViewById(R.id.button_quality_details);
        qualityButton.setOnClickListener(v -> showQualityDialog());
        qualityDetailsButton.setOnClickListener(v -> showQualityDialog());
        // The quality button over the video comes and goes with the player controls
        playerView.setControllerVisibilityListener(
                (PlayerView.ControllerVisibilityListener) visibility -> qualityButton.setVisibility(visibility));
        renderQualityLabel();
        playerRetry.setOnClickListener(v -> {
            if (currentServer != null) initializePlayer(currentServer);
        });

        // Back while in fullscreen leaves fullscreen instead of closing the player
        exitFullScreenOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                exitFullScreen();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, exitFullScreenOnBack);

        playerView.setFullscreenButtonClickListener(enterFullscreen -> {
            if (enterFullscreen) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                enterFullScreen();
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                exitFullScreen();
            }
        });

        bindChannelInfo();
        setupFavoriteButton();
        setupServers();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        } else {
            exitFullScreen();
        }
    }

    private void bindChannelInfo() {
        textChannelName.setText(channel.getName());
        Glide.with(this).load(channel.getLogoUrl())
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(imageChannelLogo);

        dbHelper = new DBHelper(this);
        // The category / country may have been deleted since the list was loaded
        Category category = channel.getCategoryId() != 0
                ? new CategoryDAO(dbHelper.getWritableDatabase()).getById(channel.getCategoryId()) : null;
        Country country = channel.getCountryId() != 0
                ? new CountryDAO(dbHelper.getWritableDatabase()).getById(channel.getCountryId()) : null;
        textCategoryName.setText(category != null ? category.getName() : getString(R.string.unknown));
        textCountryName.setText(country != null ? country.getName() : getString(R.string.unknown));

        serverDAO = new ChannelServerDAO(dbHelper.getWritableDatabase());
        servers = channel.getServers();
        int serverCount = servers == null ? 0 : servers.size();
        textServerCount.setText(UiUtils.quantity(this, R.plurals.count_servers, serverCount));
    }

    private void setupFavoriteButton() {
        favoriteDAO = new FavoriteDAO(dbHelper.getWritableDatabase());
        int channelId = channel.getId();
        isFavorite = favoriteDAO.isFavorite(channelId);
        renderFavorite();
        favoriteButton.setOnClickListener(v -> {
            if (isFavorite) {
                favoriteDAO.removeFromFavorites(channelId);
            } else {
                favoriteDAO.addToFavorites(channelId);
            }
            isFavorite = !isFavorite;
            renderFavorite();
        });
    }

    private void renderFavorite() {
        favoriteButton.setText(isFavorite ? R.string.in_favorites : R.string.add_to_favorites);
        favoriteButton.setIconResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        favoriteButton.setIconTint(ContextCompat.getColorStateList(this,
                isFavorite ? R.color.accent : R.color.text_primary));
    }

    private void setupServers() {
        if (servers == null || servers.isEmpty()) {
            findViewById(R.id.text_no_servers).setVisibility(View.VISIBLE);
            showPlayerMessage(R.drawable.ic_dns, R.string.player_no_servers, 0, false);
            return;
        }

        for (int i = 0; i < servers.size(); i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_server_chip, serverChipGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(servers.get(i).getServerName());
            chip.setTag(i);
            renderChipStatus(chip, servers.get(i));
            serverChipGroup.addView(chip);
        }

        serverChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            playServer((int) chip.getTag());
        });

        // Start with the first server that isn't known to be broken
        int first = 0;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getStatus() != ChannelServer.STATUS_BROKEN) {
                first = i;
                break;
            }
        }
        ((Chip) serverChipGroup.getChildAt(first)).setChecked(true);
    }

    /** Broken servers get a warning icon so viewers can skip them. */
    private void renderChipStatus(Chip chip, ChannelServer server) {
        boolean broken = server.getStatus() == ChannelServer.STATUS_BROKEN;
        chip.setChipIconResource(broken ? R.drawable.ic_error : R.drawable.ic_dns);
        chip.setChipIconTint(ContextCompat.getColorStateList(this, broken ? R.color.danger : R.color.text_secondary));
    }

    /** Remembers whether a server played, so the channel filters stay accurate. */
    private void recordServerStatus(ChannelServer server, int status) {
        if (server.getStatus() == status) return;
        server.setStatus(status);
        server.setLastChecked(System.currentTimeMillis());
        if (server.getId() > 0) serverDAO.updateStatus(server.getId(), status, server.getLastChecked());
        for (int i = 0; i < serverChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) serverChipGroup.getChildAt(i);
            if (servers.get((int) chip.getTag()) == server) renderChipStatus(chip, server);
        }
    }

    private void playServer(int index) {
        ChannelServer server = servers.get(index);
        String selectedUrl = server.getStreamUrl();
        if (selectedUrl != null && !selectedUrl.isEmpty()) {
            // Played as-is: cleartext is allowed (network_security_config.xml), and rewriting
            // http:// to https:// broke every stream that is only served over HTTP.
            initializePlayer(server);
        } else {
            Toast.makeText(this, "No stream URL for this server", Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(ChannelServer server) {
        currentServer = server;
        releasePlayer();
        hidePlayerMessage();

        // Some streams refuse requests without the right User-Agent / Referer
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000);
        if (server.getUserAgent() != null && !server.getUserAgent().isEmpty()) {
            httpFactory.setUserAgent(server.getUserAgent());
        }
        if (server.getReferrer() != null && !server.getReferrer().isEmpty()) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", server.getReferrer());
            httpFactory.setDefaultRequestProperties(headers);
        }

        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this, httpFactory)))
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    hidePlayerMessage();
                    recordServerStatus(server, ChannelServer.STATUS_WORKING);
                }
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                // Qualities the stream offers (HLS variants)
                availableHeights.clear();
                for (Tracks.Group group : tracks.getGroups()) {
                    if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
                    for (int i = 0; i < group.length; i++) {
                        int h = group.getTrackFormat(i).height;
                        if (h > 0 && group.isTrackSupported(i)) availableHeights.add(h);
                    }
                }
            }

            @Override
            public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                playingHeight = videoSize.height;
                renderQualityLabel();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (!NetworkMonitor.hasValidatedNetwork(ChannelDetailActivity.this)) {
                    // Our connection, not the server: don't mark it broken
                    showPlayerMessage(R.drawable.ic_wifi_off, R.string.player_offline_title,
                            R.string.player_offline_body, true);
                    return;
                }
                recordServerStatus(server, ChannelServer.STATUS_BROKEN);
                showPlayerMessage(R.drawable.ic_error, R.string.player_error_title,
                        servers != null && servers.size() > 1 ? R.string.player_error_body : 0, true);
            }
        });
        availableHeights.clear();
        playingHeight = 0;
        applyQuality();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(server.getStreamUrl())));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    // ───────────── Video quality ─────────────

    private int maxHeightPref() {
        return getSharedPreferences(PREFS_PLAYER, MODE_PRIVATE).getInt(KEY_MAX_HEIGHT, 0);
    }

    /** Applies the saved quality as a cap, so it carries over to streams with other ladders. */
    private void applyQuality() {
        if (player == null) return;
        int max = maxHeightPref();
        TrackSelectionParameters.Builder params = player.getTrackSelectionParameters().buildUpon();
        if (max == 0) {
            params.clearVideoSizeConstraints().setForceLowestBitrate(false);
        } else if (max < 0) {
            params.clearVideoSizeConstraints().setForceLowestBitrate(true);
        } else {
            params.setMaxVideoSize(Integer.MAX_VALUE, max).setForceLowestBitrate(false);
        }
        player.setTrackSelectionParameters(params.build());
    }

    private void showQualityDialog() {
        // Options: Auto, the stream's own qualities (or common ones before it loads), Data saver
        java.util.List<Integer> heights = new java.util.ArrayList<>(availableHeights);
        if (heights.isEmpty()) heights = java.util.Arrays.asList(1080, 720, 480, 360);
        java.util.List<Integer> values = new java.util.ArrayList<>();
        java.util.List<CharSequence> labels = new java.util.ArrayList<>();
        values.add(0);
        labels.add(getString(R.string.quality_option_auto));
        for (int h : heights) {
            values.add(h);
            labels.add(getString(R.string.quality_option_up_to, h));
        }
        values.add(-1);
        labels.add(getString(R.string.quality_option_lowest));

        int current = maxHeightPref();
        int checked = values.indexOf(current);
        if (checked < 0) {
            // Saved cap isn't one of this stream's qualities: show it anyway
            values.add(1, current);
            labels.add(1, getString(R.string.quality_option_up_to, current));
            checked = 1;
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.video_quality)
                        .setIcon(R.drawable.ic_hd)
                        .setSingleChoiceItems(labels.toArray(new CharSequence[0]), checked, (d, which) -> {
                            getSharedPreferences(PREFS_PLAYER, MODE_PRIVATE).edit()
                                    .putInt(KEY_MAX_HEIGHT, values.get(which)).apply();
                            applyQuality();
                            renderQualityLabel();
                            d.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, null);
        builder.show();
        if (availableHeights.size() == 1) {
            Toast.makeText(this, getString(R.string.quality_single, availableHeights.first()), Toast.LENGTH_LONG).show();
        }
    }

    private void renderQualityLabel() {
        int max = maxHeightPref();
        String choice = max == 0 ? getString(R.string.quality_short_auto)
                : max < 0 ? getString(R.string.quality_short_lowest)
                : max + "p";
        qualityDetailsButton.setText(playingHeight > 0
                ? getString(R.string.quality_label_playing, choice, playingHeight)
                : getString(R.string.quality_label, choice));
    }

    private void showPlayerMessage(int icon, int title, int body, boolean retry) {
        playerMessage.setVisibility(View.VISIBLE);
        playerMessageIcon.setImageResource(icon);
        playerMessageTitle.setText(title);
        playerMessageBody.setVisibility(body != 0 ? View.VISIBLE : View.GONE);
        if (body != 0) playerMessageBody.setText(body);
        playerRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void hidePlayerMessage() {
        playerMessage.setVisibility(View.GONE);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) enterFullScreen();
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) exitFullScreen();
    }

    private void enterFullScreen() {
        exitFullScreenOnBack.setEnabled(true);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());

        detailsScroll.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        topScrim.setVisibility(View.GONE);
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.player_bg));

        ViewGroup.LayoutParams params = playerContainer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerContainer.setLayoutParams(params);
        playerView.setFullscreenButtonState(true);
    }

    private void exitFullScreen() {
        exitFullScreenOnBack.setEnabled(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .show(WindowInsetsCompat.Type.systemBars());

        detailsScroll.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        topScrim.setVisibility(View.VISIBLE);
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.bg));

        // 16:9 of the window width, but never more than 60% of the height (tablets)
        float density = getResources().getDisplayMetrics().density;
        Configuration config = getResources().getConfiguration();
        int width = (int) (config.screenWidthDp * density);
        int maxHeight = (int) (config.screenHeightDp * density * 0.6f);
        ViewGroup.LayoutParams params = playerContainer.getLayoutParams();
        params.height = Math.min(width * 9 / 16, maxHeight);
        playerContainer.setLayoutParams(params);
        playerView.setFullscreenButtonState(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Resume the stream when coming back from the background
        if (player == null && currentServer != null) initializePlayer(currentServer);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
