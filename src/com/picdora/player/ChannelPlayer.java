package com.picdora.player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.picdora.ChannelHelper;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.Image;

@EBean
public class ChannelPlayer {
	// keep track of the last channel so we don't have to reload it
	protected static ChannelPlayer lastChannelPlayer;

	private Channel mChannel;
	// use a thread safe list for adding images in the background
	private Vector<Image> mImages;
	private OnLoadListener mListener;

	// the number of images to load from the database at a time
	private static final int DB_BATCH_SIZE = 15;
	// the threshold for when we start loading more images in the background
	private static final int NUM_IMAGES_LEFT_THRESHOLD = 10;
	// whether we are currently loading images in the background
	private boolean loadingImagesInBackground;

	protected ChannelPlayer() {
		// empty constructor for enhanced class
	}

	public static ChannelPlayer getCachedPlayer(Channel channel) {
		if (lastChannelPlayer != null
				&& channel.equals(lastChannelPlayer.getChannel())) {
			return lastChannelPlayer;
		} else {
			return null;
		}
	}

	public static Channel lastPlayedChannel() {
		if (lastChannelPlayer != null) {
			return lastChannelPlayer.getChannel();
		} else {
			return null;
		}
	}

	@Background
	public void loadChannel(Channel channel, OnLoadListener listener) {
		loadingImagesInBackground = false;
		mListener = listener;
		mChannel = channel;
		mImages = new Vector<Image>();
		loadImageBatch(DB_BATCH_SIZE, mImages);

		loadChannelCompleted();
	}

	@UiThread
	public void loadChannelCompleted() {
		if (mListener == null) {
			return;
		}

		if (mImages.isEmpty()) {
			mListener.onFailure(ChannelError.NO_IMAGES);
		} else {
			lastChannelPlayer = this;
			mListener.onSuccess();
		}
	}

	private Channel getChannel() {
		return mChannel;
	}

	public Image getImage(int index) {
		// if we are requesting a higher image index than has been loaded, load
		// enough images to meet the index
		if (index >= mImages.size()) {
			int imagesNeeded = index - mImages.size() + 1;
			// get the amount needed to reach the index, plus grab another batch
			// while we're at it
			loadImageBatch(imagesNeeded + DB_BATCH_SIZE, mImages);
		}
		// if we're getting low on images do a background load
		else if (mImages.size() - index < NUM_IMAGES_LEFT_THRESHOLD) {
			// don't start another load if one is already going
			if (!loadingImagesInBackground) {
				loadImageBatchAsync(DB_BATCH_SIZE, mImages);
			}
		}

		// if for some reason we still don't have enough images then wrap the
		// index around
		if (index >= mImages.size()) {
			index = index % mImages.size();
		}

		return mImages.get(index);
	}

	@Background(serial = "loadImagesInBackground")
	void loadImageBatchAsync(int count, Collection<Image> images) {
		loadingImagesInBackground = true;
		loadImageBatch(count, images);
		loadingImagesInBackground = false;
	}

	/**
	 * Get the specified number of images from the database and load them into
	 * the given list.
	 * 
	 * @return resultCount The number of images successfully loaded
	 */
	private synchronized int loadImageBatch(int count, Collection<Image> images) {
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
		
		// TODO: Add nsfw setting

		// set ordering and add limit
		query += " ORDER BY viewCount ASC, redditScore DESC LIMIT "
				+ Integer.toString(count);

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		int resultCount = 0;
		for (Image image : list.asList()) {
			// TODO: Figure out a better way to do this. We have to mark them as
			// viewed right now because otherwise we will pull them from the
			// database again. Maybe supply these ids to the db to avoid
			// instead, or keep a list of unviewed images
			image.markView();
			// don't add a duplicate image
			// TODO: Better way to manage duplicates in the db before we retrieve them
			if (!mImages.contains(image)) {
				mImages.add(image);
				resultCount++;
			}
		}
		list.close();

		return resultCount;
	}

	/**
	 * Callback methods for when the player is ready to start playing
	 * 
	 * @author Eli
	 * 
	 */
	public interface OnLoadListener {
		public void onSuccess();

		public void onFailure(ChannelError error);
	}

	public enum ChannelError {
		NO_IMAGES
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
