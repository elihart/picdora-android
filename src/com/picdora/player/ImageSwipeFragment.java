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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koushikdutta.async.future.Future;
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
		ImageLoader.instance().loadImage(mImage, this);
	}

	/**
	 * Called when the image loader hits an out of memory error. If try again is
	 * true we'll attempt to load the image again with lower settings. Otherwise
	 * we'll give up and show an error
	 * 
	 * @param image
	 * @param tryAgain
	 */
	private void handleOutOfMemory(Image image, boolean tryAgain) {
		Util.log("Handling out of memory");

		if (tryAgain) {
			// retry with lower bitmap display

		} else {
			// TODO: Display error or something.
		}

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		ImageLoader.instance().unregisterCallbacks(mImage.getImgurId(), this);

		cleanupImages();		
	}

	/**
	 * Cleanup memory being used for images. Recycle any bitmaps in use, remove
	 * references, and cleanup the image attacher
	 */
	private void cleanupImages() {

		if (mPhotoView != null) {
			Drawable drawable = mPhotoView.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if (bitmap != null) {
					bitmap.recycle();
				}
			}
		}
		
		mPhotoView = null;
	}

	@Override
	public void onProgress(int percentComplete) {
		mProgressText.setVisibility(View.VISIBLE);
		mProgressText.setText(percentComplete + "%");		
	}

	@Override
	public void onSuccess(Bitmap bm) {
		if(mPhotoView == null){
			return;
		}
		
		mPhotoView.setImageBitmap(bm);
		mPhotoView.setVisibility(View.VISIBLE);
		mProgress.setVisibility(View.GONE);		
	}

	@Override
	public void onError(LoadError error) {
		Util.log("Image load error : " + error);		
	}
}