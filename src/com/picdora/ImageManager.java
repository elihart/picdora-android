package com.picdora;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.picdora.models.Category;
import com.picdora.models.Image;

public abstract class ImageManager {

	/**
	 * Get images from every category that we have. Includes gifs
	 * indiscriminately
	 * 
	 * @param limit
	 *            The number of images to try to get from each category. May be
	 *            less if the category doesn't have enough images
	 */
	public static void getImagesFromServer(int limit, OnResultListener listener) {
		// TODO: Efficient raw query to just get category ids
		CursorList<Category> list = Query.many(Category.class,
				"SELECT * FROM Categories", null).get();

		List<String> ids = new ArrayList<String>();
		for (Category cat : list.asList()) {
			ids.add(Integer.toString(cat.getId()));
		}

		getImagesFromServer(limit, ids, null, listener);
	}

	/**
	 * Get more images from the server, excluding images that we already have
	 * locally
	 * 
	 * @param limit
	 *            The number of images to attempt to get. May retrieve less than
	 *            this if we don't have this many unique images on the server
	 * @param categoryIds
	 *            The ids of the categories that the images should come from
	 * @param gif
	 *            Whether or not to retrieve gifs. false for no, true if we only
	 *            want gifs, and null to mix gifs with images indiscriminately
	 */
	public static void getImagesFromServer(int limit,
			final List<String> categoryIds, Boolean gif,
			final OnResultListener listener) {
		// get list of images that we already have that we don't want the server
		// to give us again
		List<String> exclude = getImageIdsInCategories(categoryIds);

		RequestParams params = new RequestParams();
		params.put("count", Integer.toString(limit));
		params.put("category_ids", categoryIds);
		params.put("exclude", exclude);

		// set the gif setting. Leave it blank if gifs should be included,
		// false if we don't want them, and true if we only want gifs
		if (gif == null) {
			// don't do anything and the server will include gifs by default
		} else if (gif.booleanValue()) {
			// get only gifs
			params.put("gif", true);
		} else {
			// exclude gifs entirely
			params.put("gif", false);
		}

		PicdoraApiClient.get("images/top", params,
				new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(org.json.JSONArray response) {
						boolean success = saveImagesToDb(response);
						if (success) {
							listener.onSuccess();
						} else {
							listener.onFailure();
						}
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {
						Util.log("Get images failed");
						listener.onFailure();
					}
				});
	}

	public static void getImagesFromServer(int start, int end,
			final OnServerResultListener listener) {

		RequestParams params = new RequestParams();
		params.put("start", Integer.toString(start));
		params.put("end", Integer.toString(end));

		PicdoraApiClient.get("images/range", params,
				new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(org.json.JSONArray response) {
						listener.onSuccess(response);
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {
						Util.log("Get images failed");
						listener.onFailure();
					}
				});
	}

	public static void getImageUpdates(int idIndex, long lastUpdated,
			Integer batchSize, final OnImageUpdateListener listener) {

		RequestParams params = new RequestParams();
		params.put("id", Integer.toString(idIndex));
		params.put("time", String.valueOf(lastUpdated));

		if (batchSize != null) {
			params.put("limit", batchSize.toString());
		}

		PicdoraApiClient.get("images/update", params,
				new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(org.json.JSONObject response) {
						listener.onSuccess(response);
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {
						// network or server failure
						listener.onFailure();
					}
				});
	}

	public static void getCategoriesFromServer() {
		PicdoraApiClient.get("categories", new JsonHttpResponseHandler() {

			@Override
			public void onSuccess(org.json.JSONArray response) {
				saveCategoriesToDb(response);
			}

			@Override
			public void onFailure(int statusCode,
					org.apache.http.Header[] headers,
					java.lang.String responseBody, java.lang.Throwable e) {
				Util.log("Get categories failed");
			}
		});
	}

	/**
	 * Get a list of image ids as strings for use in telling the server which
	 * ids to exclude
	 * 
	 * @param categoryIds
	 * @return
	 */
	private static List<String> getImageIdsInCategories(List<String> categoryIds) {
		List<String> ids = new ArrayList<String>();
		SQLiteDatabase db = Sprinkles.getDatabase();

		String idString = "(" + TextUtils.join(",", categoryIds) + ")";
		String selection = "categoryId IN " + idString;

		Cursor cursor = db.query("Images", new String[] { "id" }, selection,
				null, null, null, null);

		int index = cursor.getColumnIndex("id");
		while (cursor.moveToNext()) {
			int id = cursor.getInt(index);
			ids.add(Integer.toString(id));
		}

		cursor.close();

		return ids;
	}

	public static long getLastId() {
		SQLiteDatabase db = Sprinkles.getDatabase();
		final String query = "SELECT MAX(id) FROM Images";

		SQLiteStatement s = db.compileStatement(query);

		long result = 0;
		try {
			result = s.simpleQueryForLong();
		} catch (SQLiteDoneException ex) {
			// no result
		}

		return result;
	}

	/**
	 * Parse a json array of Images and save them to the database
	 * 
	 * @param json
	 * @param images
	 * @return Whether or not the images saved successfully
	 */
	public static boolean saveImagesToDb(JSONArray json) {
		Transaction t = new Transaction();
		boolean success = true;
		try {
			int numImages = json.length();
			for (int i = numImages - 1; i >= 0; i--) {
				Image image = new Image(json.getJSONObject((i)));
				image.save(t);
			}
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		} finally {
			t.setSuccessful(success);
			t.finish();
		}

		return success;
	}

	private static void saveCategoriesToDb(JSONArray json) {
		Transaction t = new Transaction();
		try {
			int numCategories = json.length();
			for (int i = numCategories - 1; i >= 0; i--) {
				Category cat = new Category(json.getJSONObject((i)));
				cat.save(t);
			}
			t.setSuccessful(true);
		} catch (Exception e) {
			Util.log("Exception thrown while saving categories");
			e.printStackTrace();
			t.setSuccessful(false);
		} finally {
			t.finish();
		}
	}

	public interface OnResultListener {
		public void onSuccess();

		public void onFailure();
	}

	public interface OnServerResultListener {
		public void onSuccess(JSONArray json);

		public void onFailure();
	}

	public interface OnImageUpdateListener {
		public void onSuccess(JSONObject json);

		public void onFailure();
	}
}
