package com.cy8018.tvplayer.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.cy8018.tvplayer.model.IptvStation;
import com.cy8018.tvplayer.util.SimpleM3UParser;
import com.cy8018.tvplayer.model.Station;
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


import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements Player.EventListener, AnalyticsListener {

    private static final String TAG = "MainActivity";

    // ServerPrefix address
    public static final String ServerPrefix = "https://gitee.com/cy8018/Resources/raw/master/tv/";
    public static final String ServerPrefixAlternative = "https://raw.githubusercontent.com/cy8018/Resources/master/tv/";

    public static String CurrentServerPrefix = "https://gitee.com/cy8018/Resources/raw/master/tv/";

    // station list
    protected List<Station> mStationList;

    private StationListAdapter adapter;

    private Station mCurrentStation;

    private int mCurrentStationIndex;

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

    private TextView appTitle, textCurrentStationName, textCurrentStationSource, textBufferingInfo, textNetSpeed, textSourceInfoOverlay, textChannelNameOverlay;
    private RecyclerView stationListView;
    protected GifImageView imageLoading;
    protected ImageView imageCurrentStationLogo, appLogo;

    private SearchView searchView;

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
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // toolbar fancy stuff
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.search_action);

//        searchView.findViewById(R.id.action_search);
//        searchView.setIconified(false);


        stationListView = findViewById(R.id.stationRecyclerView);
        appTitle = findViewById(R.id.app_title);
        textCurrentStationName = findViewById(R.id.textCurrentStationName);
        textChannelNameOverlay = findViewById(R.id.channel_name);
        textSourceInfoOverlay = findViewById(R.id.source_info);
        imageCurrentStationLogo = findViewById(R.id.imageCurrentStationLogo);
        textCurrentStationSource = findViewById(R.id.textCurrentStationSource);
        textBufferingInfo = findViewById(R.id.textBufferingInfo);
        textNetSpeed = findViewById(R.id.net_speed);
        appLogo = findViewById(R.id.app_logo);
        imageLoading = findViewById(R.id.imageLoading);
        imageLoading.setImageResource(getResources().getIdentifier("@drawable/loading_pin", null, getPackageName()));
        imageLoading.setVisibility(View.INVISIBLE);


        textChannelNameOverlay.setSelected(true);

        textCurrentStationSource.setOnClickListener(new View.OnClickListener() {
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
            mStationList = data.getStationList();
            mCurrentSourceIndex = data.getCurrentSourceIndex();
            mCurrentStationIndex = data.getCurrentStationIndex();

            mCurrentStation = mStationList.get(mCurrentStationIndex);

            setCurrentPlayInfo(mCurrentStation);
            textCurrentStationSource.setText(getSourceInfo(mCurrentStation, mCurrentSourceIndex));
            textSourceInfoOverlay.setText(getSourceInfo(mCurrentStation, mCurrentSourceIndex));

            initStationListView();
            Objects.requireNonNull(stationListView.getLayoutManager()).smoothScrollToPosition(stationListView, null, mCurrentStationIndex);
        }

        if (null == mStationList || mStationList.isEmpty()) {
            new LoadM3UListThread(ServerPrefix).start();
//            new LoadListThread(ServerPrefix).start();
//            new LoadListThread(ServerPrefixAlternative).start();
//            new LoadIptvListThread().start();
        }
        new Thread(networkCheckRunnable).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                  @Override
                  public boolean onQueryTextSubmit(String query) {
                      return false;
                  }

                  @Override
                  public boolean onQueryTextChange(String newText) {
                      adapter.getFilter().filter(newText);
                      return false;
                  }
              }
        );

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //search is expanded
                appLogo.setVisibility(View.INVISIBLE);
                appTitle.setVisibility(View.INVISIBLE);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                // searchview closed
                appLogo.setVisibility(View.VISIBLE);
                appTitle.setVisibility(View.VISIBLE);
                return false;
            }
        });

        return true;
    }

    @Override
    protected void onDestroy() {

        PlayerFragmentData fragmentData = new PlayerFragmentData();
        fragmentData.setStationList(mStationList);
        fragmentData.setCurrentStationIndex(mCurrentStationIndex);
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
        ((FrameLayout) findViewById(R.id.main_media_frame)).addView(playerView);
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

        if (null != mCurrentStation) {
            MediaSource mVideoSource = buildMediaSource(Uri.parse(mCurrentStation.url.get(mCurrentSourceIndex)));
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
            findViewById(R.id.main_media_frame).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {
                    View frameView = findViewById(R.id.main_media_frame);
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

    protected void setCurrentPlayInfo(@NotNull Station station)
    {
        String logoUrl = station.logo;
        if (logoUrl == null || logoUrl.isEmpty())
        {
            imageCurrentStationLogo.setImageResource(getResources().getIdentifier("@drawable/tv", null, getPackageName()));
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
                    .into(imageCurrentStationLogo);
        }

        textChannelNameOverlay.setText(station.name);
        textCurrentStationName.setText(station.name);
        textCurrentStationName.setSelected(true);
    }

    protected  void switchSource() {

        if (null == mCurrentStation) {
            mCurrentStation = mStationList.get(mCurrentStationIndex);
        }

        int index = 0;
        if (mCurrentSourceIndex + 1 < mCurrentStation.url.size()) {
            index = mCurrentSourceIndex + 1;
        }
        play(mCurrentStation, index);
    }

    private String getSourceInfo(Station station, int source) {

        return source + 1 + "/" + station.url.size();
    }

    protected void play(Station station, int source) {

        mCurrentStationIndex = mStationList.indexOf(station);
        mCurrentSourceIndex = source;

        setCurrentPlayInfo(station);
        textCurrentStationSource.setText(getSourceInfo(station, source));
        textSourceInfoOverlay.setText(getSourceInfo(mCurrentStation, mCurrentSourceIndex));
        play(station.url.get(source));
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

    private void initStationListView() {

        Log.d(TAG, "initStationListView: ");

        if (null == mStationList || mStationList.size() < 1) {
            Toast toast= Toast.makeText(getApplicationContext(), "         Unable to load the station list.\nPlease check your network connection.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, -300);
            toast.show();
        }

        adapter= new StationListAdapter(this, mStationList);
        stationListView.setAdapter(adapter);
        stationListView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getBufferingInfo() {
        if (isBuffering) {
            int percent = getBufferedPercentage();
            String bufferingInfo = "" + percent + "%";
            textBufferingInfo.setText(bufferingInfo);
            Log.i(TAG, bufferingInfo);
        }
        if (mCurrentStation != null && mCurrentStation.name != null && mCurrentStation.name.length() > 0)
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
            if (null != jsonString && (mStationList == null || mStationList.size() == 0))
            {
                CurrentServerPrefix = serverPrefix;
                JSONObject object = JSON.parseObject(jsonString);
                Object objArray = object.get("stations");
                String str = objArray+"";
                mStationList = JSON.parseArray(str, Station.class);
                Log.d(TAG,  mStationList.size() +" stations loaded from server.");

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

            String jsonString = getJsonString(getResources().getString(R.string.iptv_station_list));
            if (null != jsonString && (mStationList == null || mStationList.size() == 0))
            {
                List<IptvStation> iptvStationList = JSON.parseArray(jsonString, IptvStation.class);
                Log.d(TAG,  iptvStationList.size() +" stations loaded from server.");

                List stationList = new ArrayList<Station>();
                for (IptvStation iptvStation: iptvStationList) {

                    List<String> urlList = new ArrayList<String>();
                    String logo = "";
                    for (Object s : stationList) {
                        if (((Station)s).name.equals(iptvStation.name)
                            ||((Station)s).name.equals(iptvStation.tvg.name)
                        ) {
                            urlList = ((Station)s).url;
                            stationList.remove(s);
                            break;
                        }
                    }
                    Station station = new Station();
                    station.name = (iptvStation.tvg.name!=null && !iptvStation.tvg.name.isEmpty()) ? iptvStation.tvg.name : iptvStation.name;
                    station.logo = iptvStation.logo;
                    urlList.add(iptvStation.url);
                    station.url = urlList;
                    stationList.add(station);
                }

                mStationList = stationList;

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

    public class LoadM3UListThread extends Thread {

        private String serverPrefix;

        LoadM3UListThread(String serverPrefix) {
            this.serverPrefix = serverPrefix;
        }

        @Override
        public void run() {

            if (serverPrefix == null || serverPrefix.length() < 1) {
                return;
            }

            String jsonString = getJsonString(serverPrefix + getResources().getString(R.string.station_list_file_name));
            if (null != jsonString && (mStationList == null || mStationList.size() == 0))
            {
                CurrentServerPrefix = serverPrefix;
                JSONObject object = JSON.parseObject(jsonString);
                Object objArray = object.get("stations");
                String str = objArray+"";
                mStationList = JSON.parseArray(str, Station.class);
                Log.d(TAG,  mStationList.size() +" stations loaded from server.");
            }

            String m3UString = getM3UString(getResources().getString(R.string.iptv_station_list));
            if (null != m3UString && mStationList != null )
            {
                CurrentServerPrefix = serverPrefix;

                try {
                    List<SimpleM3UParser.M3U_Entry> m3UStationList = new SimpleM3UParser().parseM3UString(m3UString);
                    for (SimpleM3UParser.M3U_Entry stationM3U : m3UStationList) {

                        List<String> urlList = new ArrayList<String>();
                        String logo = "";
                        for (Object s : mStationList) {
                            if (((Station)s).name.equals(stationM3U.getName())) {
                                urlList = ((Station)s).url;
                                if (((Station)s).logo.contains("http")) {
                                    logo = ((Station)s).logo;
                                }
                                mStationList.remove(s);
                                break;
                            }
                        }
                        Station station = new Station();
                        station.name = stationM3U.getName();
                        if (logo.contains("http")) {
                            station.logo = logo;
                        }
                        else {
                            station.logo = stationM3U.getTvgLogo();
                        }
                        urlList.add(stationM3U.getUrl());
                        station.url = urlList;
                        mStationList.add(station);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mStationList.size() > 0)
            {
                // Send Message to Main thread to load the station list
                mHandler.sendEmptyMessage(MSG_LOAD_LIST);
            }

            Log.d(TAG,  mStationList.size() +" stations loaded from server.");
        }

        @Nullable
        private String getM3UString(String url) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
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
                int selectedPosition = (int) msg.obj;
                mainActivity.mCurrentStation = mainActivity.mStationList.get(selectedPosition);
                mainActivity.play(mainActivity.mCurrentStation, 0);
            }
            else if (msg.what == MSG_GET_BUFFERING_INFO) {
                mainActivity.getBufferingInfo();
            }
        }
    }
}
