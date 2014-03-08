package com.picdora.player;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
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

	// TODO: On channels with low image counts there is lag as it tries to fetch
	// more images (that we don't have). Maybe get the image count on launch

	private Channel mChannel;
	// use a thread safe list for adding images in the background
	// TODO: With synchronized methods I'm not sure if we really need a
	// threadsafe list -> might want to test how much it degrades performance
	// and then decide on using it
	private Vector<Image> mImages;
	private OnLoadListener mListener;

	// the number of images to load from the database at a time
	private static final int DB_BATCH_SIZE = 15;
	// the threshold for when we start loading more images in the background
	private static final int NUM_IMAGES_LEFT_THRESHOLD = 10;
	// whether we are currently loading images in the background
	private boolean loadingImagesInBackground;
	// the maximum number of unique images in the db that can be used for this
	// channel
	private long channelImageCount;
	// a reserve of images used to replace deleted ones
	private LinkedList<Image> replacementImages;
	private static final int NUM_REPLACEMENT_IMAGES = 10;

	// TODO: Don't mark replacements as viewed until they are used (or unmark
	// them if they are never used?)

	// TODO: Use callbacks for getting images/replacements so loading is never
	// done on ui

	protected ChannelPlayer() {
		// empty constructor for enhanced class
	}

	@Background
	public void loadChannel(Channel channel, OnLoadListener listener) {
		loadingImagesInBackground = false;
		mListener = listener;
		mChannel = channel;
		mImages = new Vector<Image>();
		replacementImages = new LinkedList<Image>();
		channelImageCount = ChannelHelper.getImageCount(channel, false);
		loadImageBatch(NUM_REPLACEMENT_IMAGES, replacementImages);
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
			mListener.onSuccess();
		}
	}

	Channel getChannel() {
		return mChannel;
	}

	public synchronized Image getImage(int index) {
		// TODO: Check our channel image count and if we don't have enough then don't bother going to the db, just wrap the index around
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
			// TODO: Better way to manage duplicates in the db before we
			// retrieve them
			if (!mImages.contains(image) && !replacementImages.contains(image)) {
				images.add(image);
				resultCount++;
			}
		}
		list.close();

		return resultCount;
	}

	/**
	 * Get a replacement for an image in the list
	 * 
	 * @param position
	 * @return
	 */
	public synchronized Image getReplacementImage(int position) {
		// can't replace an image that we haven't loaded yet
		if (mImages.size() <= position) {
			throw new IndexOutOfBoundsException();
		} else {
			// make sure there are replacement images available to use, if not
			// try to load more.
			if (replacementImages.isEmpty()) {
				// TODO: use a listener
				Util.log("replacement empty");
				loadImageBatch(NUM_REPLACEMENT_IMAGES, replacementImages);				
			}

			Image img = replacementImages.poll();
			// return the original image if the list is empty
			if (img == null) {
				Util.log("empty queue");
				return mImages.get(position);
			} else {
				mImages.set(position, img);
				// if we're not already loading replacements see if we need to
				if (!loadingReplacements) {
					loadMoreReplacementsIfNeeded();
				}
				Util.log("returning replacement");
				return img;
			}
		}
	}

	private boolean loadingReplacements = false;

	@Background
	protected void loadMoreReplacementsIfNeeded() {
		Util.log("Loading replacements");

		loadingReplacements = true;
		if (replacementImages.size() < NUM_REPLACEMENT_IMAGES) {
			loadImageBatch(NUM_REPLACEMENT_IMAGES, replacementImages);
		}
		loadingReplacements = false;
	}

	/**
	 * Callback methods for when the player is ready to start playing
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
