package com.picdora.channelPlayer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;

import pl.droidsonroids.gif.GifDrawable;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.picdora.ImageUtil;
import com.picdora.PicdoraApp;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelPlayer.ImageManager.OnGetChannelImageResultListener;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.LoadError;
import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelImage.LIKE_STATUS;
import com.picdora.models.Image;
import com.picdora.ui.GlowView;

/**
 * Display an image for the ChannelViewActivity
 */
@EFragment(R.layout.fragment_swipable_image)
public class ImageSwipeFragment extends Fragment implements
		PicdoraImageLoader.LoadCallbacks {

	@ColorRes(R.color.transparent)
	protected int mColorTransparent;
	@ColorRes(R.color.glow_liked)
	protected int mColorLiked;
	@ColorRes(R.color.glow_neutral)
	protected int mColorNeutral;
	@ColorRes(R.color.glow_disliked)
	protected int mColorDisliked;

	@ViewById(R.id.image)
	PhotoView mPhotoView;
	@ViewById(R.id.progress)
	LinearLayout mProgress;
	@ViewById(R.id.progressText)
	TextView mProgressText;
	@ViewById
	TextView deletedText;
	@ViewById
	TextView debugText;
	@ViewById
	GlowView glow;

	// The position of the fragment in the view pager. Use this to retrieve what
	// image we should show
	@FragmentArg
	int fragPosition;

	protected ChannelImage mImage;
	protected ChannelViewActivity mActivity;
	/** Whether the fragment is currently visible. */
	private boolean mVisible;

	/** keep track of whether or not this fragment had it's view destroyed. */
	private boolean mDestroyed = false;
	/**
	 * keep track of the original photo coords so we can tell when we are
	 * zoomed.
	 */
	private RectF mOriginalImageRect;
	/**
	 * When comparing the current image bounds to the original we'll give some
	 * leeway of a few pixels in case it's just slightly off.
	 */
	private static final int ZOOM_DIFFERENCE_THRESHOLD = 10;

	/** the number of times we have tried to load the image unsuccessfully. */
	private int mLoadAttempts = 0;
	/** Whether the image was successfully loaded and is currently displayed. */
	private boolean mImageLoaded = false;
	/** Whether an image is actively being loaded. */
	private boolean mIsImageLoading = false;
	/** number of times to retry image loading before giving up. */
	private static final int MAX_LOAD_ATTEMPTS = 2;

	/** Initialize and get the image that this fragment should be showing. */
	@AfterViews
	void getImageToShow() {
		setDebugText("Initializing fragment and loading image");

		showLoadingCircle();

		mActivity = (ChannelViewActivity) getActivity();

		if (mVisible) {
			mActivity.setCurrentFragment(this);
		}

		// if we already have the image then don't bother getting it again
		if (mImage != null) {
			loadImage();
		} else {
			setDebugText("Getting image info");
			mActivity.getImage(fragPosition, false,
					new OnGetChannelImageResultListener() {

						@Override
						public void onGetChannelImageResult(ChannelImage image) {
							mImage = image;
							loadImage();
						}
					});
		}

	}

	/**
	 * Check whether our image is zoomed in or not.
	 * 
	 * @return
	 */
	public boolean isZoomed() {
		if (mPhotoView == null || mActivity == null) {
			return false;
		}
		// compare the current display coords to our original
		RectF curr = mPhotoView.getDisplayRect();

		if (curr == null) {
			return false;
		}

		/**
		 * Strategy #1: Compare the height of the original image to the current
		 * one. If the difference is significant, (greater than
		 * {@link #ZOOM_DIFFERENCE_THRESHOLD}), then we are zoomed either out or
		 * in.
		 */
		float dif = Math.abs(Math.abs(curr.height())
				- Math.abs(mOriginalImageRect.height()));
		return dif > ZOOM_DIFFERENCE_THRESHOLD;

		/*
		 * Strategy #2: Check if the picture is larger than the window
		 * vertically. If not then a vertical swipe won't move it and we can
		 * interpret the gesture as a like action instead. The image may still
		 * be zoomed but it's not zoomed enough to cause a gesture collision.
		 */
		// return mActivity.getWindowHeight() < Math.abs(curr.height());
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

	private void loadImage() {
		// if the view was already destroyed then the user has moved on so don't
		// bother trying to load
		if (mDestroyed) {
			return;
		}

		mIsImageLoading = true;

		// we can't load an image if we don't have one...
		if (mImage == null) {
			setDebugText("Trying to load null image.");
			Util.log("Trying to load null image");
			// TODO: Show error image
			showLoadingCircle();
			return;
		}
		// get a new image if the existing one is deleted
		else if (mImage.getImage().isDeleted()) {
			setDebugText("Image deleted");
			handleDeletedImage();
		} else {
			setDebugText("Loading image: " + getImageInfoString(mImage));
			PicdoraImageLoader.instance().loadImage(mImage.getImage(), this);
		}
	}

	@Override
	public void onSuccess(Image image, Drawable drawable) {
		mIsImageLoading = false;

		if (!image.equals(mImage.getImage())) {
			// This isn't the image we are expecting... something terrible has
			// happened
			Util.log("Received mismatched image");
		}

		/*
		 * If the image was able to be decoded as a gif then it will be a gif
		 * drawable, otherwise it'll be a normal bitmap drawable. We want to
		 * make sure the db gif value for the image mirrors this.
		 */
		ImageUtil.setGifStatus(image, (drawable instanceof GifDrawable));

		if (!mDestroyed) {
			mPhotoView.setImageDrawable(drawable);
			mPhotoView.setVisibility(View.VISIBLE);
			mProgress.setVisibility(View.GONE);
			mImageLoaded = true;
			checkForImageView();
			setDebugText("Loaded: " + getImageInfoString(mImage));

			// create a copy of the original image rect so we can tell when
			// we're zoomed
			mOriginalImageRect = new RectF(mPhotoView.getDisplayRect());
			/*
			 * When the user zooms we can update our glow bounds to follow the
			 * image as it changes.
			 */
			mPhotoView.setOnMatrixChangeListener(new OnMatrixChangedListener() {

				@Override
				public void onMatrixChanged(RectF rect) {
					/*
					 * If our image had 0 size when first set then set the
					 * bounds now. This seems to happen for gifs, maybe because
					 * they aren't immediately drawn.
					 */
					if (mOriginalImageRect.height() == 0) {
						mOriginalImageRect = new RectF(rect);
					}
					// set the new bounds for the glow if it is glowing
					if (glow.isGlowing()) {
						glow.setGlowBounds(rect);
					}
				}
			});
		}
	}

	@Override
	public void onError(LoadError error) {
		mLoadAttempts++;
		mImageLoaded = false;
		mIsImageLoading = false;

		setDebugText("Image load fail: " + error.name());

		switch (error) {
		case DOWNLOAD_CANCELED:
		case DOWNLOAD_FAILURE:
		case FAILED_DECODE:
		case OUT_OF_MEMORY:
			// TODO: request memory cleanup. Maybe only retry if we are
			// currently visible.
		case DOWNLOAD_TIMEOUT:
		case UNKOWN:
			if (mVisible && mLoadAttempts < MAX_LOAD_ATTEMPTS) {
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
			// TODO: Show error image
			showLoadingCircle();
			break;
		}
	}

	/**
	 * Create a string containing information about the given image.
	 * 
	 * @param channelImage
	 *            The image to show information about
	 */
	private String getImageInfoString(ChannelImage channelImage) {
		Image image = channelImage.getImage();
		debugText.setVisibility(View.VISIBLE);

		String info = "(%d, %d, '%s'";
		if (image.isGif()) {
			info += ", gif";
		}
		if (image.isNsfw()) {
			info += ", nsfw";
		}
		info += ")";
		return String.format(info, image.getId(), image.getRedditScore(),
				image.getImgurId());
	}

	/**
	 * Set text to show at the top of the screen. Will only show if debug mode
	 * is enabled.
	 * 
	 * @param string
	 *            Text to show.
	 */
	private void setDebugText(String string) {
		if (PicdoraApp.DEBUG) {
			debugText.setVisibility(View.VISIBLE);
			debugText.setText(string);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mDestroyed = true;

		if (mImage != null) {
			PicdoraImageLoader.instance().unregisterCallbacks(
					mImage.getImage().getImgurId(), this);
		}

		mPhotoView.setImageDrawable(null);
		mImageLoaded = false;
		mPhotoView = null;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		mVisible = isVisibleToUser;

		if (isVisibleToUser) {
			if (mActivity != null) {
				mActivity.setCurrentFragment(this);
			}

			// If we have the image details and it is not loaded or actively
			// being loaded then start the process since the fragment is
			// visible.
			if (mImage != null && !mImageLoaded && !mIsImageLoading) {
				loadImage();
			} else {
				checkForImageView();
			}
		}
	}

	/**
	 * Check if the image is being viewed and if so report the view to the
	 * activity. We only count a view if the image is fully loaded and the
	 * fragment is visible.
	 * 
	 */
	private void checkForImageView() {
		if (mImage != null && mImageLoaded && mVisible) {
			mActivity.registerImageView(mImage);
		}
	}

	@Override
	public void onProgress(int percentComplete) {
		if (mDestroyed) {
			return;
		}

		setProgress(percentComplete);
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

	/**
	 * When our image turns out to be deleted we can request another, and if
	 * that fails we can show a message on screen.
	 */

	private void handleDeletedImage() {
		ImageUtil.markImageDeleted(mImage.getImage());

		if (mDestroyed) {
			return;
		}

		// try to load a different image
		showLoadingCircle();
		mActivity.getImage(fragPosition, true,
				new OnGetChannelImageResultListener() {

					@Override
					public void onGetChannelImageResult(ChannelImage image) {
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

	/**
	 * Set our image with the given like status, and make it glow in response to
	 * a like event.
	 * 
	 * @param status
	 *            The new status type that the glow should reflect
	 */
	@UiThread
	public void setLikeStatus(LIKE_STATUS status) {
		if (mImage != null) {
			mImage.setLikeStatus(status);
		} else {
			return;
		}

		if (mPhotoView == null) {
			return;
		}

		// get the bounds of the image relative to the view
		RectF r = mPhotoView.getDisplayRect();
		if (r == null) {
			return;
		}

		int color;
		switch (status) {
		case DISLIKED:
			color = mColorDisliked;
			break;
		case LIKED:
			color = mColorLiked;
			break;
		case NEUTRAL:
			color = mColorNeutral;
			break;
		default:
			color = mColorTransparent;
		}

		glow.setGlowBounds(r).setGlowColor(color).doGlow();
	}

	public ChannelImage getImage() {
		return mImage;
	}
}