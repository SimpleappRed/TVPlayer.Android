package com.cy8018.tvplayer.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cy8018.tvplayer.R;

public class SettingsFragment extends Fragment {

    private Button loadDefaultBtn;
    private Button loadUrlBtn;
    private Button removeAllBtn;
    private TextView channelsCount;
    private TextView favChannelsCount;
    private EditText ChannelListUrl;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        loadDefaultBtn = view.findViewById(R.id.load_default_button);
        loadUrlBtn = view.findViewById(R.id.load_url_button);
        removeAllBtn = view.findViewById(R.id.remove_all_button);
        channelsCount = view.findViewById(R.id.channels_count);
        favChannelsCount = view.findViewById(R.id.fav_channels_count);
        ChannelListUrl = view.findViewById(R.id.playlist_url);

//        if (((MainActivity)getActivity()).getChannelList() != null && ((MainActivity)getActivity()).getChannelList().size() > 0){
//            loadDefaultBtn.setVisibility(View.INVISIBLE);
//        }

        int channelCount = 0;
        int favChannelCount = 0;
        if (((MainActivity)getActivity()).getChannelList() != null){
            channelCount = ((MainActivity)getActivity()).getChannelList().size();
        }
        if (((MainActivity)getActivity()).getChannelListFavorites() != null){
            favChannelCount = ((MainActivity)getActivity()).getChannelListFavorites().size();
        }

        channelsCount.setText(String.valueOf(channelCount));
        favChannelsCount.setText(String.valueOf(favChannelCount));

        loadUrlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = ChannelListUrl.getText().toString().toLowerCase();
                if (url != null && url.length() > 0) {

                    if (url.endsWith(".json") || url.endsWith(".json.gz")) {
                        ((MainActivity)getActivity()).loadJsonChannelList(url);
                    }
                    else if (url.endsWith(".m3u") || url.endsWith(".m3u8")) {
                        ((MainActivity)getActivity()).loadM3UChannelList(url);
                    }
                    else {
                        Toast toast= Toast.makeText(getContext(), "Incorrect M3U URL.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                else {
                    Toast toast= Toast.makeText(getContext(), "Please input URL.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });

        loadDefaultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (channelsCount.getText() == "0") {
                    ((MainActivity)getActivity()).loadChannelList(getResources().getString(R.string.tv_channel_list));
                }
                else
                {
                    Toast toast= Toast.makeText(getContext(), "Please remove all channels before loading sample channels.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });

        removeAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MainActivity)getActivity()).removeAllChannels()) {
                    Toast toast= Toast.makeText(getContext(), "All channels removed.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    channelsCount.setText("0");
                    favChannelsCount.setText("0");
                }
            }
        });
        return view;
    }
}
