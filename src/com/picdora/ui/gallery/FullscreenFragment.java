package com.picdora.ui.gallery;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import uk.co.senab.photoview.PhotoView;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koushikdutta.ion.ProgressCallback;
import com.picdora.R;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.LoadCallbacks;
import com.picdora.imageloader.PicdoraImageLoader.LoadError;
import com.picdora.models.Image;

/**
 * Show a fullscreen view of an image.
 * 
 */
@EFragment(R.layout.fragment_fullscreen)
public class FullscreenFragment extends DialogFragment implements
		ProgressCallback {
	@ViewById(R.id.image)
	protected PhotoView mPhotoView;
	@ViewById(R.id.progress)
	protected LinearLayout mProgressContainer;
	@ViewById(R.id.progressText)
	protected TextView mProgressText;

	/** The {@link #com.picdora.models.Image} that we should display. */
	private Image mImageToDisplay;
	/** Whether the fragment's views have been initialized yet */
	private boolean mInitialized = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* Make the fragment full screen and black */
		setStyle(DialogFragment.STYLE_NO_FRAME,
				android.R.style.Theme_Black_NoTitleBar_Fullscreen);

		/*
		 * We want to retain on image to loaded and any download progress on
		 * rotation.
		 */
		setRetainInstance(true);
	}

	@AfterViews
	protected void init() {
		mInitialized = true;
		showLoadingCircle();

		/*
		 * If an image has already been provided for us to display than start
		 * loading it, otherwise wait until an image is set
		 */
		if (mImageToDisplay != null) {
			loadImage(mImageToDisplay);
		}
	}

	/**
	 * Set the image to display. If the fragment has not been initialized yet
	 * then this image will be loaded as soon as our views our ready. If the
	 * fragment views are already loaded then this image will be loaded now.
	 * 
	 * @param image
	 */
	public void setImage(Image image) {
		mImageToDisplay = image;
		/* Don't start loading the image unless our views are initialized */
		if (mInitialized) {
			loadImage(mImageToDisplay);
		}
	}

	/**
	 * Start downloading the given image and load it into the photoview when
	 * available. Show progress updates along the way.
	 * 
	 * @param imageToDisplay
	 */
	private void loadImage(Image image) {
		showLoadingCircle();

		PicdoraImageLoader.instance().loadImage(image, new LoadCallbacks() {

			@Override
			public void onSuccess(Image image, Drawable drawable) {
				if (image.equals(mImageToDisplay)) {
					mPhotoView.setImageDrawable(drawable);
					showPhotoView();
				}
			}

			@Override
			public void onProgress(int percentComplete) {
				setProgress(percentComplete);
			}

			@Override
			public void onError(LoadError error) {
				// TODO
			}
		});
	}

	/**
	 * Show the photoview and hide all other views.
	 * 
	 */
	private void showPhotoView() {
		mPhotoView.setVisibility(View.VISIBLE);
		mProgressContainer.setVisibility(View.GONE);
	}

	/**
	 * Progress callback for the currently downloading image.
	 * 
	 */
	@Override
	public void onProgress(int downloaded, int total) {
		/*
		 * Protect from divide by 0 and convert to a percent so we can show the
		 * progress level
		 */
		if (total != 0) {
			setProgress((int) ((downloaded * 100f) / total));
		}
	}

	/**
	 * Show the loading circle, but hide the progress percentage for now.
	 * 
	 */
	private void showLoadingCircle() {
		mPhotoView.setVisibility(View.GONE);
		mProgressContainer.setVisibility(View.VISIBLE);
		mProgressText.setVisibility(View.GONE);
	}

	private void setProgress(int percentComplete) {
		// on error the percent can be wacky
		if (percentComplete < 0 || percentComplete > 100) {
			mProgressText.setVisibility(View.GONE);
		} else {
			mProgressText.setVisibility(View.VISIBLE);
			mProgressText.setText(percentComplete + "%");
		}
	}

	@Override
	public void onDestroyView() {
		/*
		 * You may have to add this code to stop your dialog from being
		 * dismissed on rotation, due to a
		 * bug(https://code.google.com/p/android/issues/detail?id=17423) with
		 * the compatibility library.
		 * 
		 * http://stackoverflow.com/questions/14657490/how-to-properly-retain-a-
		 * dialogfragment-through-rotation
		 */
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}

		super.onDestroyView();
	}

}
