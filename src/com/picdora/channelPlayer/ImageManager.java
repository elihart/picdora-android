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
import se.emilsjolander.sprinkles.Sprinkles;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.picdora.CategoryUtils;
import com.picdora.PicdoraPreferences_;
import com.picdora.Timer;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;

/**
 * Decides what images to show for a channel.
 * 
 */
@EBean
public class ImageManager {
	// TODO: Explore loading all images into cursorlist initially and then
	// pulling them from there. Could be a lot more efficient

	// TODO: Don't go in straight order of reddit score. Take different
	// categories into account and likes (in the future)

	@Pref
	protected PicdoraPreferences_ mPrefs;

	private Channel mChannel;
	private List<ChannelImage> mImages;

	// a reserve of images used to replace deleted ones
	private LinkedList<ChannelImage> imageQueue;
	/**
	 * Whether all images available to this channel have been loaded already. If
	 * true we can stop trying to load more.
	 */
	private boolean mAllImagesUsed = false;
	/** List of all image ids that have been loaded so far. */
	private List<Integer> mImageIds;
	/**
	 * The size that we'll try to keep the image queue at so we have enough
	 * images without doing too many loads.
	 * 
	 */
	private static final int TARGET_QUEUE_SIZE = 5;
	/**
	 * How low the queue count can go before we try to refill it to the target
	 * size.
	 */
	private static final int QUEUE_REFILL_THRESHOLD = TARGET_QUEUE_SIZE / 2;

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
			mImages = new ArrayList<ChannelImage>(TARGET_QUEUE_SIZE);
			imageQueue = new LinkedList<ChannelImage>();
			mImageIds = new ArrayList<Integer>(TARGET_QUEUE_SIZE);
			// get the initial images to display
			loadImageBatch(TARGET_QUEUE_SIZE, imageQueue);

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
	 *            True if we should replace the image at the given index with a
	 *            new one, for the case where the first image was bad.
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
			/*
			 * Try to get another image to use as a replacement. If successful
			 * swap it out with the current image at the index. If getting
			 * another image fails then keep the current one.
			 */
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

	/**
	 * Get the next image to show. Can return null if there are no images left
	 * to use.
	 * 
	 * @return
	 */
	private synchronized ChannelImage nextImage() {
		// if we don't have any images left in the queue and we still have
		// unused images in the db then refill the queue
		if (imageQueue.size() < QUEUE_REFILL_THRESHOLD && !mAllImagesUsed) {
			int numToLoad = TARGET_QUEUE_SIZE;
			int numLoaded = loadImageBatch(numToLoad, imageQueue);
			// if the db can't load as many as we wanted then we already have
			// all the images it can give us, don't bother trying to get more as
			// we'll only get the same ones
			if (numLoaded < numToLoad) {
				mAllImagesUsed = true;
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
	private synchronized int loadImageBatch(int count,
			Collection<ChannelImage> images) {
		Timer timer = new Timer();
		timer.start();

		// build the query. Start by only selecting images from categories that
		// this channel includes
		String imageIdsFromCategories = "(SELECT distinct imageId FROM ImageCategories WHERE categoryId IN "
				+ CategoryUtils.getCategoryIdsString(mChannel.getCategories())
				+ ")";

		/*
		 * The columns we want to get, combining the image data with the view
		 * data. For the images we can ignore deleted and reported as we only
		 * get images where that is false. We also don't need the creation or
		 * update times. For the views we want all columns, but expect null
		 * values for images that haven't been viewed yet. In that case we
		 * should initialize the view info to match the channel and image, have
		 * no views, and neutral liked status.
		 */
		String columns = String.format("id, imgurId, redditScore, nsfw, gif, "
				+ "ifnull(channelId, %d) as channelId, "
				+ "ifnull(imageId, Images.id) as imageId, "
				+ "ifnull(count, 0) as count, "
				+ "ifnull(lastSeen, 0) as lastSeen, "
				+ "ifnull(liked, %d) as liked", mChannel.getId(),
				ChannelImage.LIKE_STATUS.NEUTRAL.getId());

		// Get image and view data. Using outer join images without views will
		// have null view data.
		String query = "SELECT "
				+ columns
				+ " FROM Images LEFT OUTER JOIN Views ON Images.id=Views.imageId";

		// limit images to the categories in the channel
		query += " WHERE Images.id IN " + imageIdsFromCategories;

		// limit views to ones from this channel
		/*
		 * TODO: Incorporate view information from other channels as well so
		 * duplicate images aren't seen across channels.
		 */
		query += " AND (channelId=" + mChannel.getId()
				+ " OR channelId IS NULL)";
		// and not disliked
		// TODO: Check dislikes from other channels too!
		query += " AND (liked != " + ChannelImage.LIKE_STATUS.DISLIKED.getId()
				+ " OR liked IS NULL)";

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

		// add the nsfw setting
		if (!mPrefs.showNsfw().get()) {
			query += " AND nsfw=0";
		}

		// not reported or deleted
		query += " AND reported=0 AND deleted=0";

		// not one of the images we've already loaded
		query += " AND id NOT IN " + getImageIdsInUse();

		// order by view count and reddit score
		query += " ORDER BY count ASC, redditScore DESC";
		// add limit
		query += " LIMIT " + Integer.toString(count);

		/*
		 * Run query and parse result into views and images with sprinkles.
		 * Since our query specifies values for when a view is null we can
		 * safely instantiate a new view for each image using the cursorlists.
		 */
		SQLiteDatabase db = Sprinkles.getDatabase();
		Cursor c = db.rawQuery(query, null);
		CursorList<Image> imagesCursor = new CursorList<Image>(c, Image.class);
		CursorList<ChannelImage> viewsCursor = new CursorList<ChannelImage>(c,
				ChannelImage.class);

		/*
		 * Get each view and image. The view only has the image id at the moment
		 * so give it the image object as well. Store the image id to prevent
		 * getting it again and add it to the result list.
		 */
		int resultCount = imagesCursor.size();
		for (int i = 0; i < resultCount; i++) {
			ChannelImage view = viewsCursor.get(i);
			Image image = imagesCursor.get(i);
			view.setImage(image);
			images.add(view);
			mImageIds.add((int) image.getId());
		}
		// Util.log(DatabaseUtils.dumpCursorToString(c));

		imagesCursor.close();
		viewsCursor.close();

		timer.lap("got image batch");
		return resultCount;
	}

	/**
	 * Get a string of the image ids that have already been loaded, comma
	 * separated and in parenthesis.
	 * 
	 * @return
	 */
	private String getImageIdsInUse() {
		return "(" + TextUtils.join(",", mImageIds) + ")";
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
