package com.picdora;

import uk.co.senab.photoview.PhotoViewAttacher;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import net.frakbot.imageviewex.ImageViewEx.FillDirection;
import net.frakbot.imageviewex.ImageViewNext;
import net.frakbot.imageviewex.ImageViewNext.CacheLevel;
import net.frakbot.imageviewex.ImageViewNext.ImageLoadCompletionListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class ImageSwipeFragment extends Fragment {
	private ImageViewNext mGifView;
	private ImageView mImageView;
	private PhotoViewAttacher mAttacher;
	private ProgressBar mProgress;
	private RelativeLayout mImagesContainer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.swipable_image, container, false);
		mGifView = (ImageViewNext) view.findViewById(R.id.gif);
		mImageView = (ImageView) view.findViewById(R.id.image);
		mProgress = (ProgressBar) view.findViewById(R.id.progress);
		mImagesContainer = (RelativeLayout) view
				.findViewById(R.id.images_container);

		// get image to display
		String imageJson = getArguments().getString("imageJson");
		Image image = Util.fromJson(imageJson, Image.class);

		// get max size for display
		int screenHeight = getArguments().getInt("screenHeight");
		int screenWidth = getArguments().getInt("screenWidth");
		final float screenAspectRatio = (float) screenWidth / screenHeight;

		mGifView.setMaxHeight(screenHeight);
		mGifView.setMaxWidth(screenWidth);

		mImageView.setMaxHeight(screenHeight);
		mImageView.setMaxWidth(screenWidth);

		// set url and load image
		final String url = image.getUrl();

		if (image.isGif()) {
			mGifView.setVisibility(View.VISIBLE);
			mImageView.setVisibility(View.GONE);

			mGifView.setLoadCallbacks(new ImageLoadCompletionListener() {

				@Override
				public void onLoadStarted(ImageViewNext v, CacheLevel level) {
					setIsLoading(true);
				}

				@Override
				public void onLoadError(ImageViewNext v, CacheLevel level) {
					setIsLoading(false);
					Util.log("Error loading gif " + url);

				}

				@Override
				public void onLoadCompleted(ImageViewNext v, CacheLevel level) {
					setIsLoading(false);

					float imageAspectRatio = v.getGifAspectRatio();
					if (imageAspectRatio > screenAspectRatio) {
						RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
								RelativeLayout.LayoutParams.MATCH_PARENT,
								RelativeLayout.LayoutParams.WRAP_CONTENT);

						// Add your rules
						layout.addRule(RelativeLayout.CENTER_IN_PARENT);
						v.setLayoutParams(layout);
						v.setFillDirection(FillDirection.HORIZONTAL);
					} else {
						RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
								RelativeLayout.LayoutParams.WRAP_CONTENT,
								RelativeLayout.LayoutParams.MATCH_PARENT);

						// Add your rules
						layout.addRule(RelativeLayout.CENTER_IN_PARENT);
						v.setLayoutParams(layout);
						
						v.setFillDirection(FillDirection.VERTICAL);
					}
				}
			});

			mGifView.setUrl(url);

			mImageView.setImageBitmap(null);
		} else {
			mGifView.setVisibility(View.GONE);
			mImageView.setVisibility(View.VISIBLE);

			mGifView.setImageBitmap(null);

			ImageSize size = new ImageSize(screenWidth, screenHeight);

			ImageLoader.getInstance().loadImage(url, size,
					new ImageLoadingListener() {

						@Override
						public void onLoadingStarted(String arg0, View arg1) {
							setIsLoading(true);
						}

						@Override
						public void onLoadingFailed(String arg0, View arg1,
								FailReason reason) {
							setIsLoading(false);
							switch (reason.getType()) {
							case DECODING_ERROR:
								break;
							case IO_ERROR:
								break;
							case NETWORK_DENIED:
								break;
							case OUT_OF_MEMORY:
								Util.log("Out of memory on " + url);
								break;
							case UNKNOWN:
								break;
							default:
								break;
							}
						}

						@Override
						public void onLoadingComplete(String imageUri,
								View view, Bitmap bm) {
							setIsLoading(false);
							mImageView.setImageBitmap(bm);

							// attacher Photoviewer for zooming
							// if (mAttacher == null) {
							// mAttacher = new PhotoViewAttacher(mImageView);
							// } else {
							// mAttacher.update();
							// }
						}

						@Override
						public void onLoadingCancelled(String arg0, View arg1) {
							setIsLoading(false);

						}
					});
		}

		return view;
	}

	private void setIsLoading(boolean loading) {
		if (loading) {
			mImagesContainer.setVisibility(View.INVISIBLE);
			mProgress.setVisibility(View.VISIBLE);
		} else {
			mImagesContainer.setVisibility(View.VISIBLE);
			mProgress.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
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

		if (mImageView != null) {
			Drawable drawable = mImageView.getDrawable();
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