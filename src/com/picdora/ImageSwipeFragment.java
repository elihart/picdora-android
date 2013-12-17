package com.picdora;

import net.frakbot.imageviewex.ImageViewNext;
import net.frakbot.imageviewex.ImageViewEx.FillDirection;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ImageSwipeFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.swipable_image, container, false);
        
        // set url
        String url = getArguments().getString("url");
        
        ImageViewNext image = (ImageViewNext) view.findViewById(R.id.image);
    	image.setUrl(url);
    	//image.setFillDirection(FillDirection.HORIZONTAL);
    	
    	return view;
    }
}