package com.cy8018.tvplayer.ui;

import com.cy8018.tvplayer.model.Station;

import java.util.List;

public class PlayerFragmentData {

    private List<Station> mStationList;

    private int mCurrentStationIndex;

    private int mCurrentSourceIndex;

    public List<Station> getStationList() {
        return mStationList;
    }

    public void setStationList(List<Station> list) {
        mStationList = list;
    }

    public int getCurrentSourceIndex() {
        return mCurrentSourceIndex;
    }

    public void setCurrentSourceIndex(int source) {
        mCurrentSourceIndex = source;
    }

    public int getCurrentStationIndex() {
        return  mCurrentStationIndex;
    }

    public void setCurrentStationIndex(int index) {
        mCurrentStationIndex = index;
    }
}