package com.picdora.player;

import android.os.Bundle;
import android.support.v4.app.Fragment;



public class DataFragment extends Fragment {
    private ChannelPlayer player;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setData(ChannelPlayer player) {
        this.player = player;
    }

    public ChannelPlayer getData() {
        return player;
    }


}
