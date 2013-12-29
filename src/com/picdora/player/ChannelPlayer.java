package com.picdora.player;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import android.text.TextUtils;

import com.picdora.ChannelHelper;
import com.picdora.ImageManager;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.models.Channel.GifSetting;

public class ChannelPlayer {
	private Channel mChannel;
	private List<Image> mImages;

	// the number of images to start the channel with. If these are all run
	// through then more can be retrieved
	private static final int STARTING_IMAGE_COUNT = 50;
	// The threshold for the number of unseen pictures left in this channel
	// before we try to retrieve more from the database
	private static final int IMAGE_UPDATE_THRESHOLD = 25;

	public ChannelPlayer(Channel channel, OnReadyListener listener) {
		mChannel = channel;
		mImages = new ArrayList<Image>();
		
		// TODO: Async?
		int numImagesLoaded = loadImageBatchFromDb(STARTING_IMAGE_COUNT, mImages);
		
		// if we don't have enough local images for this channel retrieve some from the database
		if(numImagesLoaded < IMAGE_UPDATE_THRESHOLD){
			//ImageManager.getImagesFromServer(limit, categoryIds, gif)
		} else {
			listener.onReady();
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

}
