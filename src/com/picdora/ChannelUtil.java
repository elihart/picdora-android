package com.picdora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;
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
import com.picdora.models.Channel.GifSetting;
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;

@EBean
public class ChannelUtil {
	@RootContext
	Context context;

	/**
	 * Launch the ChannelViewActivity with the given channel. Update the last
	 * played time to now and save the channel asynchronously.
	 * 
	 * @param channel
	 *            The channel to play.
	 * @param activity
	 *            The activity context to launch the ChannelViewActivity from.
	 * @param updateLastPlayedTime
	 *            Whether the channel should be saved and it's Last Used field
	 *            updated to now. Save is asynchronous, so watch out for race
	 *            conditions with the View activity getting updated data,
	 *            specifically the id being set on save callback when it is
	 *            created.
	 */
	public static void playChannel(Channel channel, Activity activity,
			boolean updateLastPlayedTime) {
		if (channel == null) {
			throw new IllegalArgumentException("Channel can't be null");
		}

		if (updateLastPlayedTime) {
			channel.setLastUsed(new Date());
			channel.saveAsync();
		}

		Intent intent = new Intent(activity, ChannelViewActivity_.class);
		intent.putExtra("channel", Util.toJson(channel));
		// only allow one instance of channel view running
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
	}

	/**
	 * Get the number of images in the local database that can be used in this
	 * channel.
	 * 
	 * @param channel
	 */
	public static long getImageCount(GifSetting gifSetting,
			List<Category> categories) {
		/* TODO: Need to test this since it's been changed. 
		 * 
		 */
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "SELECT count(distinct id) FROM ImagesWithCategories WHERE categoryId IN "
				+ CategoryUtils.getCategoryIdsString(categories);

		// add the gif setting
		switch (gifSetting) {
		case ALLOWED:
			break;
		case NONE:
			query += " AND gif=0";
			break;
		case ONLY:
			query += " AND gif=1";
			break;
		}

		SQLiteStatement s = db.compileStatement(query);

		return s.simpleQueryForLong();
	}

	/**
	 * Check if a channel name is in use, case insensitive
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isNameTaken(String name) {
		String query = "SELECT count(*) FROM Channels WHERE name = '" + name
				+ "'  COLLATE NOCASE";
		
		long result = DbUtils.simpleQueryForLong(query, 0);
		if(result == 0){
			return false;
		} else {
			return true;
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
		String query = "SELECT count(*) FROM Views WHERE channelId="
				+ channel.getId();

		return (int) DbUtils.simpleQueryForLong(query, 0);
	}

	/**
	 * Get all liked images from the given channels.
	 * 
	 * @param channels
	 * @return
	 */
	public static List<Image> getLikedImages(List<Channel> channels) {
		/*
		 * TODO: Should we exclude deleted images? On the one hand the user
		 * might not like having them disappear without any warning, and on the
		 * other hand they might not like seeing the blank image and having to
		 * delete it manually. For now let's let them deal with it.
		 */
		
		// TODO: Test!

		String query = "SELECT * FROM Images WHERE id IN (SELECT imageId FROM Views WHERE liked="
				+ ChannelImage.LIKE_STATUS.LIKED.getId()
				+ " AND channelId IN "
				+ getChannelIds(channels) + ")";

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		List<Image> images = list.asList();
		list.close();

		/*
		 * Remove duplicates by creating set first. TODO: Remove this when we
		 * get uniqueness forced in the db.
		 */
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
	 * channels. This will change their status to neutral.
	 * 
	 * @param channels
	 *            The channels to remove the likes from.
	 * @param images
	 *            The images to be removed from the liked set.
	 */
	public static void deleteLikes(List<Channel> channels, List<Image> images) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "UPDATE Views SET liked="
				+ ChannelImage.LIKE_STATUS.NEUTRAL.getId()
				+ " WHERE channelId IN " + getChannelIds(channels)
				+ " AND imageId IN " + ImageUtil.getImageIds(images);

		db.compileStatement(query).execute();
	}

	/**
	 * Delete all channels in the list asynchronously.
	 * 
	 * @param channels
	 */
	@Background
	public void deleteChannels(List<Channel> channels) {
		Transaction t = new Transaction();
		for (Channel c : channels) {
			c.delete(t);
		}
		t.setSuccessful(true);
		t.finish();
	}
}
