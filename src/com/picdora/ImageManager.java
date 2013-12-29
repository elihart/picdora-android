package com.picdora;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Transaction;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.picdora.models.Category;
import com.picdora.models.Image;

public abstract class ImageManager {

	/**
	 * Get the specified number of images from the database and load them into
	 * the array
	 * 
	 * @param count
	 */
	private void loadImageBatchFromDb(int count, ArrayList<Image> images) {
		String query = "SELECT * FROM Images ORDER BY viewCount ASC, redditScore DESC LIMIT "
				+ Integer.toString(count);
		// String query =
		// "SELECT * FROM Images WHERE nsfw=0 AND gif=1 ORDER BY redditScore DESC";

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		images.addAll(list.asList());
		list.close();
	}

	/**
	 * Get images from every category that we have. Includes gifs indiscriminately
	 * @param limit The number of images to try to get from each category. May be less if the category doesn't have enough images
	 */
	public static void getImagesFromServer(int limit) {
		// TODO: Efficient raw query to just get category ids
		CursorList<Category> list = Query.many(Category.class,
				"SELECT * FROM Categories", null).get();
		
		List<Integer> ids = new ArrayList<Integer>();
		for (Category cat : list.asList()) {
			ids.add(cat.getId());
		}
		
		getImagesFromServer(limit, ids, null);
	}

	/**
	 * Get more images from the server, excluding images that we already have locally
	 * @param limit The number of images to attempt to get. May retrieve less than this if we don't have this many unique images on the server
	 * @param categoryIds The ids of the categories that the images should come from
	 * @param gif Whether or not to retrieve gifs. false for no, true if we only want gifs, and null to mix gifs with images indiscriminately
	 */
	public static void getImagesFromServer(int limit,
			final List<Integer> categoryIds, Boolean gif) {
		// get list of images that we already have that we don't want the server
		// to give us again
		// TODO: Efficient raw query to get image ids
		Date start = new Date();
		List<Integer> exclude = new ArrayList<Integer>();
		// TODO: Fix this to allow multiple ids
		CursorList<Image> list = Query.many(Image.class,
				"SELECT id FROM Images WHERE categoryId=" + categoryIds, null)
				.get();
		List<Image> imageList = list.asList();
		for (Image img : imageList) {
			exclude.add(img.getId());
		}
		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		Util.log("Getting image exclude list took " + duration);

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
						saveImagesToDb(response);
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							java.lang.String responseBody, java.lang.Throwable e) {
						Util.log("Get images failed");
					}
				});
	}

	public static void getCategoriesFromServer() {
		PicdoraApiClient.get("categories",
				new JsonHttpResponseHandler() {

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
	 * Parse a json array of Images and save them to the database
	 * 
	 * @param json
	 * @param images
	 */
	private static void saveImagesToDb(JSONArray json) {
		Transaction t = new Transaction();
		try {
			int numImages = json.length();
			for (int i = numImages - 1; i >= 0; i--) {
				Image image = new Image(json.getJSONObject((i)));
				image.save(t);
			}
			t.setSuccessful(true);
		} catch (Exception e) {
			e.printStackTrace();
			t.setSuccessful(false);
		} finally {
			t.finish();
		}
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
}
