package com.picdora.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.picdora.ChannelHelper;
import com.picdora.models.Channel;
import com.picdora.models.Image;

@EBean
public class ChannelPlayer {

	private Channel mChannel;
	// and then decide on using it
	private List<Image> mImages;
	private OnLoadListener mListener;

	// a reserve of images used to replace deleted ones
	private LinkedList<Image> imageQueue;
	// the size that we'll try to keep the image queue at so we have enough
	// images without doing too many loads
	private static final int TARGET_QUEUE_SIZE = 15;

	// TODO: Don't mark replacements as viewed until they are used (or unmark
	// them if they are never used?)

	protected ChannelPlayer() {
		// empty constructor for enhanced class
	}

	@Background
	public void loadChannel(Channel channel, OnLoadListener listener) {
		mListener = listener;
		mChannel = channel;
		mImages = new ArrayList<Image>();
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

	public Channel getChannel() {
		return mChannel;
	}

	public interface OnGetImageResultListener {
		public void onGetImageResult(Image image);
	}

	@Background
	public void getImage(int index, boolean replace,
			OnGetImageResultListener listener) {
		// synchronize access so we don't have multiple loads going for the same
		// image. Also simplifies the threading and db access
		getImageSync(index, replace, listener);
		// TODO: Consider async options if performance is laggy
	}

	private synchronized void getImageSync(int index, boolean replace,
			OnGetImageResultListener listener) {
		// can't replace an image we haven't loaded yet
		if (index >= mImages.size()) {
			replace = false;
		}

		Image result = null;

		// if they have requested a replacement then get a new image and replace
		// the old one
		if (replace) {
			Image replacement = nextImage();
			if (replacement != null) {
				mImages.set(index, replacement);
			}
			result = mImages.get(index);
		} else {
			// keep getting new images until either we have enough to satisfy
			// the index requested, or we don't have anymore to give
			while (mImages.size() <= index) {
				Image img = nextImage();
				if (img == null) {
					break;
				} else {
					mImages.add(img);
				}
			}

			// return the index requested if we have enough images, otherwise
			// wrap
			// around
			result = mImages.get(index % mImages.size());
		}

		// return the image result on the ui thread
		returnGetImageResult(result, listener);
	}

	@UiThread
	protected void returnGetImageResult(Image image,
			OnGetImageResultListener listener) {
		if (listener != null) {
			listener.onGetImageResult(image);
		}
	}

	private boolean allImagesUsed = false;

	private Image nextImage() {
		// if we don't have any images left in the queue and we still have
		// unused images in the db then refill the queue
		if (imageQueue.size() < TARGET_QUEUE_SIZE && !allImagesUsed) {
			int numToLoad = TARGET_QUEUE_SIZE * 2;
			int numLoaded = loadImageBatch(numToLoad, imageQueue);
			// if the db can't load as many as we wanted then we already have
			// all the images it can give us, don't bother trying to get more as
			// we'll only get the same ones
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
	 * @return resultCount The number of images retrieved from the db
	 */
	private int loadImageBatch(int count, Collection<Image> images) {
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
		int resultCount = list.size();
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
