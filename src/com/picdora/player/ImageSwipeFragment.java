package com.picdora.player;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import uk.co.senab.photoview.PhotoView;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.Util;
import com.picdora.imageloader.ImageLoader;
import com.picdora.imageloader.ImageLoader.LoadError;
import com.picdora.models.Image;

@EFragment(R.layout.fragment_swipable_image)
public class ImageSwipeFragment extends Fragment implements ImageLoader.LoadCallbacks {
	
	@ViewById(R.id.image)
	PhotoView mPhotoView;
	@ViewById(R.id.progress)
	LinearLayout mProgress;
	@ViewById(R.id.progressText)
	TextView mProgressText;
	
	protected Image mImage;
	
	// the number of times we have tried to load the image unsuccessfully
	private int mLoadAttempts;
	// number of times to retry before giving up
	private static final int MAX_LOAD_ATTEMPTS = 2;

	@AfterViews
	void addImage() {
		// get image to display

		String imageJson = getArguments().getString("imageJson");
		mImage = Util.fromJson(imageJson, Image.class);

		// Date start = new Date();
		// Date end = new Date();
		// Util.log("Create image " + (end.getTime() - start.getTime()));

		mPhotoView.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);
		mProgressText.setVisibility(View.GONE);

		loadImage();
	}
	
	private void loadImage(){
		if(mImage == null){
			Util.log("Trying to load null image!");			
			return;
		}

		ImageLoader.instance().loadImage(mImage, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		ImageLoader.instance().unregisterCallbacks(mImage.getImgurId(), this);
		
		mPhotoView.setImageDrawable(null);

		mPhotoView = null;	
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
	    super.setUserVisibleHint(isVisibleToUser);
	    
	    if(isVisibleToUser){
	    	// make sure we are loading this image as a priority
	    	loadImage();
	    }
	}


	@Override
	public void onProgress(int percentComplete) {
		mProgressText.setVisibility(View.VISIBLE);
		mProgressText.setText(percentComplete + "%");		
	}

	@Override
	public void onSuccess(Drawable drawable) {
		if(mPhotoView == null){
			return;
		}
		
		mPhotoView.setImageDrawable(drawable);
		mPhotoView.setVisibility(View.VISIBLE);
		mProgress.setVisibility(View.GONE);		
	}

	@Override
	public void onError(LoadError error) {
		mLoadAttempts++;
		
		switch(error){
		case DOWNLOAD_CANCELED:			
			break;
		case DOWNLOAD_FAILURE:			
			break;
		case FAILED_DECODE:			
			break;
		case OUT_OF_MEMORY:
			// TODO: request memory cleanup
			break;			
		case UNKOWN:
		}
		
		if(mLoadAttempts < MAX_LOAD_ATTEMPTS){
			loadImage();
		} else {
			// TODO: Load error image
		}
	}
}