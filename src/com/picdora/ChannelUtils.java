package com.picdora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.picdora.channelDetail.ChannelDetailActivity_;
import com.picdora.channelPlayer.ChannelViewActivity_;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;

@EBean
public class ChannelUtils {
	@RootContext
	Context context;

	/**
	 * Launch the ChannelViewActivity with the given channel.
	 * 
	 * @param channel
	 *            The channel to play.
	 * @param activity
	 *            The activity context to launch the ChannelViewActivity from.
	 * @param save
	 *            Whether the channel should be saved and it's Last Used field
	 *            updated to now. Save is synchronous!
	 */
	public static void playChannel(Channel channel, Activity activity,
			boolean save) {
		if (channel == null) {
			throw new IllegalArgumentException("Channel can't be null");
		}

		if (save) {
			channel.setLastUsed(new Date());
			channel.save();
		}

		Intent intent = new Intent(activity, ChannelViewActivity_.class);
		intent.putExtra("channel", Util.toJson(channel));
		// only allow one instance of channel view running
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
	}

	/**
	 * Get the number of images in the local database that can be used in this
	 * channel
	 * 
	 * @param channel
	 * @param unseen
	 *            Whether or not to just count images where view count is 0
	 */
	public static long getImageCount(Channel channel, boolean unseen) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "SELECT count(*) FROM Images WHERE categoryId IN "
				+ ChannelUtils.getCategoryIdsString(channel);

		// add the gif setting
		switch (channel.getGifSetting()) {
		case ALLOWED:
			break;
		case NONE:
			query += " AND gif=0";
			break;
		case ONLY:
			query += " AND gif=1";
			break;
		}

		if (unseen) {
			query += " AND viewCount=0";
		}

		SQLiteStatement s = db.compileStatement(query);

		return s.simpleQueryForLong();
	}

	/**
	 * get a comma separated list of categories ids for use in a sql query
	 * 
	 * @return
	 */
	public static String getCategoryIdsString(Channel channel) {
		List<Category> categories = channel.getCategories();

		List<Integer> ids = new ArrayList<Integer>();
		for (Category cat : categories) {
			ids.add((int) cat.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}

	/**
	 * Check if a channel name is in use, case insensitive
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isNameTaken(String name) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "SELECT count(*) FROM Channels WHERE name = '" + name
				+ "'  COLLATE NOCASE";

		SQLiteStatement s = db.compileStatement(query);

		try {
			return s.simpleQueryForLong() > 0;
		} catch (SQLiteDoneException e) {
			return false;
		}
	}

	public static List<Channel> getAllChannels(boolean includeNsfw) {
		List<Channel> channels = new ArrayList<Channel>();
		String query = "SELECT * FROM Channels";

		if (!includeNsfw) {
			query += " WHERE nsfw=0";
		}

		CursorList<Channel> list = Query.many(Channel.class, query, null).get();
		channels.addAll(list.asList());
		list.close();

		return channels;
	}

	public static void sortChannelsAlphabetically(List<Channel> channels) {
		Collections.sort(channels, new ChannelAlphabeticalComparator());

	}

	/**
	 * Basic comparator to sort channels alphabetically by name
	 * 
	 */
	private static class ChannelAlphabeticalComparator implements
			Comparator<Channel> {
		public int compare(Channel left, Channel right) {
			return left.getName().toLowerCase()
					.compareTo(right.getName().toLowerCase());
		}
	}

	public static void showChannelDetail(Channel channel, Activity activity) {
		Intent intent = new Intent(activity, ChannelDetailActivity_.class);
		intent.putExtra("channel", Util.toJson(channel));
		activity.startActivity(intent);
	}

	public static int getNumImagesViewed(Channel channel) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "SELECT count(*) FROM Views WHERE channelId="
				+ channel.getId();

		SQLiteStatement s = db.compileStatement(query);

		return (int) s.simpleQueryForLong();
	}

	/**
	 * Get all liked images from the given channels.
	 * 
	 * @param channels
	 * @return
	 */
	public static List<Image> getLikedImages(List<Channel> channels) {
		String query = "SELECT * FROM Images WHERE imgurId IN (SELECT image FROM Views WHERE liked="
				+ ChannelImage.LIKE_STATUS.LIKED.ordinal()
				+ " AND channelId IN " + getChannelIds(channels) + ")";

		CursorList<Image> list = Query.many(Image.class, query, null).get();

		List<Image> images = new ArrayList<Image>();
		images.addAll(list.asList());
		list.close();

		// remove duplicates by creating set first
		return new ArrayList<Image>(new LinkedHashSet<Image>(images));
	}

	/**
	 * Create a parenthesized, comma separated list of the ids of the given
	 * channels for use in db queries.
	 * 
	 * @param channels
	 * @return Id list - "(1,2,3)"
	 */
	public static String getChannelIds(List<Channel> channels) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Channel c : channels) {
			ids.add((int) c.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}

	/**
	 * Remove the given images from the set of Liked images in the given
	 * channels.
	 * 
	 * @param channels The channels to remove the likes from.
	 * @param images The images to be removed from the liked set.
	 */
	public static void deleteLikes(List<Channel> channels, List<Image> images) {
		// TODO 

	}
}
