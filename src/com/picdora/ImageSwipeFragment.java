package com.picdora;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import uk.co.senab.photoview.PhotoViewAttacher;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;

import net.frakbot.imageviewex.ImageViewEx.FillDirection;
import net.frakbot.imageviewex.ImageViewNext;
import net.frakbot.imageviewex.ImageViewNext.CacheLevel;
import net.frakbot.imageviewex.ImageViewNext.ImageLoadCompletionListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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

		// Give the screen size so images are scaled to save memory
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);

		// get max size for display
		int screenHeight = displaymetrics.heightPixels;
		int screenWidth = displaymetrics.widthPixels;
		final float screenAspectRatio = (float) screenWidth / screenHeight;

		mGifView.setMaxHeight(screenHeight);
		mGifView.setMaxWidth(screenWidth);

		mImageView.setMaxHeight(screenHeight);
		mImageView.setMaxWidth(screenWidth);

		// set url and load image
		final String url = image.getUrl();
		setIsLoading(true);

		if (image.isGif()) {
			mGifView.setVisibility(View.VISIBLE);
			mImageView.setVisibility(View.GONE);
			setIsLoading(true);

			mGifView.setLoadCallbacks(new ImageLoadCompletionListener() {

				@Override
				public void onLoadStarted(ImageViewNext v, CacheLevel level) {

				}

				@Override
				public void onLoadError(ImageViewNext v, CacheLevel level) {
					setIsLoading(false);
					Util.log("Error loading gif " + url);

				}

				@Override
				public void onLoadCompleted(ImageViewNext v, CacheLevel level) {
					setIsLoading(false);
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

						}

						@Override
						public void onLoadingFailed(String arg0, View arg1,
								FailReason reason) {
							setIsLoading(false);
							mImageView.setImageResource(R.drawable.ic_launcher);
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
							mAttacher = new PhotoViewAttacher(mImageView);

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
			Util.log("Showing loading");
			mImagesContainer.setVisibility(View.INVISIBLE);
			mProgress.setVisibility(View.VISIBLE);
		} else {
			Util.log("Hiding loading");
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