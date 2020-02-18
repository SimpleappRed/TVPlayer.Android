package com.cy8018.tvplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
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
    //public static final String StationListUrl = "http://52.155.97.142/tv/tv_station_list.json";
    //https://dev.azure.com/cy8018/Resources/_apis/sourceProviders/tfsgit/filecontents?repository=Resources&commitOrBranch=master&path=tv/tv_station_list.json&api-version=5.0-preview.1

    // station list
    protected List<Station> mStationList;

    private Station mCurrentStation;

    private String mCurrentUrl;

    public final MsgHandler mHandler = new MsgHandler(this);

    // message to load the station list
    public static final int MSG_LOAD_LIST = 0;

    // message to play the radio
    public static final int MSG_PLAY = 1;

    TextView textCurrentStationName, textCurrentStationSource;

    SimpleExoPlayer player;
    PlayerView playerView;
    DataSource.Factory dataSourceFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ");

        playerView = findViewById(R.id.video_view);
        textCurrentStationName = findViewById(R.id.textCurrentStationName);
        textCurrentStationSource = findViewById(R.id.textCurrentStationSource);

        textCurrentStationSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSource();
            }
        });

        initPlayer();

        new Thread(loadListRunnable).start();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    protected void initPlayer() {
        if (null == player) {
            player = new SimpleExoPlayer.Builder(this).build();
        }

        player.addListener(this);
        // Bind the player to the view.
        playerView.setPlayer(player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        // Produces DataSource instances through which media data is loaded.
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "TvPlayer"));
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    protected  void switchSource() {
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
        textCurrentStationName.setText(station.name);
        String sourceInfo = source + 1 + "/"+station.url.size();
        textCurrentStationSource.setText(sourceInfo);
        play(station.url.get(source));
    }

    protected void play(String url) {
        mCurrentUrl = url;
        Uri uri = Uri.parse(url);
        if (player.isPlaying()) {
            player.stop();
        }

        // This is the MediaSource representing the media to be played.
        MediaSource mediaSource;

        // Makes a best guess to infer the type from a Uri.
        int mediaType = Util.inferContentType(uri);
        switch (mediaType) {
            case C.TYPE_DASH:
                Log.d(TAG, "MediaType: DASH,  URL:" + url);
                mediaSource =  new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
            case C.TYPE_SS:
                Log.d(TAG, "MediaType: SS,  URL:" + url);
                mediaSource =  new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
            case C.TYPE_HLS:
                Log.d(TAG, "MediaType: HLS,  URL:" + url);
                mediaSource =  new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
            default:
                Log.d(TAG, "MediaType: OTHER,  URL:" + url);
                mediaSource =  new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
        }

        // Prepare the player with the source.
        player.prepare(mediaSource);
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
