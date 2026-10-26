package com.example.iptv.activities.user;

import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Rational;
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
import androidx.lifecycle.Lifecycle;

import com.bumptech.glide.Glide;
import com.example.iptv.NetworkMonitor;
import com.example.iptv.Prefs;
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
import com.example.iptv.database.HistoryDAO;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@UnstableApi
public class ChannelDetailActivity extends BaseActivity {

    public static final String EXTRA_CHANNEL_ID = "channel_id";
    /** Plays a URL that isn't a saved channel (Play a link / playlist entries). */
    public static final String EXTRA_STREAM_URL = "stream_url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_LOGO = "logo";
    public static final String EXTRA_USER_AGENT = "user_agent";
    public static final String EXTRA_REFERRER = "referrer";

    private static final String TAG = "Player";
    /** A stream still loading after this long counts as failed (then the next server is tried). */
    private static final long START_TIMEOUT_MS = 20_000;

    private ImageView imageChannelLogo;
    private TextView textChannelName, textCategoryName, textCountryName, textServerCount;
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private Channel channel;
    private boolean adHoc; // a pasted link, not a saved channel
    private DBHelper dbHelper;
    private View rootLayout, detailsScroll, backButton, topScrim, playerActions, channelHeader, topBar, screenGlow;
    private com.google.android.material.card.MaterialCardView playerContainer;
    private View playerMessage, playerRetry;
    private TextView playerMessageTitle, playerMessageBody;
    private ImageView playerMessageIcon;
    private ChipGroup serverChipGroup;
    private MaterialButton favoriteButton, qualityDetailsButton;
    private FavoriteDAO favoriteDAO;
    private boolean isFavorite;
    private List<ChannelServer> servers;
    private ChannelServer currentServer;
    private ChannelServerDAO serverDAO;
    private OnBackPressedCallback exitFullScreenOnBack;

    // Video quality (see Prefs.maxVideoHeight)
    private final TreeSet<Integer> availableHeights = new TreeSet<>(Collections.reverseOrder());
    private int playingHeight;

    // Subtitle tracks offered by the current stream
    private final List<Tracks.Group> textGroups = new ArrayList<>();

    // Auto-switch: servers already tried since the viewer last picked one by hand
    private final Set<Integer> triedServers = new HashSet<>();
    private boolean switchingAutomatically;
    private boolean startedPlaying;
    private boolean historyRecorded;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable startTimeout = this::onStartTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);

        rootLayout = findViewById(R.id.root_layout);
        playerContainer = findViewById(R.id.player_container);
        detailsScroll = findViewById(R.id.details_scroll);
        channelHeader = findViewById(R.id.channel_header);
        topBar = findViewById(R.id.top_bar);
        screenGlow = findViewById(R.id.screen_glow);
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
        playerActions = findViewById(R.id.player_actions);
        playerMessage = findViewById(R.id.player_message);
        playerMessageIcon = findViewById(R.id.player_message_icon);
        playerMessageTitle = findViewById(R.id.player_message_title);
        playerMessageBody = findViewById(R.id.player_message_body);
        playerRetry = findViewById(R.id.player_retry);
        qualityDetailsButton = findViewById(R.id.button_quality_details);

        UiUtils.applyInsets(rootLayout, true, false);
        UiUtils.applyInsets(findViewById(R.id.details_content), false, true);

        dbHelper = DBHelper.getInstance(this);
        channel = loadChannel(getIntent());
        if (channel == null) {
            Toast.makeText(this, R.string.channel_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.button_quality).setOnClickListener(v -> showQualityDialog());
        qualityDetailsButton.setOnClickListener(v -> showQualityDialog());
        findViewById(R.id.button_subtitles).setOnClickListener(v -> showSubtitlesDialog());
        findViewById(R.id.button_pip).setOnClickListener(v -> {
            if (!enterPip()) Toast.makeText(this, R.string.pip_unsupported, Toast.LENGTH_SHORT).show();
        });
        // The buttons over the video come and go with the player controls
        playerView.setControllerVisibilityListener(
                (PlayerView.ControllerVisibilityListener) visibility -> {
                    if (!isInPip()) playerActions.setVisibility(visibility);
                });
        // Respect the phone's caption style (Settings › Accessibility › Caption preferences)
        if (playerView.getSubtitleView() != null) {
            playerView.getSubtitleView().setUserDefaultStyle();
            playerView.getSubtitleView().setUserDefaultTextSize();
        }
        renderQualityLabel();
        playerRetry.setOnClickListener(v -> {
            if (currentServer != null) {
                triedServers.clear();
                initializePlayer(currentServer);
            }
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

    /** Opening another channel while this one plays in picture-in-picture (singleTask). */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        releasePlayer();
        recreate();
    }

    private Channel loadChannel(Intent intent) {
        String url = intent.getStringExtra(EXTRA_STREAM_URL);
        if (url != null && !url.isEmpty()) {
            adHoc = true;
            String title = intent.getStringExtra(EXTRA_TITLE);
            List<ChannelServer> list = new ArrayList<>();
            list.add(new ChannelServer(0, getString(R.string.direct_link), url,
                    intent.getStringExtra(EXTRA_USER_AGENT), intent.getStringExtra(EXTRA_REFERRER)));
            return new Channel(title != null && !title.isEmpty() ? title : url,
                    intent.getStringExtra(EXTRA_LOGO), 0, 0, list);
        }
        int channelId = intent.getIntExtra(EXTRA_CHANNEL_ID, -1);
        try {
            return channelId > 0
                    ? new ChannelDAO(dbHelper.getWritableDatabase()).getById(channelId)
                    : (Channel) intent.getSerializableExtra("channel");
        } catch (RuntimeException e) {
            Log.e(TAG, "Couldn't load channel " + channelId, e);
            return null;
        }
    }

    private void bindChannelInfo() {
        textChannelName.setText(channel.getName());
        Glide.with(this).load(channel.getLogoUrl())
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(imageChannelLogo);

        if (adHoc) {
            findViewById(R.id.meta_chips).setVisibility(View.GONE);
        } else {
            // The category / country may have been deleted since the list was loaded
            Category category = channel.getCategoryId() != 0
                    ? new CategoryDAO(dbHelper.getWritableDatabase()).getById(channel.getCategoryId()) : null;
            Country country = channel.getCountryId() != 0
                    ? new CountryDAO(dbHelper.getWritableDatabase()).getById(channel.getCountryId()) : null;
            textCategoryName.setText(category != null ? UiUtils.categoryLabel(this, category.getName()) : getString(R.string.unknown));
            textCountryName.setText(country != null ? UiUtils.countryLabel(country.getName()) : getString(R.string.unknown));
        }

        serverDAO = new ChannelServerDAO(dbHelper.getWritableDatabase());
        servers = channel.getServers();
        int serverCount = servers == null ? 0 : servers.size();
        textServerCount.setText(UiUtils.quantity(this, R.plurals.count_servers, serverCount));
    }

    private void setupFavoriteButton() {
        if (adHoc) {
            favoriteButton.setVisibility(View.GONE);
            return;
        }
        favoriteDAO = new FavoriteDAO(dbHelper.getWritableDatabase());
        int channelId = channel.getId();
        isFavorite = favoriteDAO.isFavorite(channelId);
        renderFavorite();
        favoriteButton.setOnClickListener(v -> {
            try {
                if (isFavorite) favoriteDAO.removeFromFavorites(channelId);
                else favoriteDAO.addToFavorites(channelId);
                isFavorite = !isFavorite;
                renderFavorite();
            } catch (RuntimeException e) {
                Log.e(TAG, "Couldn't update favorites", e);
            }
        });
    }

    private void renderFavorite() {
        favoriteButton.setText(isFavorite ? R.string.in_favorites : R.string.add_to_favorites);
        favoriteButton.setIconResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        favoriteButton.setIconTint(ContextCompat.getColorStateList(this,
                isFavorite ? R.color.accent : R.color.text_primary));
    }

    // ───────────── Servers ─────────────

    private void setupServers() {
        if (servers == null || servers.isEmpty()) {
            findViewById(R.id.text_no_servers).setVisibility(View.VISIBLE);
            showPlayerMessage(R.drawable.ic_dns, R.string.player_no_servers, 0, false);
            return;
        }

        for (int i = 0; i < servers.size(); i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_server_chip, serverChipGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(UiUtils.serverLabel(this, servers.get(i).getServerName()));
            chip.setTag(i);
            renderChipStatus(chip, servers.get(i));
            serverChipGroup.addView(chip);
        }

        serverChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            // A server picked by hand starts a fresh round of auto-switching
            if (!switchingAutomatically) triedServers.clear();
            playServer((int) chip.getTag());
        });

        ((Chip) serverChipGroup.getChildAt(firstServerToTry())).setChecked(true);
    }

    /** First server that isn't known to be broken (or the first one if all are). */
    private int firstServerToTry() {
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getStatus() != ChannelServer.STATUS_BROKEN) return i;
        }
        return 0;
    }

    /** Next server for auto-switch: untried, preferring working / unchecked ones. Or -1. */
    private int nextServerToTry() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < servers.size(); i++) {
                if (triedServers.contains(i)) continue;
                boolean broken = servers.get(i).getStatus() == ChannelServer.STATUS_BROKEN;
                if (pass == 0 && broken) continue; // first pass skips known-broken servers
                return i;
            }
        }
        return -1;
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
        if (server.getId() > 0) {
            try {
                serverDAO.updateStatus(server.getId(), status, server.getLastChecked());
            } catch (RuntimeException e) {
                // Never let bookkeeping stop playback
                Log.e(TAG, "Couldn't save server status", e);
            }
        }
        for (int i = 0; i < serverChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) serverChipGroup.getChildAt(i);
            if (servers.get((int) chip.getTag()) == server) renderChipStatus(chip, server);
        }
    }

    private void playServer(int index) {
        ChannelServer server = servers.get(index);
        triedServers.add(index);
        String selectedUrl = server.getStreamUrl();
        if (selectedUrl != null && !selectedUrl.isEmpty()) {
            // Played as-is: cleartext is allowed (network_security_config.xml), and rewriting
            // http:// to https:// broke every stream that is only served over HTTP.
            initializePlayer(server);
        } else {
            Toast.makeText(this, R.string.no_stream_url, Toast.LENGTH_SHORT).show();
            onServerFailed(server);
        }
    }

    /** The current server failed: try the next one, or tell the viewer none works. */
    private void onServerFailed(ChannelServer failed) {
        handler.removeCallbacks(startTimeout);
        if (!NetworkMonitor.hasValidatedNetwork(this)) {
            // Our connection, not the server: don't mark it broken or move on
            showPlayerMessage(R.drawable.ic_wifi_off, R.string.player_offline_title, R.string.player_offline_body, true);
            return;
        }
        recordServerStatus(failed, ChannelServer.STATUS_BROKEN);
        int next = Prefs.autoSwitchServers(this) ? nextServerToTry() : -1;
        if (next >= 0) {
            Toast.makeText(this, getString(R.string.server_switched, UiUtils.serverLabel(this, failed.getServerName()),
                    UiUtils.serverLabel(this, servers.get(next).getServerName())), Toast.LENGTH_SHORT).show();
            // Posted: this often runs inside the failing player's own callback, which must
            // return before that player is released
            handler.post(() -> {
                if (isFinishing()) return;
                switchingAutomatically = true;
                ((Chip) serverChipGroup.getChildAt(next)).setChecked(true);
                switchingAutomatically = false;
            });
            return;
        }
        boolean several = servers != null && servers.size() > 1;
        showPlayerMessage(R.drawable.ic_error,
                several ? R.string.player_all_failed_title : R.string.player_error_title,
                several ? R.string.player_all_failed_body : 0, true);
    }

    private void onStartTimeout() {
        if (player != null && !startedPlaying && currentServer != null) {
            Log.w(TAG, "Start timeout on " + currentServer.getServerName());
            ChannelServer failed = currentServer;
            releasePlayer();
            onServerFailed(failed);
        }
    }

    // ───────────── Player ─────────────

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(ChannelServer server) {
        currentServer = server;
        releasePlayer();
        hidePlayerMessage();
        startedPlaying = false;

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

        // FFmpeg decodes audio the phone can't (e.g. MP2 on Al Jadeed, AC-3); decoder fallback
        // tries another decoder instead of failing when the first one can't start
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true);

        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this, renderers)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this, httpFactory)))
                .build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), /* handleAudioFocus= */ true);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    startedPlaying = true;
                    handler.removeCallbacks(startTimeout);
                    hidePlayerMessage();
                    recordServerStatus(server, ChannelServer.STATUS_WORKING);
                    recordHistory();
                    updatePipParams();
                }
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                availableHeights.clear();
                textGroups.clear();
                for (Tracks.Group group : tracks.getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_VIDEO) {
                        // Qualities the stream offers (HLS variants)
                        for (int i = 0; i < group.length; i++) {
                            int h = group.getTrackFormat(i).height;
                            if (h > 0 && group.isTrackSupported(i)) availableHeights.add(h);
                        }
                    } else if (group.getType() == C.TRACK_TYPE_TEXT && group.isSupported()) {
                        textGroups.add(group);
                    }
                }
                autoSelectSubtitles(tracks);
            }

            @Override
            public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                playingHeight = videoSize.height;
                renderQualityLabel();
                updatePipParams();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePipParams(); // auto-enter PiP only while something plays
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.w(TAG, "Playback error on " + server.getServerName() + ": " + error.getErrorCodeName(), error);
                onServerFailed(server);
            }
        });
        availableHeights.clear();
        playingHeight = 0;
        applyTrackPreferences();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(server.getStreamUrl())));
        player.prepare();
        player.setPlayWhenReady(true);
        handler.postDelayed(startTimeout, START_TIMEOUT_MS);
    }

    private void recordHistory() {
        if (historyRecorded || adHoc || channel.getId() <= 0) return;
        historyRecorded = true;
        try {
            new HistoryDAO(dbHelper.getWritableDatabase()).recordWatched(channel.getId());
        } catch (RuntimeException e) {
            Log.e(TAG, "Couldn't save watch history", e);
        }
    }

    /** Quality cap and subtitle preference, applied before playback starts. */
    private void applyTrackPreferences() {
        if (player == null) return;
        int max = Prefs.maxVideoHeight(this);
        TrackSelectionParameters.Builder params = player.getTrackSelectionParameters().buildUpon();
        if (max == 0) {
            params.clearVideoSizeConstraints().setForceLowestBitrate(false);
        } else if (max < 0) {
            params.clearVideoSizeConstraints().setForceLowestBitrate(true);
        } else {
            params.setMaxVideoSize(Integer.MAX_VALUE, max).setForceLowestBitrate(false);
        }
        boolean subtitlesOn = Prefs.SUBTITLES_AUTO.equals(Prefs.subtitlesMode(this));
        params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesOn);
        if (subtitlesOn) {
            params.setPreferredTextLanguage(Prefs.subtitleLanguage(this))
                    .setSelectUndeterminedTextLanguage(true);
        }
        player.setTrackSelectionParameters(params.build());
    }

    // ───────────── Subtitles ─────────────

    /**
     * "Auto": if the stream has subtitles but none got selected (none in the preferred
     * language), show the first track rather than nothing.
     */
    private void autoSelectSubtitles(Tracks tracks) {
        if (player == null || textGroups.isEmpty()) return;
        if (!Prefs.SUBTITLES_AUTO.equals(Prefs.subtitlesMode(this))) return;
        if (tracks.isTypeSelected(C.TRACK_TYPE_TEXT)) return;
        if (!player.getTrackSelectionParameters().overrides.isEmpty()) return;
        Tracks.Group group = textGroups.get(0);
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(), 0))
                .build());
    }

    private void showSubtitlesDialog() {
        String preferredName = displayLanguage(Prefs.subtitleLanguage(this));
        List<CharSequence> labels = new ArrayList<>();
        labels.add(getString(R.string.subtitles_off));
        labels.add(getString(R.string.subtitles_auto, preferredName));
        for (Tracks.Group group : textGroups) labels.add(trackLabel(group.getTrackFormat(0)));

        int checked = Prefs.SUBTITLES_OFF.equals(Prefs.subtitlesMode(this)) ? 0 : 1;
        boolean overridden = player != null && !player.getTrackSelectionParameters().overrides.isEmpty();
        for (int i = 0; i < textGroups.size(); i++) {
            if (overridden && textGroups.get(i).isSelected()) checked = i + 2;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.subtitles)
                .setIcon(R.drawable.ic_cc)
                .setSingleChoiceItems(labels.toArray(new CharSequence[0]), checked, (d, which) -> {
                    if (which == 0) {
                        Prefs.setSubtitlesMode(this, Prefs.SUBTITLES_OFF);
                        clearTextOverride();
                    } else if (which == 1) {
                        Prefs.setSubtitlesMode(this, Prefs.SUBTITLES_AUTO);
                        clearTextOverride();
                    } else {
                        // A specific track: remember its language for next time
                        Tracks.Group group = textGroups.get(which - 2);
                        String lang = group.getTrackFormat(0).language;
                        Prefs.setSubtitlesMode(this, Prefs.SUBTITLES_AUTO);
                        if (lang != null && !lang.isEmpty() && !"und".equals(lang)) Prefs.setSubtitleLanguage(this, lang);
                        if (player != null) {
                            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(), 0))
                                    .build());
                        }
                    }
                    applySubtitleMode();
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                // Android's Live Caption is the only way to get captions for streams that send none
                .setNeutralButton(R.string.live_caption, (d, w) -> openCaptionSettings())
                .show();
        if (textGroups.isEmpty()) {
            Toast.makeText(this, R.string.subtitles_none_in_stream, Toast.LENGTH_LONG).show();
        }
    }

    private void clearTextOverride() {
        if (player == null) return;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT).build());
    }

    private void applySubtitleMode() {
        if (player == null) return;
        boolean on = Prefs.SUBTITLES_AUTO.equals(Prefs.subtitlesMode(this));
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !on)
                .setPreferredTextLanguage(Prefs.subtitleLanguage(this))
                .build());
    }

    private void openCaptionSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    private String trackLabel(Format format) {
        String name = format.label;
        if (name == null || name.isEmpty()) name = displayLanguage(format.language);
        if ((format.roleFlags & C.ROLE_FLAG_CAPTION) != 0
                || (format.sampleMimeType != null && format.sampleMimeType.contains("cea"))) {
            name += " (CC)";
        }
        return name;
    }

    private String displayLanguage(String code) {
        if (code == null || code.isEmpty() || "und".equals(code)) return getString(R.string.unknown);
        String name = new Locale(code).getDisplayLanguage(Locale.getDefault());
        return name.isEmpty() ? code : name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
    }

    // ───────────── Video quality ─────────────

    private void showQualityDialog() {
        // Options: Auto, the stream's own qualities (or common ones before it loads), Data saver
        List<Integer> heights = new ArrayList<>(availableHeights);
        if (heights.isEmpty()) heights = Arrays.asList(1080, 720, 480, 360);
        List<Integer> values = new ArrayList<>();
        List<CharSequence> labels = new ArrayList<>();
        values.add(0);
        labels.add(getString(R.string.quality_option_auto));
        for (int h : heights) {
            values.add(h);
            labels.add(getString(R.string.quality_option_up_to, h));
        }
        values.add(-1);
        labels.add(getString(R.string.quality_option_lowest));

        int current = Prefs.maxVideoHeight(this);
        int checked = values.indexOf(current);
        if (checked < 0) {
            // Saved cap isn't one of this stream's qualities: show it anyway
            values.add(1, current);
            labels.add(1, getString(R.string.quality_option_up_to, current));
            checked = 1;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.video_quality)
                .setIcon(R.drawable.ic_hd)
                .setSingleChoiceItems(labels.toArray(new CharSequence[0]), checked, (d, which) -> {
                    Prefs.setMaxVideoHeight(this, values.get(which));
                    applyTrackPreferences();
                    renderQualityLabel();
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        if (availableHeights.size() == 1) {
            Toast.makeText(this, getString(R.string.quality_single, availableHeights.first()), Toast.LENGTH_LONG).show();
        }
    }

    private void renderQualityLabel() {
        int max = Prefs.maxVideoHeight(this);
        String choice = max == 0 ? getString(R.string.quality_short_auto)
                : max < 0 ? getString(R.string.quality_short_lowest)
                : max + "p";
        qualityDetailsButton.setText(playingHeight > 0
                ? getString(R.string.quality_label_playing, choice, playingHeight)
                : getString(R.string.quality_label, choice));
    }

    // ───────────── Picture-in-picture ─────────────

    private boolean pipSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    private boolean isInPip() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
    }

    /** @return false when the device can't do picture-in-picture */
    private boolean enterPip() {
        if (!pipSupported()) return false;
        try {
            return enterPictureInPictureMode(pipParams());
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.w(TAG, "Couldn't enter picture-in-picture", e);
            return false;
        }
    }

    private PictureInPictureParams pipParams() {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
        VideoSize size = player != null ? player.getVideoSize() : VideoSize.UNKNOWN;
        Rational ratio = size.width > 0 && size.height > 0 ? new Rational(size.width, size.height) : new Rational(16, 9);
        // Android only accepts ratios between 1:2.39 and 2.39:1
        float r = ratio.floatValue();
        if (r > 2.39f) ratio = new Rational(239, 100);
        else if (r < 1 / 2.39f) ratio = new Rational(100, 239);
        builder.setAspectRatio(ratio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: go to PiP smoothly when the viewer leaves the app
            builder.setAutoEnterEnabled(Prefs.autoPip(this) && player != null && player.isPlaying());
            builder.setSeamlessResizeEnabled(true);
        }
        return builder.build();
    }

    private void updatePipParams() {
        if (!pipSupported()) return;
        try {
            setPictureInPictureParams(pipParams());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
        }
    }

    /** Android 8–11: leaving the app (Home) while a channel plays goes to PiP. */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Prefs.autoPip(this)
                && player != null && player.isPlaying()) {
            enterPip();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean inPip, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(inPip, newConfig);
        if (inPip) {
            // Just the video in the small window
            playerView.setUseController(false);
            playerActions.setVisibility(View.GONE);
            setChromeVisible(false);
            if (startedPlaying) hidePlayerMessage();
            setPlayerFramed(false);
        } else {
            playerView.setUseController(true);
            if (getLifecycle().getCurrentState() == Lifecycle.State.CREATED) {
                // The PiP window was closed (not expanded): stop playing
                finish();
                return;
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                enterFullScreen();
            } else {
                exitFullScreen();
            }
        }
    }

    // ───────────── Layout ─────────────

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
        if (isInPip()) return; // PiP resizes are handled by onPictureInPictureModeChanged
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) enterFullScreen();
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) exitFullScreen();
    }

    private void enterFullScreen() {
        exitFullScreenOnBack.setEnabled(true);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());

        setChromeVisible(false);
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.player_bg));
        setPlayerFramed(false);
        playerView.setFullscreenButtonState(true);
    }

    private void exitFullScreen() {
        exitFullScreenOnBack.setEnabled(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .show(WindowInsetsCompat.Type.systemBars());

        setChromeVisible(true);
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.bg));
        setPlayerFramed(true);
        playerView.setFullscreenButtonState(false);
    }

    /** Everything around the video: top bar, glow, channel card, buttons and servers. */
    private void setChromeVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        topBar.setVisibility(v);
        screenGlow.setVisibility(v);
        channelHeader.setVisibility(v);
        detailsScroll.setVisibility(v);
        topScrim.setVisibility(v);
    }

    /**
     * Framed: rounded card inset from the edges, 16:9 of the available width (never more than
     * 60% of the height, for tablets). Unframed: edge-to-edge for fullscreen / picture-in-picture.
     */
    private void setPlayerFramed(boolean framed) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) playerContainer.getLayoutParams();
        if (framed) {
            int gutter = getResources().getDimensionPixelSize(R.dimen.page_gutter);
            float density = getResources().getDisplayMetrics().density;
            Configuration config = getResources().getConfiguration();
            int width = (int) (config.screenWidthDp * density) - 2 * gutter;
            int maxHeight = (int) (config.screenHeightDp * density * 0.6f);
            params.setMarginStart(gutter);
            params.setMarginEnd(gutter);
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_md);
            params.height = Math.min(width * 9 / 16, maxHeight);
            playerContainer.setRadius(getResources().getDimension(R.dimen.radius_lg));
        } else {
            params.setMarginStart(0);
            params.setMarginEnd(0);
            params.topMargin = 0;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerContainer.setRadius(0);
        }
        playerContainer.setLayoutParams(params);
    }

    // ───────────── Lifecycle ─────────────

    @Override
    protected void onStart() {
        super.onStart();
        // Resume the stream when coming back from the background
        if (player == null && currentServer != null) initializePlayer(currentServer);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // In picture-in-picture the activity is only paused, so playback continues there
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void releasePlayer() {
        handler.removeCallbacks(startTimeout);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    /** Opens the player for a URL that isn't a saved channel. */
    public static Intent forLink(Context context, String url, String title, String logo) {
        return forLink(context, url, title, logo, null, null);
    }

    public static Intent forLink(Context context, String url, String title, String logo,
                                 String userAgent, String referrer) {
        return new Intent(context, ChannelDetailActivity.class)
                .putExtra(EXTRA_STREAM_URL, url)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_LOGO, logo)
                .putExtra(EXTRA_USER_AGENT, userAgent)
                .putExtra(EXTRA_REFERRER, referrer);
    }
}
