package com.cy8018.tvplayer.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.ChannelData;

import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {


    // station list
    protected List<ChannelData> mChannelList;
    private ChannelListAdapter adapter;
    private RecyclerView stationListView;
    private TextView textNoFavChannel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        stationListView = view.findViewById(R.id.channel_list_favorite);
        textNoFavChannel = view.findViewById(R.id.no_fav_channel_text);
        reloadList();
        return view;
    }

    public void reloadList() {
        mChannelList = ((MainActivity)getActivity()).getChannelListFavorites();

        if (mChannelList != null && mChannelList.size() > 0) {
            ViewGroup.LayoutParams layout = textNoFavChannel.getLayoutParams();
            layout.height = 0;
            textNoFavChannel.setLayoutParams(layout);
            textNoFavChannel.setVisibility(View.INVISIBLE);
        }

        int index = -1;
        int counter = -1;
        ChannelData currentChannel = ((MainActivity)getActivity()).getCurrentChannel();
        if (currentChannel != null) {
            for (ChannelData channel: mChannelList) {
                counter ++;
                if (channel.name.equals(currentChannel.name)) {
                    index = counter;
                    break;
                }
            }
        }

        if (index >= 0) {
            adapter = new ChannelListAdapter(this.getContext(), mChannelList, index);
        }
        else {
            adapter = new ChannelListAdapter(this.getContext(), mChannelList);
        }

        stationListView.setAdapter(adapter);
        stationListView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        if (index > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            Objects.requireNonNull(stationListView.getLayoutManager()).scrollToPosition(index);
        }
    }
}
