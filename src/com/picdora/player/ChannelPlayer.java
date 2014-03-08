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

	// whether we are currently loading images in the background
	private boolean loadingImagesInBackground;

	// a reserve of images used to replace deleted ones
	private LinkedList<Image> imageQueue;
	// the size that we'll try to keep the image queue at so we have enough
	// images without doing too many loads
	private static final int TARGET_QUEUE_SIZE = 15;

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
		imageQueue = new LinkedList<Image>();
		loadImageBatch(TARGET_QUEUE_SIZE * 2, imageQueue);

		loadChannelCompleted();
	}

	@UiThread
	public void loadChannelCompleted() {
		if (mListener == null) {
			return;
		}

		if (mImages.isEmpty() && imageQueue.isEmpty()) {
			mListener.onFailure(ChannelError.NO_IMAGES);
		} else {
			mListener.onSuccess();
		}
	}

	Channel getChannel() {
		return mChannel;
	}

	public interface OnGetImageResultListener {
		public void onGetImageResult(Image image);
	}

	@Background
	public void getImage(int index, boolean replace,
			OnGetImageResultListener listener) {
		// can't replace an image we haven't loaded yet
		if (index >= mImages.size()) {
			replace = false;
		}

		if (replace) {
			Image replacement = nextImage();
			if (replacement != null) {
				mImages.set(index, replacement);
			}
			if (listener != null) {
				giveResult(mImages.get(index), listener);
			}
			return;
		}

		while (mImages.size() <= index) {
			Image img = nextImage();
			if (img == null) {
				break;
			} else {
				mImages.add(img);
			}
		}

			giveResult(mImages.get(index % mImages.size()), listener);
	}
	
	@UiThread
	protected void giveResult(Image image, OnGetImageResultListener listener){
		if(listener != null){
			listener.onGetImageResult(image);
		}
	}

	private boolean allImagesUsed = false;

	private synchronized Image nextImage() {
		if (!allImagesUsed && imageQueue.size() < TARGET_QUEUE_SIZE) {
			int numToLoad = TARGET_QUEUE_SIZE * 2;
			int numLoaded = loadImageBatch(TARGET_QUEUE_SIZE * 2, imageQueue);
			if (numLoaded < numToLoad) {
				allImagesUsed = true;
			}
		}

		return imageQueue.poll();
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
			if (!mImages.contains(image) && !imageQueue.contains(image)) {
				images.add(image);
				resultCount++;
			}
		}
		list.close();

		return resultCount;
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
