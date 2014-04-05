package com.picdora.ui.gallery;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import uk.co.senab.photoview.PhotoView;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.picdora.ImageUtils;
import com.picdora.R;
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

	private Activity mContext;

	/** The {@link #com.picdora.models.Image} that we should display. */
	private Image mImageToDisplay;
	/** Whether the fragment's views have been initialized yet */
	private boolean mInitialized = false;

	private Future<ImageView> mCurrentDownload;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* Make the fragment full screen and black */
		setStyle(DialogFragment.STYLE_NO_FRAME,
				android.R.style.Theme_Black_NoTitleBar_Fullscreen);

		setRetainInstance(true);
	}

	@AfterViews
	protected void init() {
		mInitialized = true;
		mContext = getActivity();
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
		/* Cancel previous load to start the new one */
		if (mCurrentDownload != null && !mCurrentDownload.isDone()) {
			mCurrentDownload.cancel(true);
		}

		showLoadingCircle();

		mCurrentDownload = Ion
				.with(mContext)
				// .load("https://raw2.github.com/koush/ion/master/ion-sample/telescope.jpg")
				.load(ImageUtils.getImgurLink(image, ImageUtils.ImgurSize.FULL))
				.progressHandler(this).withBitmap().deepZoom()
				.intoImageView(mPhotoView)
				.setCallback(new FutureCallback<ImageView>() {
					@Override
					public void onCompleted(Exception e, ImageView result) {
						mPhotoView.setVisibility(View.VISIBLE);
						mProgressContainer.setVisibility(View.GONE);
					}
				});

	}

	/**
	 * Progress callback for the currently downloading image.
	 * 
	 */
	@Override
	public void onProgress(int downloaded, int total) {
		setProgress((int) ((downloaded * 100f) / total));
	}

	/**
	 * Show the loading circle, but hide the progress percentage for now.
	 * 
	 */
	private void showLoadingCircle() {
		try {
			mPhotoView.setVisibility(View.GONE);
			mProgressContainer.setVisibility(View.VISIBLE);
			mProgressText.setVisibility(View.GONE);
		} catch (NullPointerException e) {
			// the fragment was probably destroyed so we don't have to do
			// anything
		}
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
