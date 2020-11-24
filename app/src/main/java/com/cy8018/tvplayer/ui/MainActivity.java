package com.cy8018.tvplayer.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.PictureDrawable;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.AppDatabase;
import com.cy8018.tvplayer.db.ChannelData;
import com.cy8018.tvplayer.model.IptvStation;
import com.cy8018.tvplayer.util.SimpleM3UParser;
import com.cy8018.tvplayer.util.SvgSoftwareLayerSetter;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements Player.EventListener, AnalyticsListener {

    private static final String TAG = "MainActivity";

    // channel list
    public static List<ChannelData> mChannelList;
    public static int gTotalChannelCount = 0;
    private ChannelData mCurrentChannel;
    private int mCurrentChannelIndex;
    private int mCurrentSourceIndex;

    // message to load the station list
    public static final int MSG_LOAD_LIST = 0;

    // message to play the radio
    public static final int MSG_PLAY = 1;

    // message to get buffering info
    public static final int MSG_GET_BUFFERING_INFO = 2;

    // message to now playing bar
    public static final int MSG_HIDE_NOW_PLAYING_BAR = 3;

    public static final int NOW_PLAYING_BAR_FADING_TIME = 7000;

    public static final int NET_SPEED_CHECK_INTERVAL = 1000;

    private SimpleExoPlayer player;
    private DataSource.Factory dataSourceFactory;

    private RetainedFragment dataFragment;
    public Fragment selectedFragment, homeFragment, channelsFragment, settingsFragment;
    private Dialog mFullScreenDialog;
    private PlayerView playerView;
    public LoadingDialog loadingDialog;
    private BottomNavigationView bottomNav;

    private View mainFrame, appTitleBar, nowPlayingBar, nowPlayingBall, mediaFrame, bufferInfoMediaFrame, controlOverlay;
    private TextView sourceInfoBar, sourceInfoMediaFrame, bufferPercentageMediaFrame, netSpeedBar, netSpeedBall, netSpeedOverlay, netSpeedMediaFrame, sourceInfoOverlay, channelNameOverlay, aspectRatioTextOverlay, channelNameBar, channelInfoBar, channelNameMediaFrame;
    private ImageView countryFlagBar, favIconBar, channelLogoBar, mFullScreenIcon, favIconOverlay, aspectRatioIconOverlay, favIconMediaFrame;
    protected GifImageView loadingPicMediaFrame, playButtonBar, playBtnBall, playButtonOverlay;
    protected CircleImageView channelLogoBall;

    private Animation slideInTopAnim, slideInLeftAnim, slideInLeftBarAnim, slideOutTopAnim, slideOutLeftAnim, slideOutLeftBarAnim, fadeOutAnim, fadeOutFastAnim, fadeInAnim, fadeInFastAnim, rotateAnim, scaleAnim;

    private RequestBuilder<PictureDrawable> requestBuilder;

    private final String STATE_RESUME_WINDOW = "resumeWindow";
    private final String STATE_PLAYER_ASPECT_RATIO = "aspectRatio";
    private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";

    private int mCurrentAspectRatio;

    private int mResumeWindow;
    private boolean mExoPlayerFullscreen = false;

    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    private long lastBarActiveTimeStamp = 0;

    protected boolean isBuffering = false;
    public boolean isLoading = false;

    protected static boolean isCheckingThreadRunning = false;

    protected Thread checkingThread;

    public final MsgHandler mHandler = new MsgHandler(this);

    protected int mPlaybackStatus;
    static class PlaybackStatus {
        static final int IDLE = 0;
        static final int LOADING = 1;
        static final int PLAYING = 2;
        static final int PAUSED = 3;
        static final int STOPPED = 4;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCurrentAspectRatio = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        Log.d(TAG, "onCreate: ");

        dataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(this, getString(R.string.app_name)),
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS * 2,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS * 2,
                true);

        loadingDialog = new LoadingDialog(this);

        // Animations
        slideInTopAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        slideInLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        slideInLeftBarAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_bar);
        slideOutTopAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_top);
        slideOutLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slideOutLeftBarAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left_bar);
        fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        fadeOutFastAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out_fast);
        fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeInFastAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_fast);
        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale);

        mainFrame = findViewById(R.id.main_frame);

        // App Title Bar
        appTitleBar = findViewById(R.id.app_title_bar);

        // Control overlay
        controlOverlay = findViewById(R.id.control_overlay);
        controlOverlay.setVisibility(View.INVISIBLE);

        // MediaFrame
        mediaFrame = findViewById(R.id.media_frame);
        bufferInfoMediaFrame = findViewById(R.id.buffer_info_media_frame);
        bufferInfoMediaFrame.setVisibility(View.INVISIBLE);
        bufferPercentageMediaFrame = findViewById(R.id.buffer_percentage_media_frame);
        netSpeedMediaFrame = findViewById(R.id.net_speed_media_frame);
        loadingPicMediaFrame = findViewById(R.id.loading_pic_media_frame);
        loadingPicMediaFrame.setImageResource(R.drawable.loading_wave);
        channelNameMediaFrame = findViewById(R.id.channel_name_media_frame);
        favIconMediaFrame = findViewById(R.id.fav_icon_media_frame);
        favIconMediaFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favIconMediaFrame.startAnimation(scaleAnim);
                onCurrentFavIconTapped();
            }
        });
        sourceInfoMediaFrame = findViewById(R.id.source_info_media_frame);
        sourceInfoMediaFrame.setVisibility(View.GONE);
        sourceInfoMediaFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceInfoMediaFrame.startAnimation(scaleAnim);
                switchSource();
                setLastBarActiveTimeStamp();
            }
        });


        // Now Playing Ball
        nowPlayingBall = findViewById(R.id.now_playing_ball);
        nowPlayingBall.setVisibility(View.GONE);
        netSpeedBall = findViewById(R.id.net_speed_ball);
        channelLogoBall = findViewById(R.id.channel_logo_ball);
        channelLogoBall.setVisibility(View.GONE);
        channelLogoBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNowPlayingBall();
                showNowPlayingBar();
                switchToHome();
            }
        });
        playBtnBall = findViewById(R.id.play_button_ball);
        playBtnBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNowPlayingBall();
                showNowPlayingBar();
                switchToHome();
            }
        });


        // Now Playing Bar
        nowPlayingBar = findViewById(R.id.now_playing_bar);
        nowPlayingBar.setVisibility(View.GONE);
        playButtonBar = findViewById(R.id.play_button_bar);
        countryFlagBar = findViewById(R.id.country_flag_bar);
        channelInfoBar = findViewById(R.id.channel_info_bar);
        favIconBar = findViewById(R.id.fav_icon_bar);
        channelNameBar = findViewById(R.id.channel_name_bar);
        channelLogoBar = findViewById(R.id.channel_logo_bar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            channelLogoBar.setClipToOutline(true);
        }
        sourceInfoBar = findViewById(R.id.source_info_bar);
        netSpeedBar = findViewById(R.id.net_speed_bar);
        playButtonBar.setImageResource(R.drawable.play);
        playButtonBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "OnClickListener mPlaybackStatus: " + mPlaybackStatus);
                setLastBarActiveTimeStamp();
                onPlayButtonTapped();
            }
        });
        favIconBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favIconBar.startAnimation(scaleAnim);
                onCurrentFavIconTapped();
            }
        });
        nowPlayingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToHome();
                setLastBarActiveTimeStamp();
            }
        });
        sourceInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceInfoBar.startAnimation(scaleAnim);
                switchSource();
                setLastBarActiveTimeStamp();
            }
        });
        countryFlagBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlaybackStatus != PlaybackStatus.PLAYING) {
                    nowPlayingBar.startAnimation(slideOutLeftAnim);
                    nowPlayingBar.setVisibility(View.GONE);
                    stopPlayer();
                }
            }
        });


        // Control Overlay
        playButtonOverlay = findViewById(R.id.play_button_overlay);
        channelNameOverlay = findViewById(R.id.channel_name_overlay);
        channelNameOverlay.setSelected(true);
        sourceInfoOverlay = findViewById(R.id.source_info_overlay);
        sourceInfoOverlay.setVisibility(View.GONE);
        netSpeedOverlay = findViewById(R.id.net_speed_overlay);
        favIconOverlay = findViewById(R.id.fav_icon_overlay);
        aspectRatioTextOverlay = findViewById(R.id.aspect_ratio_text_overlay);
        aspectRatioIconOverlay = findViewById(R.id.aspect_ratio_icon_overlay);
        aspectRatioIconOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchAspectRatio();
                aspectRatioIconOverlay.startAnimation(scaleAnim);
                aspectRatioTextOverlay.startAnimation(scaleAnim);
            }
        });
        sourceInfoOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceInfoOverlay.startAnimation(scaleAnim);
                switchSource();
            }
        });
        favIconOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favIconOverlay.startAnimation(scaleAnim);
                onCurrentFavIconTapped();
            }
        });
        playButtonOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayButtonTapped();
            }
        });

        if (savedInstanceState != null) {
            mResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
            mCurrentAspectRatio = savedInstanceState.getInt(STATE_PLAYER_ASPECT_RATIO);
            mExoPlayerFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
        }


        // Fragments
        homeFragment = new HomeFragment();
        channelsFragment = new ChannelsFragment();
        settingsFragment = new SettingsFragment();

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setSelectedItemId(R.id.nav_home);

        // find the retained fragment on activity restarts
        FragmentManager fm = getSupportFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag("data");
        // create the fragment and data the first time
        if (dataFragment == null) {
            // add the fragment
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment,"data").commit();
        }
        // the data is available in dataFragment.getData()
        PlayerFragmentData data = dataFragment.getData();
        if(data != null) {
            mChannelList = data.getChannelList();
            mCurrentSourceIndex = data.getCurrentSourceIndex();
            mCurrentChannelIndex = data.getCurrentChannelIndex();
            mCurrentChannel = data.getCurrentChannel();

            setCurrentPlayInfo(mCurrentChannel);
            showNowPlayingBall(true);
        }

        if (mChannelList == null || mChannelList.size() == 0) {
            mChannelList = AppDatabase.getInstance(getApplicationContext()).channelDao().getAll();
            gTotalChannelCount = mChannelList.size();
        }

        startCheckingThread();
    }

    public List<ChannelData> getChannelList() {
        return mChannelList;
    }

    private void switchAspectRatio() {

        if (mCurrentAspectRatio == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            mCurrentAspectRatio = AspectRatioFrameLayout.RESIZE_MODE_FILL;
        }
        else {
            mCurrentAspectRatio = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        }
        setAspectRatioText();
        playerView.setResizeMode(mCurrentAspectRatio);
    }

    private void onPlayButtonTapped() {
        switch (mPlaybackStatus) {
            case PlaybackStatus.IDLE:
            case PlaybackStatus.PAUSED:
                if (null != mCurrentChannel && !mCurrentChannel.url.isEmpty()) {
                    setCurrentPlayInfo();
                    play(mCurrentChannel.url.get(mCurrentSourceIndex));
                }
                break;
            case PlaybackStatus.PLAYING:
                pausePlayer();
                break;
            default:
        }
    }

    private void onCurrentFavIconTapped() {
        if (mCurrentChannel.isFavorite) {
            removeFromFavorites(mCurrentChannel.name);
            favIconBar.setImageResource(R.drawable.star_outline);
            favIconOverlay.setImageResource(R.drawable.star_outline_overlay);
            favIconMediaFrame.setImageResource(R.drawable.star_outline_overlay);
        } else {
            addToFavorites(mCurrentChannel.name);
            favIconBar.setImageResource(R.drawable.star);
            favIconOverlay.setImageResource(R.drawable.star_overlay);
            favIconMediaFrame.setImageResource(R.drawable.star_overlay);
        }

        setLastBarActiveTimeStamp();

        if (bottomNav.getSelectedItemId() == R.id.nav_home) {
            ((HomeFragment)selectedFragment).reloadList();
        }
    }

    public void addToFavorites(String channelName) {
        AppDatabase.getInstance(getApplicationContext()).channelDao().addToFavorites(channelName);
        for (ChannelData channel: mChannelList) {
            if (channel.name.equals(channelName)) {
                mChannelList.get(mChannelList.indexOf(channel)).isFavorite = true;
                break;
            }
        }
        if (mCurrentChannel != null && channelName.equals(mCurrentChannel.name)) {
            favIconBar.setImageResource(R.drawable.star);
            favIconOverlay.setImageResource(R.drawable.star_overlay);
        }
    }

    public void removeFromFavorites(String channelName) {
        AppDatabase.getInstance(getApplicationContext()).channelDao().removeFromFavorites(channelName);
        for (ChannelData channel: mChannelList) {
            if (channel.name.equals(channelName)) {
                mChannelList.get(mChannelList.indexOf(channel)).isFavorite = false;
                break;
            }
        }
        if (mCurrentChannel != null && channelName.equals(mCurrentChannel.name)) {
            favIconBar.setImageResource(R.drawable.star_outline);
            favIconOverlay.setImageResource(R.drawable.star_outline_overlay);
        }
    }

    public List<ChannelData> getChannelListFavorites() {

        List<ChannelData> favChannels = new ArrayList<>();
        for (ChannelData channel: mChannelList) {
            if (channel.isFavorite) {
                favChannels.add(channel);
            }
        }
        return favChannels;
    }

    public Animation getScaleAnim() {
        return scaleAnim;
    }

    public ChannelData getCurrentChannel() {
        return mCurrentChannel;
    }

    public int getTotalChannelCount() {
        return gTotalChannelCount;
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.nav_home:
                            if (selectedFragment != null && selectedFragment.getClass() == ChannelsFragment.class) {
                                ((ChannelsFragment) selectedFragment).clearFilter();
                            }
                            if (homeFragment == null) {
                                homeFragment = new HomeFragment();
                            }
                            selectedFragment = homeFragment;
                            mediaFrame.setVisibility(View.VISIBLE);
                            break;
                        case R.id.nav_channels:
                            if (selectedFragment != null && selectedFragment.getClass() == ChannelsFragment.class) {
                                ((ChannelsFragment) selectedFragment).clearFilter();
                            }
                            if (channelsFragment == null) {
                                channelsFragment = new ChannelsFragment();
                            }
                            selectedFragment = channelsFragment;
                            mediaFrame.setVisibility(View.GONE);
                            break;
                        case R.id.nav_setting:
                            if (selectedFragment != null && selectedFragment.getClass() == ChannelsFragment.class) {
                                ((ChannelsFragment) selectedFragment).clearFilter();
                            }
                            if (settingsFragment == null) {
                                settingsFragment = new SettingsFragment();
                            }
                            selectedFragment = settingsFragment;
                            mediaFrame.setVisibility(View.GONE);
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                    return true;
                }
            };

    @Override
    protected void onDestroy() {
        PlayerFragmentData fragmentData = new PlayerFragmentData();
        fragmentData.setChannelList(mChannelList);
        fragmentData.setCurrentChannel(mCurrentChannel);
        fragmentData.setCurrentChannelIndex(mCurrentChannelIndex);
        fragmentData.setCurrentSourceIndex(mCurrentSourceIndex);

        // store the data in the fragment
        dataFragment.setData(fragmentData);

        releasePlayer();

        stopCheckingThread();

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_RESUME_WINDOW, mResumeWindow);
        outState.putInt(STATE_PLAYER_ASPECT_RATIO, mCurrentAspectRatio);
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, mExoPlayerFullscreen);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "onPlayerStateChanged: playWhenReady:"+ playWhenReady + " playbackState:" + playbackState);
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                mPlaybackStatus = PlaybackStatus.LOADING;
                channelLogoBall.clearAnimation();
                channelLogoBall.setVisibility(View.GONE);
                playButtonBar.setVisibility(View.VISIBLE);
                playBtnBall.setVisibility(View.VISIBLE);
                playButtonBar.setImageResource(R.drawable.loading_circle);
                playBtnBall.setImageResource(R.drawable.loading_circle);
                playButtonOverlay.setImageResource(R.drawable.loading_wave);
                countryFlagBar.setImageResource(R.drawable.close_round);
                showBufferingInfo();
                break;
            case Player.STATE_ENDED:
                hideBufferingInfo();
                mPlaybackStatus = PlaybackStatus.STOPPED;
                playButtonBar.setImageResource(R.drawable.play);
                playButtonOverlay.setImageResource(R.drawable.play_overlay);
                countryFlagBar.setImageResource(R.drawable.close_round);
                break;
            case Player.STATE_READY:
                mPlaybackStatus = playWhenReady ? PlaybackStatus.PLAYING : PlaybackStatus.PAUSED;
                hideBufferingInfo();
                if (mPlaybackStatus == PlaybackStatus.PLAYING) {
                    setCurrentPlayInfo();
                    playBtnBall.setVisibility(View.GONE);
                    channelLogoBall.setVisibility(View.VISIBLE);
                    channelLogoBall.startAnimation(rotateAnim);
                    playButtonBar.setImageResource(R.drawable.pause);
                    playButtonOverlay.setImageResource(R.drawable.pause_overlay);
                }
                else {
                    channelLogoBall.clearAnimation();
                    playButtonBar.setImageResource(R.drawable.play);
                    playButtonOverlay.setImageResource(R.drawable.play_overlay);
                    countryFlagBar.setImageResource(R.drawable.close_round);
                }
                break;
            case Player.STATE_IDLE:
            default:
                mPlaybackStatus = PlaybackStatus.IDLE;

                setCurrentPlayInfo();
                hideBufferingInfo();
                if (mCurrentChannel != null) {
                    channelLogoBall.clearAnimation();
                    channelLogoBall.setVisibility(View.VISIBLE);
                }
                if (playBtnBall.getVisibility() == View.VISIBLE) {
                    playBtnBall.setVisibility(View.GONE);
                }
                playButtonBar.setImageResource(R.drawable.play);
                playButtonOverlay.setImageResource(R.drawable.play_overlay);
                countryFlagBar.setImageResource(R.drawable.close_round);
                break;
        }
    }

    private  void hideBufferingInfo () {
        isBuffering = false;

//        netSpeedOverlay.startAnimation(fadeInFastAnim);
//        channelNameOverlay.startAnimation(fadeInFastAnim);
//        favIconOverlay.startAnimation(fadeInFastAnim);

        netSpeedOverlay.setVisibility(View.VISIBLE);
        channelNameOverlay.setVisibility(View.VISIBLE);
        favIconOverlay.setVisibility(View.VISIBLE);

        if (sourceInfoOverlay.getText().length() > 0) {
//            sourceInfoOverlay.startAnimation(fadeInFastAnim);
            sourceInfoOverlay.setVisibility(View.VISIBLE);
        }

        if (bufferInfoMediaFrame.getVisibility() == View.VISIBLE) {
            bufferInfoMediaFrame.startAnimation(fadeOutFastAnim);
            bufferInfoMediaFrame.setVisibility(View.INVISIBLE);
        }

//        bufferPercentageMediaFrame.setVisibility(View.INVISIBLE);
//        channelNameMediaFrame.setVisibility(View.INVISIBLE);
//        sourceInfoMediaFrame.setVisibility(View.INVISIBLE);
//        loadingPicMediaFrame.setVisibility(View.INVISIBLE);
//        netSpeedMediaFrame.setVisibility(View.INVISIBLE);
//        favIconMediaFrame.setVisibility(View.INVISIBLE);

        netSpeedBar.setVisibility(View.GONE);
        netSpeedBall.setVisibility(View.GONE);
    }

    private void showBufferingInfo () {
        isBuffering = true;

        if (mCurrentChannel != null) {
            channelNameMediaFrame.setText(mCurrentChannel.name);
            channelNameMediaFrame.setVisibility(View.VISIBLE);
        }

        if (sourceInfoMediaFrame.getText().length()> 0) {
            if (sourceInfoMediaFrame.getVisibility() != View.VISIBLE) {
                sourceInfoMediaFrame.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            sourceInfoMediaFrame.setVisibility(View.GONE);
        }

        bufferInfoMediaFrame.setVisibility(View.VISIBLE);
//        netSpeedMediaFrame.setVisibility(View.VISIBLE);
//        favIconMediaFrame.setVisibility(View.VISIBLE);
//        loadingPicMediaFrame.setVisibility(View.VISIBLE);
//        sourceInfoMediaFrame.setVisibility(View.VISIBLE);
//        bufferPercentageMediaFrame.setVisibility(View.VISIBLE);

        if (channelNameOverlay.getVisibility() == View.VISIBLE && !mExoPlayerFullscreen) {
//            channelNameOverlay.startAnimation(fadeOutFastAnim);
//            netSpeedOverlay.startAnimation(fadeOutFastAnim);
//            sourceInfoOverlay.startAnimation(fadeOutFastAnim);
//            favIconOverlay.startAnimation(fadeOutFastAnim);

            channelNameOverlay.setVisibility(View.INVISIBLE);
            netSpeedOverlay.setVisibility(View.INVISIBLE);
            //sourceInfoOverlay.setVisibility(View.INVISIBLE);
            //favIconOverlay.setVisibility(View.INVISIBLE);
        }

        netSpeedBar.setVisibility(View.VISIBLE);
        netSpeedBall.setVisibility(View.VISIBLE);
    }

    private long getNetSpeed() {
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(getApplicationContext().getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : TrafficStats.getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            return calculationTime;
        }

        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }

    public String getNetSpeedText(long speed) {
        String text = "";
        if (speed >= 0 && speed < 1024) {
            text = speed + " B/s";
        } else if (speed >= 1024 && speed < (1024 * 1024)) {
            text = speed / 1024 + " KB/s";
        } else if (speed >= (1024 * 1024) && speed < (1024 * 1024 * 1024)) {
            text = speed / (1024 * 1024) + " MB/s";
        }
        return text;
    }

    public int getBufferedPercentage() {
        if (null == player) {
            return 0;
        }
        return player.getBufferedPercentage();
    }

    private void initFullscreenDialog() {
        mFullScreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (mExoPlayerFullscreen)
                    closeFullscreenDialog();
                super.onBackPressed();
            }
        };
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void openFullscreenDialog() {
        if (getCurrentChannel() == null) {
            return;
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        mFullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.fullscreen_close));
        mExoPlayerFullscreen = true;
        mFullScreenDialog.show();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void closeFullscreenDialog() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        ((FrameLayout) findViewById(R.id.media_frame)).addView(playerView);
        mExoPlayerFullscreen = false;
        mFullScreenDialog.dismiss();
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.fullscreen));
    }

    private void initFullscreenButton() {
        PlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
        mFullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
        mFullScreenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mExoPlayerFullscreen)
                    openFullscreenDialog();
                else
                    closeFullscreenDialog();
            }
        });
    }

    private void initExoPlayer() {

        player = new SimpleExoPlayer.Builder(this).build();

        playerView.setPlayer(player);
        playerView.setResizeMode(mCurrentAspectRatio);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        player.addListener(this);
        player.addAnalyticsListener(this);

        setAspectRatioText();

        if (null != mCurrentChannel) {
            MediaSource mVideoSource = buildMediaSource(Uri.parse(mCurrentChannel.url.get(mCurrentSourceIndex)));
            Log.i("DEBUG"," mVideoSource: " + mVideoSource);

            player.prepare(mVideoSource);
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (playerView == null) {
            playerView =  findViewById(R.id.video_view);
            initFullscreenDialog();
            initFullscreenButton();
        }

        initExoPlayer();

        if (mExoPlayerFullscreen) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
            mFullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.fullscreen_close));
            mFullScreenDialog.show();
        }
        else
        {
            mediaFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {

                    ViewGroup.LayoutParams layout = mediaFrame.getLayoutParams();
                    layout.height = (int)(mediaFrame.getWidth()*0.5625);

                    // open app in landscape mode
                    if (mainFrame.getHeight() < mainFrame.getWidth()) {
                        layout.height = layout.height/3;
                    }

                    mediaFrame.setLayoutParams(layout);
                    mediaFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    private MediaSource buildMediaSource(Uri uri) {

        @C.ContentType int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
//            case C.TYPE_HLS:
//                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
//            case C.TYPE_OTHER:
//                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerView != null && player != null) {
            mResumeWindow = player.getCurrentWindowIndex();
            player.release();
        }
        if (mFullScreenDialog != null)
            mFullScreenDialog.dismiss();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void setAspectRatioText() {
        if (mCurrentAspectRatio == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            aspectRatioTextOverlay.setText(getString(R.string.aspect_ratio_fit));
        }
        else {
            aspectRatioTextOverlay.setText(getString(R.string.aspect_ratio_fill));
        }
    }

    protected void setCurrentPlayInfo() {
        if (mCurrentChannel != null) {
            setCurrentPlayInfo(mCurrentChannel);
        }
    }

    protected void setCurrentPlayInfo(@NotNull ChannelData channel)
    {
        if (channel == null) {
            return;
        }

        if (controlOverlay.getVisibility() == View.INVISIBLE) {
            controlOverlay.setVisibility(View.VISIBLE);
        }

        mCurrentChannel = channel;
        mCurrentChannelIndex = mChannelList.indexOf(channel);

        String logoUrl = channel.logo;
        if (logoUrl == null || logoUrl.isEmpty())
        {
            Glide.with(this).clear(channelLogoBar);
            Glide.with(this).clear(channelLogoBall);
            channelLogoBar.setImageResource(R.drawable.tv_logo_trans);
            channelLogoBall.setImageResource(R.drawable.tv_logo_profile);
        }
        else
        {
            // Load the channel logo.
            Glide.with(this)
                    .asBitmap()
                    .placeholder(R.drawable.tv_logo_trans)
                    .error(R.drawable.tv_logo_trans)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(channelLogoBar);

            Glide.with(this)
                    .asBitmap()
                    .placeholder(R.drawable.tv_logo_trans)
                    .error(R.drawable.tv_logo_trans)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(channelLogoBall);
        }

        if (channel.countryCode != null && channel.countryCode.length() > 0 && !channel.countryCode.toLowerCase().equals("unsorted")) {
            String flagUrl = getFlagResourceByCountry(channel.countryCode.toLowerCase());
            if (flagUrl != null) {
                requestBuilder =
                        Glide.with(this)
                                .as(PictureDrawable.class)
                                //.placeholder(R.drawable.globe)
                                .error(R.drawable.globe)
                                .listener(new SvgSoftwareLayerSetter());

                requestBuilder.load(Uri.parse(flagUrl)).into(countryFlagBar);
            }
        }
        else {
            countryFlagBar.setImageResource(R.drawable.globe);
        }

        if (channel.isFavorite) {
            favIconBar.setImageResource(R.drawable.star);
            favIconOverlay.setImageResource(R.drawable.star_overlay);
            favIconMediaFrame.setImageResource(R.drawable.star_overlay);
        }
        else {
            favIconBar.setImageResource(R.drawable.star_outline);
            favIconOverlay.setImageResource(R.drawable.star_outline_overlay);
            favIconMediaFrame.setImageResource(R.drawable.star_outline_overlay);
        }

        if (channel != null) {

            String sChannelInfo = "";
            if (channel.countryName != null && channel.countryName.length() > 0 && !channel.countryName.trim().toLowerCase().equals("unsorted")) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += ", ";
                }
                sChannelInfo += channel.countryName;
            }
            if (channel.languageName != null && channel.languageName.length() > 0) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += ", ";
                }
                sChannelInfo += channel.languageName;
            }
            if (channel.category != null && channel.category.length() > 0) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += ", ";
                }
                sChannelInfo += channel.category;
            }

            channelInfoBar.setText(sChannelInfo);

            if (sChannelInfo.length() > 0) {
                channelInfoBar.setSelected(true);
                if (channelInfoBar.getVisibility() != View.VISIBLE) {
                    channelInfoBar.setVisibility(View.VISIBLE);
                }
            }
            else {
                channelInfoBar.setVisibility(View.GONE);
            }
        }


        channelNameOverlay.setText(channel.name);
        channelNameMediaFrame.setText(channel.name);
        channelNameBar.setText(channel.name);
        channelNameBar.setSelected(true);

        String sourceInfo = getCurrentSourceInfo();
        if (sourceInfo == null || sourceInfo.equals("1/1")) {
            sourceInfoBar.setText("");
            sourceInfoOverlay.setText("");
            sourceInfoMediaFrame.setText("");

            sourceInfoBar.setVisibility(View.GONE);
            sourceInfoOverlay.setVisibility(View.GONE);
            sourceInfoMediaFrame.setVisibility(View.GONE);
        }
        else {
            sourceInfoBar.setText(sourceInfo);
            sourceInfoOverlay.setText(sourceInfo);
            sourceInfoMediaFrame.setText(sourceInfo);

            if (isBuffering) {
                sourceInfoMediaFrame.setVisibility(View.VISIBLE);
            }
            else {
                sourceInfoOverlay.setVisibility(View.VISIBLE);
            }
            sourceInfoBar.setVisibility(View.VISIBLE);
        }
    }

    protected  void switchSource() {
        if (null == mCurrentChannel && mChannelList != null && mCurrentChannelIndex > 0 && mChannelList.size() > mCurrentChannelIndex) {
            mCurrentChannel = mChannelList.get(mCurrentChannelIndex);
        }
        if (null != mCurrentChannel ) {
            int index = 0;
            if (mCurrentSourceIndex + 1 < mCurrentChannel.url.size()) {
                index = mCurrentSourceIndex + 1;
            }
            play(mCurrentChannel, index);
        }
    }

    private String getSourceInfo(ChannelData channel, int source) {
        return source + 1 + "/" + channel.url.size();
    }

    public String getCurrentSourceInfo() {
        return getSourceInfo(mCurrentChannel, mCurrentSourceIndex);
    }

    public void clearCurrentChannel() {

        mCurrentChannel = null;
        mCurrentChannelIndex = 0;
        mCurrentSourceIndex = 0;
    }

    public ChannelData findChannelByName(String name) {
        ChannelData channel = null;
        for (Object s : mChannelList) {
            if (((ChannelData)s).name.equals(name)) {
                channel = (ChannelData)s;
                break;
            }
        }
        return channel;
    }

    private void switchToHome() {
        if (bottomNav.getSelectedItemId() != R.id.nav_home) {
            if (selectedFragment != null && selectedFragment.getClass() == ChannelsFragment.class) {
                ((ChannelsFragment) selectedFragment).clearFilter();
            }
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    public void setLastBarActiveTimeStamp() {
        this.lastBarActiveTimeStamp = System.currentTimeMillis();
    }

    private void showNowPlayingBar() {

        setLastBarActiveTimeStamp();
        if (mCurrentChannel != null) {
            if (nowPlayingBar.getVisibility() == View.INVISIBLE || nowPlayingBar.getVisibility() == View.GONE) {
                nowPlayingBar.startAnimation(slideInLeftBarAnim);
                nowPlayingBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideNowPlayingBar() {
        if (mCurrentChannel != null) {
            if (nowPlayingBar.getVisibility() == View.VISIBLE) {
                nowPlayingBar.startAnimation(slideOutLeftBarAnim);
                nowPlayingBar.setVisibility(View.GONE);
            }
        }
    }

    private void showNowPlayingBall(boolean slideIn) {
        if ((nowPlayingBall.getVisibility() == View.INVISIBLE || nowPlayingBall.getVisibility() == View.GONE)
                && (nowPlayingBar.getVisibility() == View.INVISIBLE || nowPlayingBar.getVisibility() == View.GONE)) {
            if (slideIn) {
                nowPlayingBall.startAnimation(slideInLeftAnim);
            }
            else {
                nowPlayingBall.startAnimation(fadeInAnim);
            }
            if (mCurrentChannel != null) {
                nowPlayingBall.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideNowPlayingBall() {
        if (mCurrentChannel != null) {
            if (nowPlayingBall.getVisibility() == View.VISIBLE) {
                nowPlayingBall.startAnimation(fadeOutAnim);
                nowPlayingBall.setVisibility(View.GONE);
            }
        }
    }

    protected void play(ChannelData channel, int source) {
        AppDatabase.getInstance(getApplicationContext()).channelDao().setLastSource(channel.name, source);
        mCurrentSourceIndex = source;
        setCurrentPlayInfo(channel);

        showNowPlayingBar();

        play(channel.url.get(source));
    }

    protected void play(ChannelData channel) {
        int source = AppDatabase.getInstance(getApplicationContext()).channelDao().getLastSource(channel.name);
        play(channel, source);
    }

    protected void play(String url) {
        Uri uri = Uri.parse(url);
        pausePlayer();

        // Prepare the player with the source.
        player.prepare(buildMediaSource(uri));
        player.setPlayWhenReady(true);
    }

    protected void stopPlayer() {
        clearCurrentChannel();
        player.stop(true);
        controlOverlay.setVisibility(View.INVISIBLE);

        if (bottomNav.getSelectedItemId() == R.id.nav_home) {
            ((HomeFragment)selectedFragment).reloadList();
        }
        if (bottomNav.getSelectedItemId() == R.id.nav_channels) {
            ((ChannelsFragment)selectedFragment).reloadList();
        }

        ((ViewGroup) playerView.getParent()).removeView(playerView);
        ((FrameLayout) findViewById(R.id.media_frame)).addView(playerView);
    }

    protected void pausePlayer() {
        if (player.isPlaying()) {
            player.stop();
        }
    }

    public String getFlagResourceByCountry(@NotNull String country) {
        String url = null;
        if (country != null && country.trim().length() > 0 && !country.trim().toLowerCase().equals("unsorted")) {

            if (country.trim().toLowerCase().equals("uk")) {
                country = "gb";
            }
            url = getResources().getString(R.string.country_flags_url) + country.trim() + getResources().getString(R.string.country_flags_file_extension);
        }
        return url;
    }

    public boolean removeAllChannels() {
        try {
            AppDatabase.getInstance(getApplicationContext()).channelDao().removeAll();
            mChannelList.clear();
            gTotalChannelCount = 0;
        }
        catch (Exception e) {
            Toast toast= Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }
        return true;
    }

    public void loadChannelList(String url) {
        if (!isLoading) {
            loadingDialog.startLoadingDialog();
            new LoadIptvListThread(url).start();
        }
    }

    public void loadJsonChannelList(String url) {
        if (!isLoading) {
            loadingDialog.startLoadingDialog();
            new LoadIptvJsonListThread(url).start();
        }
    }

    public void loadM3UChannelList(String url) {
        if (!isLoading) {
            loadingDialog.startLoadingDialog();
            new LoadM3UListThread(url).start();
        }
    }

    private void initStationListView() {

        Log.d(TAG, "initStationListView: ");

        if (null == mChannelList || mChannelList.size() < 1) {
            Toast toast= Toast.makeText(getApplicationContext(), "         Unable to load the station list.\nPlease check your network connection.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        else {
            gTotalChannelCount = mChannelList.size();
            if (bottomNav.getSelectedItemId() != R.id.nav_channels) {
                try {
                    bottomNav.setSelectedItemId(R.id.nav_channels);
                }
                catch (Exception e) {
                    Log.e(TAG, "initStationListView: bottomNav.setSelectedItemId: " + e.getMessage());
                }
            }
        }
    }

    private void getBufferingInfo() {
        if (isBuffering) {
            int percent = getBufferedPercentage();
            String bufferingInfo = "" + percent + "%";
            bufferPercentageMediaFrame.setText(bufferingInfo);
            Log.i(TAG, bufferingInfo);
        }
        if (mCurrentChannel != null && mCurrentChannel.name != null && mCurrentChannel.name.length() > 0)
        {
            String netSpeed = getNetSpeedText(getNetSpeed());
            netSpeedMediaFrame.setText(netSpeed);
            netSpeedOverlay.setText(netSpeed);
            netSpeedBar.setText(netSpeed);
            netSpeedBall.setText(netSpeed);

            Log.i(TAG, netSpeed);
        }
    }

    private void startCheckingThread() {
        if (!isCheckingThreadRunning && (checkingThread == null || !checkingThread.isAlive())) {
            isCheckingThreadRunning = true;
            checkingThread = new Thread(checkingRunnable);
            checkingThread.start();
        }
    }

    private void stopCheckingThread() {
        isCheckingThreadRunning = false;
    }

    Runnable checkingRunnable = new Runnable() {
        @Override
        public void run() {
            while (isCheckingThreadRunning) {
                try {
                    long nowTimeStamp = System.currentTimeMillis();
                    long timeDiff = (nowTimeStamp - lastBarActiveTimeStamp);
                    if (timeDiff > NOW_PLAYING_BAR_FADING_TIME) {
                        mHandler.sendEmptyMessage(MSG_HIDE_NOW_PLAYING_BAR);
                    }
                    else {
                        mHandler.sendEmptyMessage(MSG_GET_BUFFERING_INFO);
                    }
                    Thread.sleep(NET_SPEED_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public class LoadIptvListThread extends Thread {

        private final String channelListUrl;

        LoadIptvListThread(String url) {
            this.channelListUrl = url;
        }

        @Override
        public void run() {

            if (channelListUrl == null || channelListUrl.length() == 0) {
                return;
            }

            if (mChannelList != null && mChannelList.size() > 0) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please remove all channels before loading sample channels.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }

            isLoading = true;

            String jsonString = getJsonString(channelListUrl);
            if (null != jsonString && (mChannelList == null || mChannelList.size() == 0))
            {
                List<IptvStation> iptvStationList = JSON.parseArray(jsonString, IptvStation.class);
                Log.d(TAG,  iptvStationList.size() +" stations loaded from server.");

                ArrayList channelList = new ArrayList<ChannelData>();
                for (IptvStation iptvStation: iptvStationList) {

                    List<String> urlList = new ArrayList<String>();
                    String logo = "";
                    for (Object s : channelList) {
                        if (((ChannelData)s).name.equals(iptvStation.name)
                                ||((ChannelData)s).name.equals(iptvStation.tvg.name)
                        ) {
                            if (((ChannelData)s).logo != null && ((ChannelData)s).logo.length() > 0) {
                                logo = ((ChannelData)s).logo;
                            }

                            urlList = ((ChannelData)s).url;
                            channelList.remove(s);
                            break;
                        }
                    }
                    ChannelData channel = new ChannelData();
                    channel.name = (iptvStation.tvg.name != null && !iptvStation.tvg.name.isEmpty()) ? iptvStation.tvg.name : iptvStation.name;
                    if (iptvStation.logo != null && iptvStation.logo.length() > 0) {
                        channel.logo = iptvStation.logo;
                    }
                    else {
                        channel.logo = logo;
                    }
                    channel.countryCode = iptvStation.country != null ? iptvStation.country.code : "";
                    channel.countryName = iptvStation.country != null ? iptvStation.country.name : "";
                    channel.languageName = (iptvStation.language != null && iptvStation.language.size() > 0) ? iptvStation.language.get(0).name : "";
                    channel.category = iptvStation.category;
                    urlList.add(iptvStation.url);
                    channel.url = new ArrayList<String>();
                    if (urlList != null) {
                        channel.url.addAll(urlList);
                    }
                    channelList.add(channel);
                }
                mChannelList = channelList;
                if (mChannelList != null)
                {
                    if (mChannelList.size() > 0) {
                        AppDatabase.getInstance(getApplicationContext()).channelDao().insertAll(channelList);
                        // Send Message to Main thread to load the station list
                        mHandler.sendEmptyMessage(MSG_LOAD_LIST);
                    }

                    Log.d(TAG,  mChannelList.size() +" channels loaded from server.");

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadingDialog.dismissDialog();
                            Toast toast = Toast.makeText(getApplicationContext(), mChannelList.size() +" channels added.", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }
            }
            isLoading = false;
        }

        // Given a string representation of a URL, sets up a connection and gets an input stream.
        private InputStream downloadUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(9000 /* milliseconds */);
            conn.setConnectTimeout(6000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            return conn.getInputStream();
        }

        public String inputStream2Str(InputStream is) throws IOException {
            StringBuffer sb;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));

                sb = new StringBuffer();

                String data;
                while ((data = br.readLine()) != null) {
                    sb.append(data);
                }
            } finally {
                br.close();
            }

            return sb.toString();
        }

        private String getJsonString(String url) {
            String jsonData = null;
            try {
                InputStream stream = null;

                try {
                    stream = downloadUrl(url);
                    if (url.endsWith(".gz"))
                    {
                        stream = new GZIPInputStream(stream);
                    }
                    jsonData = inputStream2Str(stream);
                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }

                Log.d(TAG, "jsonData: " + jsonData);
                return jsonData;
            } catch (Exception e) {
                e.printStackTrace();

                final String errorMsg = "Exception when loading channels: " + e.getMessage();
                Log.e(TAG, errorMsg);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });

                return getJsonStringBackup(getResources().getString(R.string.tv_channel_list_backup));
            }
        }

        @Nullable
        private String getJsonStringBackup(String url) {
            String jsonData = null;
            try {
                InputStream stream = null;

                try {
                    stream = downloadUrl(url);
                    if (url.endsWith(".gz"))
                    {
                        stream = new GZIPInputStream(stream);
                    }
                    jsonData = inputStream2Str(stream);
                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }

                Log.d(TAG, "jsonData: " + jsonData);
                return jsonData;
            } catch (Exception e) {
                isLoading = false;
                e.printStackTrace();
                final String errorMsg = "Exception when loading channels: " + e.getMessage();
                Log.e(TAG, errorMsg);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }
            return null;
        }
    }

    public class LoadIptvJsonListThread extends Thread {

        private final String channelListUrl;

        LoadIptvJsonListThread(String url) {
            this.channelListUrl = url;
        }

        @Override
        public void run() {

            if (channelListUrl == null || channelListUrl.length() == 0) {
                return;
            }

            if (mChannelList != null && mChannelList.size() > 0) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please remove all channels before loading default channels.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }

            isLoading = true;

            String jsonString = getJsonString(channelListUrl);
            if (null != jsonString && (mChannelList == null || mChannelList.size() == 0))
            {
                List<IptvStation> iptvStationList = JSON.parseArray(jsonString, IptvStation.class);
                Log.d(TAG,  iptvStationList.size() +" stations loaded from server.");

                ArrayList channelList = new ArrayList<ChannelData>();
                for (IptvStation iptvStation: iptvStationList) {

                    List<String> urlList = new ArrayList<String>();
                    String logo = "";
                    for (Object s : channelList) {
                        if (((ChannelData)s).name.equals(iptvStation.name)
                            ||((ChannelData)s).name.equals(iptvStation.tvg.name)
                        ) {
                            urlList = ((ChannelData)s).url;
                            channelList.remove(s);
                            break;
                        }
                    }
                    ChannelData channel = new ChannelData();
                    channel.name = (iptvStation.tvg.name != null && !iptvStation.tvg.name.isEmpty()) ? iptvStation.tvg.name : iptvStation.name;
                    channel.logo = iptvStation.logo;
                    channel.countryCode = iptvStation.country != null ? iptvStation.country.code : "";
                    channel.countryName = iptvStation.country != null ? iptvStation.country.name : "";
                    channel.languageName = (iptvStation.language != null && iptvStation.language.size() > 0) ? iptvStation.language.get(0).name : "";
                    channel.category = iptvStation.category;
                    urlList.add(iptvStation.url);
                    channel.url = new ArrayList<String>();
                    if (urlList != null) {
                        channel.url.addAll(urlList);
                    }
                    channelList.add(channel);
                }

                mChannelList = channelList;

                if (mChannelList != null)
                {
                    if (mChannelList.size() > 0) {
                        AppDatabase.getInstance(getApplicationContext()).channelDao().insertAll(channelList);
                        // Send Message to Main thread to load the station list
                        mHandler.sendEmptyMessage(MSG_LOAD_LIST);
                    }

                    Log.d(TAG,  mChannelList.size() +" channels loaded from server.");

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadingDialog.dismissDialog();
                            Toast toast = Toast.makeText(getApplicationContext(), mChannelList.size() +" channels added.", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }
            }

            isLoading = false;
        }

        // Given a string representation of a URL, sets up a connection and gets an input stream.
        private InputStream downloadUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(9000 /* milliseconds */);
            conn.setConnectTimeout(9000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            return conn.getInputStream();
        }

        public String inputStream2Str(InputStream is) throws IOException {
            StringBuffer sb;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));

                sb = new StringBuffer();

                String data;
                while ((data = br.readLine()) != null) {
                    sb.append(data);
                }
            } finally {
                br.close();
            }

            return sb.toString();
        }

        private String getJsonString(String url) {
            String jsonData = null;
            try {
                InputStream stream = null;

                try {
                    stream = downloadUrl(url);
                    if (url.endsWith(".gz"))
                    {
                        stream = new GZIPInputStream(stream);
                    }
                    jsonData = inputStream2Str(stream);
                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }

                Log.d(TAG, "jsonData: " + jsonData);
                return jsonData;
            } catch (Exception e) {
                e.printStackTrace();

                final String errorMsg = "Exception when loading channels: " + e.getMessage();
                Log.e(TAG, errorMsg);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }
            return null;
        }
    }

    public class LoadM3UListThread extends Thread {

        private final String channelListUrl;

        LoadM3UListThread(String url) {
            this.channelListUrl = url;
        }

        @Override
        public void run() {
            if (channelListUrl == null || channelListUrl.length() < 1) {
                return;
            }

            isLoading = true;
            String m3UString = getM3UString(channelListUrl);
            if (null != m3UString && mChannelList != null )
            {
                try {
                    ArrayList channelList = new ArrayList<ChannelData>();
                    List<SimpleM3UParser.M3U_Entry> m3UStationList = new SimpleM3UParser().parseM3UString(m3UString);
                    for (SimpleM3UParser.M3U_Entry stationM3U : m3UStationList) {

                        List<String> urlList = new ArrayList<String>();
                        String logo = "";
                        for (Object s : mChannelList) {
                            if (s != null && ((ChannelData)s).name != null && ((ChannelData)s).name.equals(stationM3U.getName())) {
                                urlList = ((ChannelData)s).url;
                                if (((ChannelData)s).logo != null && ((ChannelData)s).logo.contains("http")) {
                                    logo = ((ChannelData)s).logo;
                                }
                                mChannelList.remove(s);
                                break;
                            }
                        }
                        ChannelData channel = new ChannelData();
                        channel.name = stationM3U.getName();
                        if (logo.contains("http")) {
                            channel.logo = logo;
                        }
                        else {
                            channel.logo = stationM3U.getTvgLogo();
                        }
                        urlList.add(stationM3U.getUrl());
                        channel.url = new ArrayList<String>();
                        if (urlList != null) {
                            channel.url.addAll(urlList);
                        }
                        channelList.add(channel);

                    }
                    mChannelList = channelList;
                    AppDatabase.getInstance(getApplicationContext()).channelDao().insertAll(channelList);

                } catch (IOException e) {
                    e.printStackTrace();
                    isLoading = false;
                }
            }
            if (mChannelList != null)
            {
                if (mChannelList.size() > 0) {
                    // Send Message to Main thread to load the station list
                    mHandler.sendEmptyMessage(MSG_LOAD_LIST);
                }

                Log.d(TAG,  mChannelList.size() +" channels loaded from server.");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismissDialog();
                        Toast toast = Toast.makeText(getApplicationContext(), mChannelList.size() +" channels added.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }
            isLoading = false;
        }

        @Nullable
        private String getM3UString(String url) {
            try {
                OkHttpClient client = new OkHttpClient();

                Request.Builder requestBuilder = new Request.Builder();
//                requestBuilder.header("Accept-Encoding", "gzip");
                Request request = requestBuilder.url(url).build();
                Response responses = client.newCall(request).execute();
                assert responses.body() != null;
                String data = responses.body().string();
                Log.d(TAG, "getM3UString: [" + data + "]");

                return data;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception when loading station list: " + e.getMessage());
                isLoading = false;
            }
            return null;
        }

        @Nullable
        private String getJsonString(String url) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response responses = client.newCall(request).execute();
                assert responses.body() != null;
                String jsonData = responses.body().string();
                Log.d(TAG, "getJsonString: [" + jsonData + "]");

                return jsonData;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception when loading station list: " + e.getMessage());
            }
            return null;
        }
    }

    public static class MsgHandler extends Handler {

        WeakReference<MainActivity> mMainActivityWeakReference;

        MsgHandler(MainActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            super.handleMessage(msg);

            //Log.d(TAG, "Handler: msg.what = " + msg.what);

            MainActivity mainActivity = mMainActivityWeakReference.get();

            if (msg.what == MSG_LOAD_LIST) {
                mainActivity.initStationListView();
            }
            else if (msg.what == MSG_PLAY) {
                ChannelData channel = (ChannelData) msg.obj;
                if (channel != null) {
                    mainActivity.setCurrentPlayInfo(channel);
                    mainActivity.play(channel);
                }
            }
            else if (msg.what == MSG_GET_BUFFERING_INFO) {
                mainActivity.getBufferingInfo();
            }
            else if (msg.what == MSG_HIDE_NOW_PLAYING_BAR) {
                mainActivity.hideNowPlayingBar();
                if (mainActivity.getCurrentChannel() != null) {
                    mainActivity.showNowPlayingBall(false);
                    mainActivity.getBufferingInfo();
                }
            }
        }
    }
}
