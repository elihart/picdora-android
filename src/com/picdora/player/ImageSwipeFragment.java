package com.picdora.player;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import uk.co.senab.photoview.PhotoView;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Image;

@EFragment(R.layout.fragment_swipable_image)
public class ImageSwipeFragment extends Fragment {
	@ViewById(R.id.image)
	PhotoView mPhotoView;
	@ViewById(R.id.progress)
	LinearLayout mProgress;
	@ViewById(R.id.progressText)
	TextView mProgressText;

	@AfterViews
	void addImage() {
		// get image to display

		String imageJson = getArguments().getString("imageJson");
		Image image = Util.fromJson(imageJson, Image.class);

		// Date start = new Date();
		// Date end = new Date();
		// Util.log("Create image " + (end.getTime() - start.getTime()));

		setScreenSize();

		// reset and hide the views until an image is loaded
		cleanupImages();

		mPhotoView.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);
		mProgressText.setVisibility(View.GONE);

		loadImage(image);
	}

	private void loadImage(final Image image) {

		Ion.with(getActivity(), image.getUrl())
				.progressHandler(new ProgressCallback() {

					@Override
					public void onProgress(int current, int size) {
						mProgressText.setVisibility(View.VISIBLE);
						int percent = (int) (current * 100f / size);					
						mProgressText.setText(percent + "%");
					}
				})
				.withBitmap()
				.error(R.drawable.ic_launcher)
				.intoImageView(mPhotoView)				
				.setCallback(new FutureCallback<ImageView>() {

					@Override
					public void onCompleted(Exception arg0, ImageView arg1) {
						mProgress.setVisibility(View.GONE);
						mPhotoView.setVisibility(View.VISIBLE);	
					}
				});
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

	/**
	 * Set the max size of the image views to be the screen size. This will
	 * allow us to save memory by scaling the images to fit the screen
	 */
	private void setScreenSize() {
		// Give the screen size so images are scaled to save memory
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);

		// get max size for display
		int screenHeight = displaymetrics.heightPixels;
		int screenWidth = displaymetrics.widthPixels;

		mPhotoView.setMaxHeight(screenHeight);
		mPhotoView.setMaxWidth(screenWidth);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

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
	}
}