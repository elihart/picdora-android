package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.player.ChannelViewActivity_;

@EBean
public class ChannelHelper {
	@RootContext
	Context context;

	public static void playChannel(Channel channel, Activity activity) {
		Intent intent = new Intent(activity, ChannelViewActivity_.class);
		intent.putExtra("channel", Util.toJson(channel));
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
				+ ChannelHelper.getCategoryIdsString(channel);

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
			ids.add(cat.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}

}
