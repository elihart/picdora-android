package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.json.JSONException;

import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.picdora.ImageManager.OnResultListener;
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
	 * Get the number of images on the server that can be used in this channel
	 * 
	 * @param channel
	 */
	public static void getServerImageCount(Channel channel,
			final OnImageCountReadyListener listener) {
		RequestParams params = new RequestParams();

		// set the categories to include
		params.put("category_ids", getCategoryIdsList(channel));

		// set the gif setting. Leave it blank if gifs should be included,
		// false if we don't want them, and true if we only want gifs
		switch (channel.getGifSetting()) {
		case ONLY:
			params.put("gif", true);
			break;
		case NONE:
			params.put("gif", false);
			break;
		case ALLOWED:
			// don't put anything
			break;
		default:
			break;
		}

		PicdoraApiClient.get("/images/count", params,
				new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(org.json.JSONObject response) {

						int count;
						try {
							count = response.getInt("count");
							listener.onReady(count);
						} catch (JSONException e) {
							listener.onReady(null);
						}

					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {

						listener.onReady(null);
					}
				});
	}

	/**
	 * Get the category ids for the categories in this channel
	 * 
	 * @param channel
	 * @return A list of the category ids
	 */
	private static List<String> getCategoryIdsList(Channel channel) {
		List<String> ids = new ArrayList<String>();
		for (Category cat : channel.getCategories()) {
			ids.add(Integer.toString(cat.getId()));
		}
		return ids;
	}

	public interface OnImageCountReadyListener {
		void onReady(Integer count);
	}

	/**
	 * Get the number of images in the local database that can be used in this
	 * channel
	 * 
	 * @param channel
	 * @param unseen
	 *            Whether or not to just count images where view count is 0
	 */
	public static long getLocalImageCount(Channel channel, boolean unseen) {
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

	public static void getChannelImagesFromServer(Channel channel, int limit,
			final OnImageRequestReady listener) {

		// get gif setting
		Boolean gif;
		switch (channel.getGifSetting()) {
		case ONLY:
			gif = true;
			break;
		case NONE:
			gif = false;
			break;
		case ALLOWED:
			gif = null;
			break;
		default:
			gif = null;
			break;
		}

		ImageManager.getImagesFromServer(limit, getCategoryIdsList(channel),
				gif, new OnResultListener() {

					@Override
					public void onSuccess() {
						listener.onReady(true);
					}

					@Override
					public void onFailure() {
						listener.onReady(false);
					}
				});
	}

	// callback for an image request to the server
	public interface OnImageRequestReady {
		public void onReady(boolean successful);
	}

}
