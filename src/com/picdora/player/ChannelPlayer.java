package com.picdora.player;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.api.BackgroundExecutor;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.picdora.ChannelHelper;
import com.picdora.ChannelHelper.OnGetImageReadyListener;
import com.picdora.ChannelHelper.OnImageCountReadyListener;
import com.picdora.PicdoraApiClient;
import com.picdora.models.Channel;
import com.picdora.models.Image;

@EBean
public class ChannelPlayer {
	private Channel mChannel;
	private List<Image> mImages;
	private long mNumLocalImages;
	private OnReadyListener mListener;
	private long mNumServerImages;

	// the number of images to start the channel with. If these are all run
	// through then more can be retrieved
	private static final int STARTING_IMAGE_COUNT = 50;
	// The threshold for the number of unseen pictures left in this channel
	// before we try to retrieve more from the database
	private static final int IMAGE_UPDATE_THRESHOLD = 25;
	// the number of images to retrieve in a batch from the server
	private static final int IMAGE_BATCH_SIZE_FROM_SERVER = 1000;

	public ChannelPlayer() {
		// empty constructor for enhanced class
	}

	public void loadChannel(Channel channel, OnReadyListener listener) {
		mListener = listener;
		loadChannelAsync(channel);
	}

	@Background(id = "load_channel")
	void loadChannelAsync(final Channel channel) {
		mChannel = channel;
		mImages = new ArrayList<Image>();

		// figure out how many images we already have locally
		mNumLocalImages = ChannelHelper.getLocalImageCount(channel, false);

		// get the max number of images available on the server that can be used
		// for this channel
		ChannelHelper.getServerImageCount(channel,
				new OnImageCountReadyListener() {

					@Override
					public void onSuccess(int count) {
						// if we got destroyed while this was loading the
						// listener will be null, so just return and don't do
						// anything more
						if (mListener == null) {
							return;
						}

						mNumServerImages = count;
						// if the server doesn't have any images then we have
						// nothing to show
						if (mNumServerImages == 0) {
							loadFinished(false, ChannelError.NO_IMAGES);
							return;
						}

						// if we need more images locally then retrieve some
						// from the server if there are more available
						if (mNumLocalImages < IMAGE_UPDATE_THRESHOLD
								&& mNumServerImages > mNumLocalImages) {
							ChannelHelper.getChannelImagesFromServer(channel,
									IMAGE_BATCH_SIZE_FROM_SERVER,
									new OnGetImageReadyListener() {

										@Override
										public void onSuccess() {
											if (mListener != null) {
												loadImageBatchFromDb(
														STARTING_IMAGE_COUNT,
														mImages);
												loadFinished(true, null);
											}
										}

										@Override
										public void onFailure() {
											if (mListener != null) {
												// if we failed to get more
												// images but we still have some
												// locally that we can show,
												// just use those. Otherwise
												// error
												if (mNumLocalImages > 0) {
													loadImageBatchFromDb(
															STARTING_IMAGE_COUNT,
															mImages);
													loadFinished(true, null);
												} else {
													loadFinished(
															false,
															ChannelError.SERVER_ERROR);
												}
											}

										}

									});
						} else {
							loadFinished(true, null);
						}
					}

					@Override
					public void onFailure(String errorMsg) {
						loadFinished(false, ChannelError.SERVER_ERROR);
					}
				});

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
	 * the array
	 * 
	 * @param count
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

	public enum ChannelError {
		NO_IMAGES, SERVER_ERROR
	}

	/**
	 * Clean up all resources related to this player
	 */
	public void destroy() {
		// set listener to null so any background threads that end won't do their callbacks
		mListener = null;

		// cancel the loading background thread if it is in progress
		BackgroundExecutor.cancelAll("load_channel", true);
	}

}
