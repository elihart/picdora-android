package com.picdora.player;

import java.util.Date;

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
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.LoadError;
import com.picdora.models.Image;
import com.picdora.player.ChannelPlayer.OnGetChannelImageResultListener;
import com.picdora.player.ChannelViewActivity.OnGetImageResultListener;

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

	// The position of the fragment in the view pager. Use this to retrieve what
	// image we should show
	@FragmentArg
	int fragPosition;

	protected Image mImage;
	protected ChannelViewActivity mActivity;

	// keep track of whether or not this fragment had it's view destroyed
	private boolean viewActive;

	// the number of times we have tried to load the image unsuccessfully
	private int mLoadAttempts;
	// number of times to retry image loading before giving up
	private static final int MAX_LOAD_ATTEMPTS = 2;

	@AfterViews
	void addImage() {
		viewActive = true;
		mLoadAttempts = 0;

		showLoadingCircle();

		mActivity = (ChannelViewActivity) getActivity();

		// if we already have the image then don't bother getting it again
		if (mImage != null) {
			loadImage();
		} else {
			//final Date start = new Date();
			mActivity.getImage(fragPosition, false,
					new OnGetImageResultListener() {

						@Override
						public void onGetImageResult(Image image) {
							mImage = image;
//							Util.log("Getting " + image.getImgurId() + " took "
//									+ (new Date().getTime() - start.getTime()));
							loadImage();
						}
					});
		}

	}

	private void showLoadingCircle() {
		try {
			mPhotoView.setVisibility(View.GONE);
			mProgress.setVisibility(View.VISIBLE);
			mProgressText.setVisibility(View.GONE);
			deletedText.setVisibility(View.GONE);
		} catch (NullPointerException e) {
			// the fragment was probably destroyed so we don't have to do
			// anything
		}
	}

	private Date loadStart;
	private boolean downloading;

	private void loadImage() {
		// if the view was already destroyed then the user has moved on so don't
		// bother trying to load
		if (!viewActive) {
			return;
		} 
		// we can't load an image if we don't have one...
		else if (mImage == null) {
			showLoadingCircle();
			// TODO: Maybe show an error image?
			return;
		} else if (mImage.isDeleted()) {
			handleDeletedImage();
		} else {
			loadStart = new Date();
			downloading = false;
//			Util.log("Load " + mImage.getImgurId());
			PicdoraImageLoader.instance().loadImage(mImage, this);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		viewActive = false;

		if (mImage != null) {
			PicdoraImageLoader.instance().unregisterCallbacks(
					mImage.getImgurId(), this);
		}

		mPhotoView.setImageDrawable(null);

		mPhotoView = null;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (isVisibleToUser) {
			// make sure we are loading this image as a priority
			loadImage();
//			if (mImage != null) {
//				Util.log("Viewing " + mImage.getImgurId());
//			}
		}
	}

	@Override
	public void onProgress(int percentComplete) {
		if (!downloading) {
//			Util.log("Image " + mImage.getImgurId() + " took "
//					+ (new Date().getTime() - loadStart.getTime())
//					+ " to start");
			downloading = true;
		}

		if (!viewActive) {
			return;
		}

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
//		Util.log("Image " + mImage.getImgurId() + " took "
//				+ (new Date().getTime() - loadStart.getTime()) + " to finish");
		// TODO: check whether it is animated or not and update the gif status
		// in the db in background if it's not right
		if (viewActive) {
			mPhotoView.setImageDrawable(drawable);
			mPhotoView.setVisibility(View.VISIBLE);
			mProgress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onError(LoadError error) {
//		Util.log("Load fail " + mImage.getImgurId() + " " + error);
		mLoadAttempts++;

		switch (error) {
		case DOWNLOAD_CANCELED:
			// if we're visible than we need to load the image
			if (isVisible()) {
				loadImage();
			}
			break;
		case DOWNLOAD_FAILURE:
		case FAILED_DECODE:
		case OUT_OF_MEMORY:
			// TODO: request memory cleanup
		case DOWNLOAD_TIMEOUT:
		case UNKOWN:
			if (mLoadAttempts < MAX_LOAD_ATTEMPTS) {
				loadImage();
			} else {
				// TODO: Load error image
				showLoadingCircle();
			}
			break;
		case IMAGE_DELETED:
			handleDeletedImage();
			break;
		default:
			// TODO: Show error imge
			showLoadingCircle();
			break;
		}
	}

	private void handleDeletedImage() {
		// TODO: Set up reporting to server and save to db
		mImage.setDeleted(true);

		if (!viewActive) {
			return;
		}

		// try to load a different image
		showLoadingCircle();
		mActivity.getImage(fragPosition, true, new OnGetImageResultListener() {

			@Override
			public void onGetImageResult(Image image) {
				if (image == null || image.equals(mImage)) {
					// couldn't get a replacement :(
					mImage = image;
					showDeletedText();
				} else {
					mImage = image;
					loadImage();
				}
			}
		});
	}

	private void showDeletedText() {
		try {
			deletedText.setVisibility(View.VISIBLE);
			mProgress.setVisibility(View.GONE);
			mPhotoView.setVisibility(View.GONE);
		} catch (NullPointerException e) {

		}
	}
}