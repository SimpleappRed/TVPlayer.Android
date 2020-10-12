package com.cy8018.tvplayer.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.AppDatabase;
import com.cy8018.tvplayer.db.ChannelData;
import com.cy8018.tvplayer.model.IptvStation;
import com.cy8018.tvplayer.util.SimpleM3UParser;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
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
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements Player.EventListener, AnalyticsListener {

    public boolean isLoading = false;

    private static final String TAG = "MainActivity";

    private MessageListener mMessageListener;

    // ServerPrefix address
    public static final String ServerPrefix = "https://gitee.com/cy8018/Resources/raw/master/tv/";
    public static final String ServerPrefixAlternative = "https://raw.githubusercontent.com/cy8018/Resources/master/tv/";

    public static String CurrentServerPrefix = "https://gitee.com/cy8018/Resources/raw/master/tv/";

    // channel list
    public static List<ChannelData> mChannelList;

//    public static List<ChannelData> mChannelListFull;

    private ChannelData mCurrentChannel;

    private int mCurrentChannelIndex;

    private int mCurrentSourceIndex;

    public final MsgHandler mHandler = new MsgHandler(this);

    // message to load the station list
    public static final int MSG_LOAD_LIST = 0;

    // message to play the radio
    public static final int MSG_PLAY = 1;

    // message to get buffering info
    public static final int MSG_GET_BUFFERING_INFO = 2;

    private final String STATE_RESUME_WINDOW = "resumeWindow";
    private final String STATE_RESUME_POSITION = "resumePosition";
    private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";

    private int mResumeWindow;
    private long mResumePosition;
    private boolean mExoPlayerFullscreen = false;

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ImageView mFullScreenIcon;
    private Dialog mFullScreenDialog;
    private DataSource.Factory dataSourceFactory;
    private RetainedFragment dataFragment;

    private TextView textCurrentChannelSource, textBufferingInfo, textNetSpeed, textSourceInfoOverlay, textChannelNameOverlay, textCurrentChannelName;
    private TextView channelInfo;
    private ImageView countryFlag, isFavorite;
    protected GifImageView imageLoading;
    protected ImageView imageCurrentChannelLogo;
    private BottomNavigationView bottomNav;
    public Fragment selectedFragment;
    private View appTitleBar, nowPlayingBarHome;

    private int nowPlayingBarHomeHeight = 0;

    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    protected boolean isBuffering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onCreate: ");

        // Produces DataSource instances through which media data is loaded.
//        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)));

        dataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(this, getString(R.string.app_name)),
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS * 2,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS * 2,
                true);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setSelectedItemId(R.id.nav_home);

        appTitleBar = findViewById(R.id.appTitleBar);
        nowPlayingBarHome = findViewById(R.id.nowPlayingBarHome);
        ViewGroup.LayoutParams nowPlayingBarHomeLayout = nowPlayingBarHome.getLayoutParams();
        nowPlayingBarHomeHeight = nowPlayingBarHomeLayout.height;
        nowPlayingBarHomeLayout.height = 1;
        nowPlayingBarHome.setLayoutParams(nowPlayingBarHomeLayout);
        nowPlayingBarHome.setVisibility(View.INVISIBLE);

        countryFlag = findViewById(R.id.imageCountryFlag);
        channelInfo = findViewById(R.id.textChannelInfo);
        isFavorite = findViewById(R.id.favorite_icon);

        textCurrentChannelName = findViewById(R.id.textCurrentChannelName);
        textChannelNameOverlay = findViewById(R.id.channel_name);
        textSourceInfoOverlay = findViewById(R.id.source_info);
        imageCurrentChannelLogo = findViewById(R.id.imageCurrentChannelLogo);
        textCurrentChannelSource = findViewById(R.id.textCurrentStationSource);
        textBufferingInfo = findViewById(R.id.textBufferingInfo);
        textNetSpeed = findViewById(R.id.net_speed);
        imageLoading = findViewById(R.id.imageLoading);
        imageLoading.setImageResource(getResources().getIdentifier("@drawable/loading_pin", null, getPackageName()));
        imageLoading.setVisibility(View.INVISIBLE);
        textChannelNameOverlay.setSelected(true);

        isFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentChannel.isFavorite) {
                    removeFromFavorites(mCurrentChannel.name);
                    isFavorite.setImageResource(getResources().getIdentifier("@drawable/ic_star_outline", null, getPackageName()));
                } else {
                    addToFavorites(mCurrentChannel.name);
                    isFavorite.setImageResource(getResources().getIdentifier("@drawable/ic_star", null, getPackageName()));
                }

                if (bottomNav.getSelectedItemId() == R.id.nav_home) {
                    ((HomeFragment)selectedFragment).reloadList();
                }
            }
        });

        nowPlayingBarHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            }
        });

        textCurrentChannelSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSource();
            }
        });

        textSourceInfoOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSource();
            }
        });

        if (savedInstanceState != null) {
            mResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
            mResumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION);
            mExoPlayerFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
        }

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
            if (mChannelList != null) {
                mCurrentChannel = mChannelList.get(mCurrentChannelIndex);
            }

            setCurrentPlayInfo(mCurrentChannel);
            textCurrentChannelSource.setText(getSourceInfo(mCurrentChannel, mCurrentSourceIndex));
            textSourceInfoOverlay.setText(getSourceInfo(mCurrentChannel, mCurrentSourceIndex));
        }

        if (mChannelList == null || mChannelList.size() == 0) {
            mChannelList = AppDatabase.getInstance(getApplicationContext()).channelDao().getAll();
        }
        new Thread(networkCheckRunnable).start();
    }

    public List<ChannelData> getChannelList() {
        return mChannelList;
    }

    public void addToFavorites(String channelName) {
        AppDatabase.getInstance(getApplicationContext()).channelDao().addToFavorites(channelName);
        for (ChannelData channel: mChannelList) {
            if (channel.name.equals(channelName)) {
                mChannelList.get(mChannelList.indexOf(channel)).isFavorite = true;
                break;
            }
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

    public ChannelData getCurrentChannel() {
        return mCurrentChannel;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    View frameView = findViewById(R.id.media_frame);
                    ViewGroup.LayoutParams frameLayout = frameView.getLayoutParams();

                    switch (item.getItemId()) {
                        case R.id.nav_home:
                            selectedFragment = new HomeFragment();
                            frameLayout.height = (int)(frameView.getWidth()*0.5625);
                            frameView.setLayoutParams(frameLayout);
                            frameView.setVisibility(View.VISIBLE);

                            break;
//                        case R.id.nav_fav:
//                            selectedFragment = new FavoritesFragment();
//                            frameLayout.height = (int)(frameView.getWidth()*0.5625)/2;
//                            frameView.setLayoutParams(frameLayout);
//                            frameView.setVisibility(View.VISIBLE);
//
//                            break;
                        case R.id.nav_channels:
                            selectedFragment = new ChannelsFragment();
                            frameLayout.height = 1;
                            frameView.setLayoutParams(frameLayout);
                            frameView.setVisibility(View.INVISIBLE);
                            break;
                        case R.id.nav_setting:
                            selectedFragment = new SettingsFragment();
                            frameLayout.height = 1;
                            frameView.setLayoutParams(frameLayout);
                            frameView.setVisibility(View.INVISIBLE);
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                    return true;
                }
            };

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_main, menu);
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//        SearchView searchView = (SearchView) searchItem.getActionView();
//
//        searchView.setOnQueryTextListener(
//                new SearchView.OnQueryTextListener() {
//                  @Override
//                  public boolean onQueryTextSubmit(String query) {
//                      return false;
//                  }
//
//                  @Override
//                  public boolean onQueryTextChange(String newText) {
//                      adapter.getFilter().filter(newText);
//                      return false;
//                  }
//              }
//        );
//
//        searchView.setOnSearchClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //search is expanded
//                appLogo.setVisibility(View.INVISIBLE);
//                appTitle.setVisibility(View.INVISIBLE);
//
//                View frameView = findViewById(R.id.media_frame);
//                ViewGroup.LayoutParams layout = frameView.getLayoutParams();
//                layout.height = (int)(frameView.getWidth()*0.5625)/2;
//                frameView.setLayoutParams(layout);
//
//            }
//        });
//
//        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
//            @Override
//            public boolean onClose() {
//                // searchview closed
//                appLogo.setVisibility(View.VISIBLE);
//                appTitle.setVisibility(View.VISIBLE);
//
//                View frameView = findViewById(R.id.media_frame);
//                ViewGroup.LayoutParams layout = frameView.getLayoutParams();
//                layout.height = (int)(frameView.getWidth()*0.5625);
//                frameView.setLayoutParams(layout);
//
//                return false;
//            }
//        });
//
//        return true;
//    }

    @Override
    protected void onDestroy() {
        PlayerFragmentData fragmentData = new PlayerFragmentData();
        fragmentData.setChannelList(mChannelList);
//        fragmentData.setChannelListFull(mChannelListFull);
        fragmentData.setCurrentChannelIndex(mCurrentChannelIndex);
        fragmentData.setCurrentSourceIndex(mCurrentSourceIndex);

        // store the data in the fragment
        dataFragment.setData(fragmentData);

        releasePlayer();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_RESUME_WINDOW, mResumeWindow);
        outState.putLong(STATE_RESUME_POSITION, mResumePosition);
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, mExoPlayerFullscreen);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "onPlayerStateChanged: playWhenReady:"+ playWhenReady + " playbackState:" + playbackState);
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                showBufferingInfo();
                break;
            case Player.STATE_ENDED:
                hideBufferingInfo();
                break;
            case Player.STATE_READY:
                hideBufferingInfo();
                break;
            case Player.STATE_IDLE:
            default:
                hideBufferingInfo();
                break;
        }
    }

    private  void hideBufferingInfo () {
        imageLoading.setVisibility(View.INVISIBLE);
        isBuffering = false;
        textBufferingInfo.setText("");
    }

    private void showBufferingInfo () {
        isBuffering = true;
        imageLoading.setVisibility(View.VISIBLE);
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
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_skrink));
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
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_expand));
    }

    private void initFullscreenButton() {
        PlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
        mFullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
        FrameLayout mFullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
        mFullScreenButton.setOnClickListener(new View.OnClickListener() {
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

        if (null == player) {
            player = new SimpleExoPlayer.Builder(this).build();
        }

        playerView.setPlayer(player);
        //playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        player.addListener(this);
        player.addAnalyticsListener(this);

        boolean haveResumePosition = mResumeWindow != C.INDEX_UNSET;
        if (haveResumePosition) {
            Log.i("DEBUG"," haveResumePosition ");
            player.seekTo(mResumeWindow, mResumePosition);
        }

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
            mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_skrink));
            mFullScreenDialog.show();
        }
        else
        {
            findViewById(R.id.media_frame).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {
                    View frameView = findViewById(R.id.media_frame);
                    ViewGroup.LayoutParams layout = frameView.getLayoutParams();
                    layout.height = (int)(frameView.getWidth()*0.5625);
                    frameView.setLayoutParams(layout);
                    frameView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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
            mResumePosition = Math.max(0, player.getContentPosition());
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

    protected void setCurrentPlayInfo(@NotNull ChannelData channel)
    {
        String logoUrl = channel.logo;
        if (logoUrl == null || logoUrl.isEmpty())
        {
            imageCurrentChannelLogo.setImageResource(getResources().getIdentifier("@drawable/tv_transparent", null, getPackageName()));
        }
        else
        {
            if (!logoUrl.toLowerCase().contains("http"))
            {
                logoUrl = CurrentServerPrefix + "logo/" + logoUrl;
            }
            // Load the station logo.
            Glide.with(this)
                    .asBitmap()
                    .timeout(10000)
                    .load(logoUrl)
                    .into(imageCurrentChannelLogo);
        }

        if (channel.countryCode != null && channel.countryCode.length() > 0) {
            countryFlag.setImageResource(getResources().getIdentifier("@drawable/"+ channel.countryCode, null, getPackageName()));
        }

        if (channel.isFavorite) {
            isFavorite.setImageResource(getResources().getIdentifier("@drawable/ic_star", null, getPackageName()));
        }
        else {
            isFavorite.setImageResource(getResources().getIdentifier("@drawable/ic_star_outline", null, getPackageName()));
        }

        if (channel != null) {

            String sChannelInfo = "";
            if (channel.countryName != null && channel.countryName.length() > 0) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += " / ";
                }
                sChannelInfo += channel.countryName;
            }
            if (channel.languageName != null && channel.languageName.length() > 0) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += " / ";
                }
                sChannelInfo += channel.languageName;
            }
            if (channel.category != null && channel.category.length() > 0) {
                if (sChannelInfo.length() > 0) {
                    sChannelInfo += " / ";
                }
                sChannelInfo += channel.category;
            }


            channelInfo.setText(sChannelInfo);
        }

        textChannelNameOverlay.setText(channel.name);
        textCurrentChannelName.setText(channel.name);
        textCurrentChannelName.setSelected(true);
    }

    protected  void switchSource() {
        if (null == mCurrentChannel) {
            mCurrentChannel = mChannelList.get(mCurrentChannelIndex);
        }
        int index = 0;
        if (mCurrentSourceIndex + 1 < mCurrentChannel.url.size()) {
            index = mCurrentSourceIndex + 1;
        }
        play(mCurrentChannel, index);
    }

    private String getSourceInfo(ChannelData channel, int source) {
        return source + 1 + "/" + channel.url.size();
    }

    public String getCurrentSourceInfo() {
        return getSourceInfo(mCurrentChannel, mCurrentSourceIndex);
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

    protected void play(ChannelData channel, int source) {
        mCurrentChannel = channel;
        mCurrentChannelIndex = mChannelList.indexOf(channel);
        mCurrentSourceIndex = source;
        setCurrentPlayInfo(channel);
        textCurrentChannelSource.setText(getSourceInfo(channel, source));
        textSourceInfoOverlay.setText(getSourceInfo(mCurrentChannel, mCurrentSourceIndex));

        if (appTitleBar.getVisibility() == View.VISIBLE) {
            ViewGroup.LayoutParams layout = appTitleBar.getLayoutParams();
            layout.height = 1;
            appTitleBar.setLayoutParams(layout);
            appTitleBar.setVisibility(View.INVISIBLE);
        }

        if (nowPlayingBarHome.getVisibility() == View.INVISIBLE) {
            ViewGroup.LayoutParams nowPlayingBarHomeLayout = nowPlayingBarHome.getLayoutParams();
            nowPlayingBarHomeLayout.height = nowPlayingBarHomeHeight;
            nowPlayingBarHome.setLayoutParams(nowPlayingBarHomeLayout);
            nowPlayingBarHome.setVisibility(View.VISIBLE);
        }

        if (bottomNav.getSelectedItemId() != R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
//        else {
//            ((HomeFragment) selectedFragment).loadData();
//        }

        play(channel.url.get(source));
    }

    protected void play(String url) {
        Uri uri = Uri.parse(url);
        if (player.isPlaying()) {
            player.stop();
        }

        // Prepare the player with the source.
        player.prepare(buildMediaSource(uri));
        player.setPlayWhenReady(true);
    }

    public boolean removeAllChannels() {
        try {
            AppDatabase.getInstance(getApplicationContext()).channelDao().removeAll();
            mChannelList.clear();
        }
        catch (Exception e) {
            Toast toast= Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }
        return true;
    }

    public void loadChannelList() {
        if (!isLoading) {
            new LoadIptvListThread().start();
        }
    }

    public void loadM3UChannelList(String url) {
        if (!isLoading) {
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
            if (bottomNav.getSelectedItemId() != R.id.nav_channels) {
                bottomNav.setSelectedItemId(R.id.nav_channels);
            }
        }
    }

    private void getBufferingInfo() {
        if (isBuffering) {
            int percent = getBufferedPercentage();
            String bufferingInfo = "" + percent + "%";
            textBufferingInfo.setText(bufferingInfo);
            Log.i(TAG, bufferingInfo);
        }
        if (mCurrentChannel != null && mCurrentChannel.name != null && mCurrentChannel.name.length() > 0)
        {
            String netSpeed = getNetSpeedText(getNetSpeed());
            textNetSpeed.setText(netSpeed);
            Log.i(TAG, netSpeed);
        }
    }

    Runnable networkCheckRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    mHandler.sendEmptyMessage(MSG_GET_BUFFERING_INFO);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public class LoadListThread extends Thread {

        private String serverPrefix;

        LoadListThread(String serverPrefix) {
            this.serverPrefix = serverPrefix;
        }

        @Override
        public void run() {

            if (serverPrefix == null || serverPrefix.length() < 1) {
                return;
            }

            String jsonString = getJsonString(serverPrefix + getResources().getString(R.string.station_list_file_name));
            if (null != jsonString && (mChannelList == null || mChannelList.size() == 0))
            {
                CurrentServerPrefix = serverPrefix;
                JSONObject object = JSON.parseObject(jsonString);
                Object objArray = object.get("stations");
                String str = objArray+"";
                mChannelList = (ArrayList<ChannelData>) JSON.parseArray(str, ChannelData.class);
                Log.d(TAG,  mChannelList.size() +" stations loaded from server.");

                // Send Message to Main thread to load the station list
                mHandler.sendEmptyMessage(MSG_LOAD_LIST);
            }
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

    public class LoadIptvListThread extends Thread {

        @Override
        public void run() {

            isLoading = true;

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

            String jsonString = getJsonString(getResources().getString(R.string.iptv_station_list));
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
                AppDatabase.getInstance(getApplicationContext()).channelDao().insertAll(channelList);

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

        @Nullable
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
                Log.e(TAG, "Exception when loading station list: " + e.getMessage());
            }
            return null;
        }
    }

    public class LoadM3UListThread extends Thread {

        private String channelListUrl;

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

            Log.d(TAG, "Handler: msg.what = " + msg.what);

            MainActivity mainActivity = mMainActivityWeakReference.get();

            if (msg.what == MSG_LOAD_LIST) {
                mainActivity.initStationListView();
            }
            else if (msg.what == MSG_PLAY) {
                ChannelData channel = (ChannelData) msg.obj;
                if (channel != null) {
                    mainActivity.setCurrentPlayInfo(channel);
                    mainActivity.play(channel, 0);
                }
            }
            else if (msg.what == MSG_GET_BUFFERING_INFO) {
                mainActivity.getBufferingInfo();
            }
        }
    }
}
