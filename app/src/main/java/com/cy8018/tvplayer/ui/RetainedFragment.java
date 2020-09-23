package com.cy8018.tvplayer.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

public class RetainedFragment extends Fragment {

    // data object we want to retain
    private PlayerFragmentData data;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setData(PlayerFragmentData data) {
        this.data = data;
    }

    public PlayerFragmentData getData() {
        return data;
    }
}

