package com.picdora.channelPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.picdora.CategoryUtils;
import com.picdora.PicdoraPreferences_;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelPreview;
import com.picdora.models.Image;

@EBean
public class ChannelPlayer {
	// TODO: Explore loading all images into cursorlist initially and then
	// pulling them from there. Could be a lot more efficient

	// TODO: Don't go in straight order of reddit score. Take different
	// categories into account and likes (in the future)

	@Pref
	protected PicdoraPreferences_ mPrefs;

	private Channel mChannel;
	// and then decide on using it
	private List<ChannelImage> mImages;

	// a reserve of images used to replace deleted ones
	private LinkedList<ChannelImage> imageQueue;
	// the size that we'll try to keep the image queue at so we have enough
	// images without doing too many loads
	private static final int TARGET_QUEUE_SIZE = 15;

	// TODO: Don't mark replacements as viewed until they are used (or unmark
	// them if they are never used?)

	protected ChannelPlayer() {
		// empty constructor for enhanced class
	}

	/**
	 * Initialize the player with the given channel and start loading images to
	 * show.
	 * 
	 * @param channel
	 *            The channel to load that we are going to play.
	 * @param listener
	 *            Callback for when load completes
	 */
	@Background
	public void loadChannel(Channel channel, OnLoadListener listener) {
		ChannelError error = null;

		if (channel == null) {
			error = ChannelError.BAD_CHANNEL;
		} else {
			// init arrays and fields
			mChannel = channel;
			mImages = new ArrayList<ChannelImage>();
			imageQueue = new LinkedList<ChannelImage>();
			// get the initial images to display
			loadImageBatch(TARGET_QUEUE_SIZE * 2, imageQueue);

			// check that the load was able to populate the imageQueue
			if (mImages.isEmpty() && imageQueue.isEmpty()) {
				// If our lists are empty then this channel has no images to
				// display and we have nothing to show
				error = ChannelError.NO_IMAGES;
			}
		}

		// alert the listener on the ui thread of the result
		loadChannelCompleted(error, listener);
	}

	/**
	 * Return the result of loading the channel on the UI Thread.
	 * 
	 * @param error
	 *            If the load failed then this will be the cause of the fail. On
	 *            success this should be null.
	 * @param listener
	 *            The listener to alert. If null then no callback will be made.
	 */
	@UiThread
	protected void loadChannelCompleted(ChannelError error,
			OnLoadListener listener) {
		// if no listener was passed then we have nobody to alert
		if (listener == null) {
			return;
		}
		// if an error was passed then alert the failure
		else if (error != null) {
			listener.onFailure(error);
		}
		// No error. Success!
		else {
			listener.onSuccess();
		}
	}

	public Channel getChannel() {
		return mChannel;
	}

	public interface OnGetChannelImageResultListener {
		public void onGetChannelImageResult(ChannelImage image);
	}

	/**
	 * Get the image at the given index in a background thread.
	 * 
	 * @param index
	 *            The index of the image to retrieve
	 * @param replace
	 *            True if a we should replace the image at the given index with
	 *            a new one, for the case where the first image was bad.
	 * @param listener
	 *            Callback for when the image is ready
	 */
	@Background
	public void getImageAsync(int index, boolean replace,
			OnGetChannelImageResultListener listener) {
		ChannelImage result = getImage(index, replace);

		// return the image on the ui thread
		returnGetImageAsyncResult(result, listener);
	}

	@UiThread
	protected void returnGetImageAsyncResult(ChannelImage image,
			OnGetChannelImageResultListener listener) {
		if (listener != null) {
			listener.onGetChannelImageResult(image);
		}
	}

	/**
	 * Get the image at the given index on the ui thread. Careful, this might do
	 * a db access.
	 * 
	 * @param index
	 *            The index of the image to retrieve
	 * @param replace
	 *            True if a we should replace the image at the given index with
	 *            a new one, for the case where the first image was bad.
	 */
	public synchronized ChannelImage getImage(int index, boolean replace) {
		// can't replace an image we haven't loaded yet
		if (index >= mImages.size()) {
			replace = false;
		}

		ChannelImage result = null;

		// if they have requested a replacement then get a new image and replace
		// the old one
		if (replace) {
			ChannelImage replacement = nextImage();
			if (replacement != null) {
				mImages.set(index, replacement);
			}
			result = mImages.get(index);
		} else {
			// keep getting new images until either we have enough to satisfy
			// the index requested, or we don't have anymore to give
			while (mImages.size() <= index) {
				ChannelImage img = nextImage();
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

		return result;
	}

	private boolean allImagesUsed = false;

	private ChannelImage nextImage() {
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
	private int loadImageBatch(int count, Collection<ChannelImage> images) {
		// TODO: Test this with the new db schema changes
		
		// build the query. Start by only selecting images from categories that
		// this channel includes
		String imageIdsFromCategories = "(SELECT distinct imageId FROM ImageCategories WHERE categoryId IN "
				+ CategoryUtils.getCategoryIdsString(mChannel.getCategories()) + ")";
		
		String query = "SELECT * FROM Images WHERE id IN " + imageIdsFromCategories;

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

		if (!mPrefs.showNsfw().get() || !mChannel.isNsfw()) {
			query += " AND nsfw=0";
		}

		/* TODO: We need to reuse images at some point... */
		query += " AND id NOT IN (SELECT imageId FROM Views WHERE channelId="
				+ mChannel.getId() + ")";

		// set ordering and add limit
		query += " ORDER BY redditScore DESC LIMIT " + Integer.toString(count);

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		int resultCount = list.size();
		for (Image image : list.asList()) {
			// TODO: Figure out a better way to do this. We have to mark them as
			// viewed right now because otherwise we will pull them from the
			// database again. Maybe supply these ids to the db to avoid
			// instead, or keep a list of unviewed images

			// don't add a duplicate image
			// TODO: Better way to manage duplicates in the db before we
			// retrieve them
			ChannelImage channelImage = new ChannelImage(mChannel, image);
			if (!mImages.contains(channelImage)
					&& !imageQueue.contains(channelImage)) {
				// don't save if we're previewing.
				if (!ChannelPreview.isPreview(mChannel)) {
					channelImage.markView();
					channelImage.save();
				}
				images.add(channelImage);
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

	/** Error codes for loading a channel */
	public enum ChannelError {

		/** The channel doesn't contain any images to show */
		NO_IMAGES,
		/** The given channel is null or is non functional */
		BAD_CHANNEL;
	}
}
