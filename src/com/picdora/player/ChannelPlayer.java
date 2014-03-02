package com.picdora.player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
	// keep track of the last channel so we don't have to reload it 
	protected static ChannelPlayer lastChannelPlayer;

	private Channel mChannel;
	private List<Image> mImages;
	private Queue<Image> mUpcomingImages;
	private GetPlayerListener mListener;

	// the number of images to load from the database when we ne
	private static final int DB_BATCH_SIZE = 15;
	// the number of images to load at the beginning
	private static final int STARTING_BATCH_SIZE = 30;

	public ChannelPlayer() {
		// empty constructor for enhanced class
	}

	public static void getPlayer(Channel channel, GetPlayerListener listener) {
		if (channel.equals(lastChannelPlayer)) {
			listener.onSuccess(lastChannelPlayer);
		} else {
			lastChannelPlayer = new ChannelPlayer();
			lastChannelPlayer.loadChannel(channel, listener);
		}
	}

	@Background
	protected void loadChannel(Channel channel, GetPlayerListener listener) {
		mListener = listener;
		mChannel = channel;
		mImages = new ArrayList<Image>();
		mUpcomingImages = new LinkedList<Image>();
		loadImageBatch(STARTING_BATCH_SIZE, mImages);

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
			mListener.onSuccess(this);
		}
	}

	public Image getImage(int index) {
		// if we are requesting a higher image index than has been loaded, load
		// enough images to meet the index
		if (index >= mImages.size()) {
			loadImageBatch(index - mImages.size() + 1 + DB_BATCH_SIZE, mImages);
		} else {
			// check if we should proactively load more images before they are
			// needed

			// TODO: Load images in background. Right now though images are
			// loaded according to view count, and loading in background will
			// load duplicates

			// loadMoreImagesIfNeeded(index);
		}

		// if for some reason we still don't have enough images then wrap the
		// index around
		if (index >= mImages.size()) {
			index = index % mImages.size();
		}

		return mImages.get(index);
	}

	/**
	 * If the given index is close to the number of images that we have loaded
	 * we should proactively load more images in anticipation of needing them
	 * soon
	 * 
	 * @param index
	 *            The image index that is being accessed
	 */
	@Background(serial = "loadImagesInBackground")
	void loadMoreImagesIfNeeded(int index) {
		if (index - mImages.size() > DB_BATCH_SIZE) {
			loadImageBatch(index - mImages.size() + 1 + DB_BATCH_SIZE, mImages);
		}
	}

	/**
	 * Get the specified number of images from the database and load them into
	 * the given list
	 */
	private int loadImageBatch(int count, List<Image> images) {
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
		for (Image image : list.asList()) {
			image.markView();
			mImages.add(image);
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
	public interface GetPlayerListener {
		public void onSuccess(ChannelPlayer player);

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
