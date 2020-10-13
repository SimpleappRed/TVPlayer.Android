package com.cy8018.tvplayer.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.ChannelData;

import java.util.List;
import java.util.Objects;

public class ChannelsFragment extends Fragment {
    // station list
    protected List<ChannelData> mChannelList;

    private EditText searchView;
    private ChannelListAdapter adapter;
    private RecyclerView stationListView;
    private TextView textNoChannel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channels, container, false);
        stationListView = view.findViewById(R.id.station_list_all);
        textNoChannel = view.findViewById(R.id.no_channel_text);
        searchView = view.findViewById(R.id.search_bar);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        reloadList();

        return view;
    }

    public void clearFilter() {
        adapter.getFilter().filter("");
    }

    public void reloadList() {
        mChannelList = ((MainActivity)getActivity()).getChannelList();

        if (mChannelList != null && mChannelList.size() > 0) {
            ViewGroup.LayoutParams layout = textNoChannel.getLayoutParams();
            layout.height = 0;
            textNoChannel.setLayoutParams(layout);
            textNoChannel.setVisibility(View.INVISIBLE);
        }

        int index = -1;
        ChannelData currentChannel = ((MainActivity)getActivity()).getCurrentChannel();
        if (currentChannel != null) {
            for (ChannelData channel: mChannelList) {
                index ++;
                if (channel.name.equals(currentChannel.name)) {
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
