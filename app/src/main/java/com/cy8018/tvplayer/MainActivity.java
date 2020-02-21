package com.cy8018.tvplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


import java.lang.ref.WeakReference;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Player.EventListener{

    private static final String TAG = "MainActivity";

    // Station list JSON file url
    public static final String StationListUrl = "https://gitee.com/cy8018/Resources/raw/master/tv/tv_station_list.json";

    // station list
    protected List<Station> mStationList;

    private Station mCurrentStation;

    private String mCurrentStationName;

    private int mCurrentStationIndex;

    private String mCurrentSource;

    private String mCurrentUrl;

    public final MsgHandler mHandler = new MsgHandler(this);

    // message to load the station list
    public static final int MSG_LOAD_LIST = 0;

    // message to play the radio
    public static final int MSG_PLAY = 1;

    TextView textCurrentStationName, textCurrentStationSource;

    private final String STATE_RESUME_WINDOW = "resumeWindow";
    private final String STATE_RESUME_POSITION = "resumePosition";
    private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";
    private final String STATE_CURRENT_URL = "currentUrl";
    private final String STATE_CURRENT_SOURCE = "currentSource";
    private final String STATE_CURRENT_STATION_INDEX = "currentStationIndex";
    private final String STATE_CURRENT_STATION_NAME = "currentStationName";

    private int mResumeWindow;
    private long mResumePosition;
    private boolean mExoPlayerFullscreen = false;

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private MediaSource mVideoSource;
    private FrameLayout mFullScreenButton;
    private ImageView mFullScreenIcon;
    private Dialog mFullScreenDialog;
    private DataSource.Factory dataSourceFactory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: ");

        // Produces DataSource instances through which media data is loaded.
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)));

        textCurrentStationName = findViewById(R.id.textCurrentStationName);
        textCurrentStationSource = findViewById(R.id.textCurrentStationSource);

        textCurrentStationSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSource();
            }
        });

        if (savedInstanceState != null) {
            mResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
            mResumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION);
            mExoPlayerFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
            mCurrentUrl = savedInstanceState.getString(STATE_CURRENT_URL);
            mCurrentSource = savedInstanceState.getString(STATE_CURRENT_SOURCE);
            mCurrentStationIndex = savedInstanceState.getInt(STATE_CURRENT_STATION_INDEX);
            mCurrentStationName = savedInstanceState.getString(STATE_CURRENT_STATION_NAME);

            textCurrentStationName.setText(mCurrentStationName);
            textCurrentStationSource.setText(mCurrentSource);
        }

        new Thread(loadListRunnable).start();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt(STATE_RESUME_WINDOW, mResumeWindow);
        outState.putLong(STATE_RESUME_POSITION, mResumePosition);
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, mExoPlayerFullscreen);
        outState.putString(STATE_CURRENT_URL, mCurrentUrl);
        outState.putString(STATE_CURRENT_SOURCE, mCurrentSource);
        outState.putString(STATE_CURRENT_STATION_NAME, mCurrentStationName);
        outState.putInt(STATE_CURRENT_STATION_INDEX, mCurrentStationIndex);

        super.onSaveInstanceState(outState);
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


    private void openFullscreenDialog() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        mFullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_skrink));
        mExoPlayerFullscreen = true;
        mFullScreenDialog.show();
    }


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
        mFullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
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

        boolean haveResumePosition = mResumeWindow != C.INDEX_UNSET;

        if (haveResumePosition) {
            Log.i("DEBUG"," haveResumePosition ");
            player.seekTo(mResumeWindow, mResumePosition);
        }

        if (null != mCurrentUrl && !mCurrentUrl.isEmpty()) {
            mVideoSource = buildMediaSource(Uri.parse(mCurrentUrl));
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
    }


    private MediaSource buildMediaSource(Uri uri) {
        @C.ContentType int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
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
            player.stop();
            player.release();
            player = null;
        }
    }

    protected  void switchSource() {

        if (null == mCurrentStation) {
            mCurrentStation = mStationList.get(mCurrentStationIndex);
        }

        int index = 0;
        int currentIndex = mCurrentStation.url.lastIndexOf(mCurrentUrl);
        if (currentIndex >= mCurrentStation.url.size() - 1) {
            index = 0;
        }
        else {
            index = currentIndex + 1;
        }
        play(mCurrentStation, index);
    }

    protected void play(Station station, int source) {

        mCurrentStationName = station.name;
        mCurrentSource = source + 1 + "/" + station.url.size();
        mCurrentStationIndex = mStationList.indexOf(station);

        textCurrentStationName.setText(mCurrentStationName);
        textCurrentStationSource.setText(mCurrentSource);
        play(station.url.get(source));
    }

    protected void play(String url) {
        mCurrentUrl = url;
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
        RecyclerView stationListView = findViewById(R.id.stationRecyclerView);
        StationListAdapter adapter= new StationListAdapter(this, mStationList);
        stationListView.setAdapter(adapter);
        stationListView.setLayoutManager(new LinearLayoutManager(this));
    }

    Runnable loadListRunnable = new Runnable(){
        @Override
        public void run() {
            String jsonString = getJsonString(StationListUrl);
            JSONObject object = JSON.parseObject(jsonString);
            Object objArray = object.get("stations");
            String str = objArray+"";

            mStationList = JSON.parseArray(str, Station.class);
            Log.d(TAG,  mStationList.size() +" stations loaded from server.");

            // Send Message to Main thread to load the station list
            mHandler.sendEmptyMessage(MSG_LOAD_LIST);
        }

        @Nullable
        private String getJsonString(String url) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response responses = client.newCall(request).execute();
                String jsonData = responses.body().string();
                Log.d(TAG, "getJsonString: [" + jsonData + "]");

                return jsonData;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    public static class MsgHandler extends Handler {
        WeakReference<MainActivity> mMainActivityWeakReference;

        MsgHandler(MainActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
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
        }
    }
}
