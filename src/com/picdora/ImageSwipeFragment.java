package com.picdora;

import net.frakbot.imageviewex.ImageViewNext;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ImageSwipeFragment extends Fragment {
	private ImageViewNext mImage; 


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.swipable_image, container, false);
        mImage = (ImageViewNext) view.findViewById(R.id.image);
        
        
        // set url
        String url = getArguments().getString("url");   
        Util.log(url);
        mImage.setUrl(url);
    	
    	return view;
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView();
    	if(mImage != null){
    		Drawable drawable = mImage.getDrawable();
    		if (drawable instanceof BitmapDrawable) {
    		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
    		    Bitmap bitmap = bitmapDrawable.getBitmap();
    		    if(bitmap != null){
    		    	bitmap.recycle();
    		    }
    		}
    	}
    }
}