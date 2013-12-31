package com.picdora.player;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.api.BackgroundExecutor;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import android.app.Activity;

import com.picdora.ChannelHelper;
import com.picdora.ChannelHelper.OnImageRequestReady;
import com.picdora.ChannelHelper.OnImageCountReadyListener;
import com.picdora.PicdoraApiClient;
import com.picdora.models.Channel;
import com.picdora.models.Image;

@EBean
public class ChannelPlayer {
	@RootContext
	Activity activity;

	private Channel mChannel;
	private List<Image> mImages;
	private OnReadyListener mListener;
	private long mNumLocalImages;
	// The number of images on the server that can be used for this channel.
	// Null until we get in contact with the server
	private Integer mNumServerImages = null;

	// the number of images to load from the database at a time
	private static final int DB_BATCH_SIZE = 15;
	// The threshold for the number of unseen pictures left in this channel
	// before we try to retrieve more from the database
	private static final int IMAGE_UPDATE_THRESHOLD = 60;
	// the number of images to retrieve in a batch from the server
	private static final int IMAGE_BATCH_SIZE_FROM_SERVER = 300;

	public ChannelPlayer() {
		// empty constructor for enhanced class
	}

	public void loadChannel(Channel channel, OnReadyListener listener) {
		mListener = listener;
		loadChannelAsync(channel);
	}

	@Background
	void loadChannelAsync(final Channel channel) {
		mChannel = channel;
		mImages = new ArrayList<Image>();

		// the number of images in the local database that the user hasn't seen
		// yet
		long numLocalUnseenImages = ChannelHelper.getLocalImageCount(channel,
				true);

		// if we have enough unseen content then just start showing them
		if (numLocalUnseenImages > IMAGE_UPDATE_THRESHOLD) {
			loadImageBatchFromDb(DB_BATCH_SIZE, mImages);
			loadFinished(true, null);
			return;
		}

		// otherwise we need to worry about loading more images

		// figure out how many images we have in total locally
		mNumLocalImages = ChannelHelper.getLocalImageCount(channel, false);

		// get the max number of images available on the server that can be used
		// for this channel
		getServerImageCount(new OnImageCountReadyListener() {

			@Override
			public void onReady(Integer count) {
				mNumServerImages = count;
				afterServerImageCount();
			}
		});
	}

	@Background
	void afterServerImageCount() {
		// if we got destroyed while this was loading the
		// listener will be null, so just return and don't do
		// anything more
		if (mListener == null) {
			return;
		}

		// Next we attempt to get more images from the server

		// If we don't have an image count from the server don't try to get any.
		// If we have any local images we can use just show
		// those, otherwise report an error
		if (mNumServerImages == null) {
			if (mNumLocalImages > 0) {
				loadImageBatchFromDb(DB_BATCH_SIZE, mImages);
				loadFinished(true, null);
			} else {
				loadFinished(false, ChannelError.SERVER_ERROR);
			}
			return;
		}

		// if the server doesn't have any images and we don't
		// have any locally then we have nothing to show
		if (mNumServerImages == 0 && mNumLocalImages == 0) {
			loadFinished(false, ChannelError.NO_IMAGES);
			return;
		}

		// if the server doesn't have more images for us then
		// just use what we have locally
		if (mNumLocalImages >= mNumServerImages) {
			loadImageBatchFromDb(DB_BATCH_SIZE, mImages);
			loadFinished(true, null);
			return;
		}

		// Otherwise there are more images on the server, so
		// let's get them!
		requestImagesFromServer(new OnImageRequestReady() {

			@Override
			public void onReady(boolean successful) {
				handleImageRequestResult(successful);
			}
		});
	}

	/**
	 * Request images from the server
	 * 
	 * @param count
	 */
	@UiThread
	void requestImagesFromServer(OnImageRequestReady listener) {
		// must be run on the ui thread so the http library can work
		ChannelHelper.getChannelImagesFromServer(mChannel,
				IMAGE_BATCH_SIZE_FROM_SERVER, listener);
	}

	@UiThread
	void getServerImageCount(OnImageCountReadyListener onImageCountReadyListener) {
		// run this on the ui thread so that the http async library can work
		ChannelHelper.getServerImageCount(mChannel, onImageCountReadyListener);
	}

	@Background
	void handleImageRequestResult(boolean successful) {
		if (mListener == null) {
			return;
		}

		if (successful) {
			// get the new number of images in the
			// database
			mNumLocalImages = ChannelHelper.getLocalImageCount(mChannel, false);
			loadImageBatchFromDb(DB_BATCH_SIZE, mImages);
			loadFinished(true, null);
		} else {
			// if we failed to get more
			// images but we still have some
			// locally that we can show,
			// just use those. Otherwise
			// error
			if (mNumLocalImages > 0) {
				loadImageBatchFromDb(DB_BATCH_SIZE, mImages);
				loadFinished(true, null);
			} else {
				loadFinished(false, ChannelError.SERVER_ERROR);
			}
		}
	}

	// respond to the loading callback on the UI thread
	@UiThread
	void loadFinished(boolean successful, ChannelError error) {
		if (mListener != null) {
			if (successful) {
				mListener.onReady();
			} else {
				mListener.onError(error);
			}
		}
	}

	public Image getImage(int index) {
		if (index >= mImages.size()) {
			loadImageBatchFromDb(index - mImages.size() + 1 + 10, mImages);
		}

		return mImages.get(index);

	}

	/**
	 * Get the specified number of images from the database and load them into
	 * the given list
	 */
	private int loadImageBatchFromDb(int count, List<Image> images) {
		// build the query. Start by only selecting images from categories that
		// this channel includes
		String query = "SELECT * FROM Images WHERE categoryId IN "
				+ ChannelHelper.getCategoryIdsString(mChannel);

		// add the gif setting
		switch (mChannel.getGifSetting()) {
		case ALLOWED:
			break;
		case NONE:
			query += " AND gif=0";
			break;
		case ONLY:
			query += " AND gif=1";
			break;
		}

		// set ordering and add limit
		query += " ORDER BY viewCount ASC, redditScore DESC LIMIT "
				+ Integer.toString(count);

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		int resultCount = list.size();
		images.addAll(list.asList());
		list.close();

		return resultCount;
	}

	/**
	 * Callback methods for when the player is ready to start playing
	 * 
	 * @author Eli
	 * 
	 */
	public interface OnReadyListener {
		public void onReady();

		public void onError(ChannelError error);
	}

	public interface OnDbLoadListener {
		public void loadComplete();
	}

	public enum ChannelError {
		NO_IMAGES, SERVER_ERROR
	}

	/**
	 * Clean up all resources related to this player
	 */
	public void destroy() {
		// set listener to null so any background threads that end won't do
		// their callbacks
		mListener = null;
	}

}
