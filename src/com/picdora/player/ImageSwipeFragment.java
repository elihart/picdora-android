package com.picdora.player;

import java.io.File;

import uk.co.senab.photoview.PhotoView;

import net.frakbot.imageviewex.ImageViewNext;
import net.frakbot.imageviewex.ImageViewNext.CacheLevel;
import net.frakbot.imageviewex.ImageViewNext.ImageLoadCompletionListener;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.DiscCacheUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.R.drawable;
import com.picdora.R.id;
import com.picdora.R.layout;
import com.picdora.models.Image;

@EFragment(R.layout.swipable_image)
public class ImageSwipeFragment extends Fragment {
	@ViewById(R.id.gif)
	ImageViewNext mGifView;
	@ViewById(R.id.image)
	PhotoView mPhotoView;
	@ViewById(R.id.progress)
	ProgressBar mProgress;

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
		mGifView.setVisibility(View.GONE);
		mPhotoView.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);

		// use a different loader if the image is an animated gif
		if (image.isGif()) {
			loadGif(image);
		} else {
			loadImage(image, null);
		}
	}

	private void loadGif(final Image image) {
		mGifView.setLoadCallbacks(new ImageLoadCompletionListener() {

			@Override
			public void onLoadStarted(ImageViewNext v, CacheLevel level) {

			}

			@Override
			public void onLoadError(ImageViewNext v, CacheLevel level) {
				// don't care about the cache misses, but if the network misses
				// then we have a problem
				switch (level) {
				case DISK:
					break;
				case MEMORY:
					break;
				case NETWORK:
					Util.log("Error getting gif from network");
					// TODO: Handle this
					break;
				default:
					break;
				}
			}

			@Override
			public void onLoadCompleted(ImageViewNext v, CacheLevel level) {
				mProgress.setVisibility(View.GONE);
				mGifView.setVisibility(View.VISIBLE);
			}
		});

		mGifView.setUrl(image.getUrl());
	}

	private void loadImage(final Image image, final DisplayImageOptions options) {

		ImageLoadingListener listener = new ImageLoadingListener() {

			@Override
			public void onLoadingStarted(String arg0, View arg1) {

			}

			@Override
			public void onLoadingFailed(String arg0, View arg1,
					FailReason reason) {
				mPhotoView.setImageResource(R.drawable.ic_launcher);
				switch (reason.getType()) {
				case DECODING_ERROR:
					break;
				case IO_ERROR:
					break;
				case NETWORK_DENIED:
					break;
				case OUT_OF_MEMORY:
					// Do something about the memory error. If options were
					// already passed in tell the handler not to try again with
					// new options
					if (options != null) {
						handleOutOfMemory(image, false);
					} else {
						handleOutOfMemory(image, true);
					}
					break;
				case UNKNOWN:
					break;
				default:
					break;
				}
			}

			@Override
			public void onLoadingComplete(String imageUri, View view, Bitmap bm) {

				// check if this image is actually a gif.
				DiscCacheAware cache = ImageLoader.getInstance().getDiscCache();
				File file = DiscCacheUtil.findInCache(image.getUrl(), cache);
				
				// check for image deletion
				// http://imgur.com/l7sMPQe.jpg
				int deletedImgHash = 1101349688;
				if(deletedImgHash == bm.hashCode()){
					Util.log("deleted image found! " + imageUri);
				}
				Util.log(imageUri + " : " + bm.hashCode());

				try {
					Movie gif = Movie.decodeFile(file.getAbsolutePath());
					if (gif != null) {
						image.setGif(true);
						loadGif(image);
						return;
					}
				} catch (Exception e) {

				}

				// add image to view
				mPhotoView.setImageBitmap(bm);
				mProgress.setVisibility(View.GONE);
				mPhotoView.setVisibility(View.VISIBLE);

				// attacher Photoviewer for zooming
				//mAttacher = new PhotoViewAttacher(mImageView);

			}

			@Override
			public void onLoadingCancelled(String arg0, View arg1) {

			}
		};

		// If specific display options were specified then use them. otherwise
		// use the defaults
		String url = image.getUrl();
		if (options == null) {
			ImageLoader.getInstance().loadImage(url, listener);
		} else {
			ImageLoader.getInstance().loadImage(url, options, listener);
		}
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
			DisplayImageOptions options = new DisplayImageOptions.Builder()
					.cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
					.bitmapConfig(Bitmap.Config.RGB_565).build();

			loadImage(image, options);
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

		mGifView.setMaxHeight(screenHeight);
		mGifView.setMaxWidth(screenWidth);

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

		if (mGifView != null) {
			Drawable drawable = mGifView.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if (bitmap != null) {
					bitmap.recycle();
				}
			}
		}

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