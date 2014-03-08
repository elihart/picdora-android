package com.picdora.player;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;

import uk.co.senab.photoview.PhotoView;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.Util;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.LoadError;
import com.picdora.models.Image;

@EFragment(R.layout.fragment_swipable_image)
public class ImageSwipeFragment extends Fragment implements
		PicdoraImageLoader.LoadCallbacks {

	@ViewById(R.id.image)
	PhotoView mPhotoView;
	@ViewById(R.id.progress)
	LinearLayout mProgress;
	@ViewById(R.id.progressText)
	TextView mProgressText;
	@ViewById
	TextView deletedText;

	// The position of the fragment in the view pager. Use this to retrieve what image we should show
	@FragmentArg
	int fragPosition;

	// if we try to load an image that was deleted we can request a replacement
	// and then try again. If we get really unlucky, or something goes wrong, we
	// might keep getting deleted images so let's give up after a few tries
	private int numDeletedImages;
	private static final int NUM_DELETED_ATTEMPTS = 5;

	protected Image mImage;
	protected ChannelViewActivity mActivity;

	// the number of times we have tried to load the image unsuccessfully
	private int mLoadAttempts;
	// number of times to retry image loading before giving up
	private static final int MAX_LOAD_ATTEMPTS = 2;

	@AfterViews
	void addImage() {
		numDeletedImages = 0;
		mLoadAttempts = 0;

		mPhotoView.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);
		mProgressText.setVisibility(View.GONE);
		deletedText.setVisibility(View.GONE);

		mActivity = (ChannelViewActivity) getActivity();
		mImage = mActivity.getImage(fragPosition);

		loadImage();
	}

	private void loadImage() {
		if (mImage == null) {
			return;
		} else if (mImage.isDeleted()) {
			handleDeletedImage();
		} else {
			PicdoraImageLoader.instance().loadImage(mImage, this);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		PicdoraImageLoader.instance().unregisterCallbacks(mImage.getImgurId(),
				this);

		mPhotoView.setImageDrawable(null);

		mPhotoView = null;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (isVisibleToUser) {
			// make sure we are loading this image as a priority
			loadImage();
		}
	}

	@Override
	public void onProgress(int percentComplete) {
		// on error the percent can be wacky
		if (percentComplete < 0 || percentComplete > 100) {
			mProgressText.setVisibility(View.GONE);
		} else {
			mProgressText.setVisibility(View.VISIBLE);
			mProgressText.setText(percentComplete + "%");
		}
	}

	@Override
	public void onSuccess(Drawable drawable) {
		if (mPhotoView == null) {
			return;
		}

		mPhotoView.setImageDrawable(drawable);
		mPhotoView.setVisibility(View.VISIBLE);
		mProgress.setVisibility(View.GONE);
	}

	@Override
	public void onError(LoadError error) {
		mLoadAttempts++;

		switch (error) {
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
			break;
		case IMAGE_DELETED:
			handleDeletedImage();
			return;
		default:
			break;
		}

		if (mLoadAttempts < MAX_LOAD_ATTEMPTS) {
			loadImage();
		} else {
			// TODO: Load error image
		}
	}

	private void handleDeletedImage() {
		Util.log("Deleted: " + mImage.getUrl());

		// mPhotoView is null if the fragment was destroyed
		if (mPhotoView == null) {
			return;
		}

		// TODO: Set up reporting to server and save to db
		mImage.setDeleted(true);

		// if we keep getting deleted images, then something might be wrong...
		// anyway, give up after a few attempts
		numDeletedImages++;
		if (numDeletedImages < NUM_DELETED_ATTEMPTS) {
			// try to load a different image
			mProgressText.setVisibility(View.GONE);
			mProgress.setVisibility(View.VISIBLE);
			mPhotoView.setVisibility(View.GONE);
			Image replacement = mActivity.getReplacementImage(fragPosition);

			if (replacement == null || replacement.equals(mImage)) {
				// couldn't get a replacement :(
				Util.log("bad replacement");
				showDeletedText();
			} else {
				Util.log("loading replacement");
				mImage = replacement;
				loadImage();
			}
		}
		// otherwise just say the image was deleted
		else {
			Util.log("Attempts ran out");
			showDeletedText();
		}
	}

	private void showDeletedText() {
		if (mPhotoView == null) {
			return;
		}

		deletedText.setVisibility(View.VISIBLE);
		mProgress.setVisibility(View.GONE);
		mPhotoView.setVisibility(View.GONE);
	}
}