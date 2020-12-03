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
import com.cy8018.tvplayer.util.MarginItemDecoration;

import java.util.List;
import java.util.Objects;

public class ChannelsFragment extends Fragment {
    // station list
    protected List<ChannelData> mChannelList;

    private EditText searchView;
    private ChannelListAdapter adapter;
    private RecyclerView channelListView;
    private TextView textNoChannel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channels, container, false);
        channelListView = view.findViewById(R.id.channel_list_all);
        channelListView.addItemDecoration(new MarginItemDecoration(getContext(), 50, 8));
        textNoChannel = view.findViewById(R.id.no_channel_text);
        searchView = view.findViewById(R.id.search_bar);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (adapter != null) {
                    adapter.getFilter().filter(charSequence);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    @Override
    public void onPause() {
        clearFilter();
        super.onPause();
    }

    public void clearFilter() {
        searchView.setText("");
        adapter.getFilter().filter("");
    }

    public void reloadList() {
        MainActivity mainActivity = ((MainActivity)getActivity());
        if (mainActivity != null) {
            mChannelList = mainActivity.getChannelList();

            if (mChannelList != null && mChannelList.size() > 0) {
                textNoChannel.setVisibility(View.GONE);
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

            channelListView.setAdapter(adapter);
            channelListView.setLayoutManager(new LinearLayoutManager(this.getContext()));
            if (index > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                Objects.requireNonNull(channelListView.getLayoutManager()).scrollToPosition(index);
            }
        }
    }
}
