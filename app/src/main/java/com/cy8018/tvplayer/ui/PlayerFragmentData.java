package com.cy8018.tvplayer.ui;

import com.cy8018.tvplayer.db.ChannelData;

import java.util.ArrayList;
import java.util.List;

public class PlayerFragmentData {

    private List<ChannelData> mChannelList;

    private ChannelData mCurrentChannel;

    private int mCurrentChannelIndex;

    private int mCurrentSourceIndex;

    public List<ChannelData> getChannelList() {
        return mChannelList;
    }

    public ChannelData getCurrentChannel() {
        return mCurrentChannel;
    }

    public void setCurrentChannel(ChannelData channel) {
        mCurrentChannel = channel;
    }

    public void setChannelList(List<ChannelData> list) {
        mChannelList = list;
    }

//    public void setChannelListFull(List<ChannelData> list) {
//        mChannelListFull = list;
//    }

    public int getCurrentSourceIndex() {
        return mCurrentSourceIndex;
    }

    public void setCurrentSourceIndex(int source) {
        mCurrentSourceIndex = source;
    }

    public int getCurrentChannelIndex() {
        return mCurrentChannelIndex;
    }

    public void setCurrentChannelIndex(int index) {
        mCurrentChannelIndex = index;
    }
}