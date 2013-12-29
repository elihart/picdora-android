package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
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
		params.put("category_ids", getCategoryIds(channel));

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
							count = response.getInt("key");
							listener.onSuccess(count);
						} catch (JSONException e) {
							listener.onFailure("Json error");
						}
						
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {
						
						listener.onFailure("Request failed");
					}
				});
	}

	/**
	 * Get the category ids for the categories in this channel
	 * 
	 * @param channel
	 * @return A list of the category ids
	 */
	private static List<Integer> getCategoryIds(Channel channel) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Category cat : channel.getCategories()) {
			ids.add(cat.getId());
		}
		return ids;
	}

	public interface OnImageCountReadyListener {
		public void onSuccess(int count);
		public void onFailure(String errorMsg);
	}

	/**
	 * Get the number of images in the local database that can be used in this
	 * channel
	 * 
	 * @param channel
	 */
	public static int getLocalImageCount(Channel channel) {
		// TODO:
		// SQLiteStatement s = mDb.compileStatement(select count(*) from users
		// where uname='"+loginname+ "' and pwd='"+loginpass+"');";
		//
		// long count = s.simpleQueryForLong();
		return 0;

	}
}
