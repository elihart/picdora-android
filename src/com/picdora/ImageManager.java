package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.picdora.models.Category;
import com.picdora.models.Image;

public class ImageManager {
	private ArrayList<Image> mImages;

	public ImageManager() {
		mImages = new ArrayList<Image>();
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
	private void loadImageBatchFromDb(int count, ArrayList<Image> images) {
		String query = "SELECT * FROM Images ORDER BY viewCount ASC, redditScore DESC LIMIT "
				+ Integer.toString(count);
		// String query =
		// "SELECT * FROM Images WHERE nsfw=0 AND gif=1 ORDER BY redditScore DESC";

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		images.addAll(list.asList());
		list.close();
	}
	
	public static void getImagesFromServer(int count){
		CursorList<Category> list = Query.many(Category.class, "SELECT * FROM Categories", null).get();
		for(Category cat : list.asList()){
			getImagesFromServer(count, cat.getId());
		}
	}

	public static void getImagesFromServer(int count, int categoryId) {
		AsyncHttpClient client = new AsyncHttpClient();
		
		ArrayList<Integer> exclude = new ArrayList<Integer>();
		CursorList<Image> list = Query.many(Image.class, "SELECT id FROM Images WHERE categoryId=" + categoryId, null).get();
		for(Image img : list.asList()){
			exclude.add(img.getId());
		}

		RequestParams params = new RequestParams();
		params.put("count", Integer.toString(count));
		params.put("category_id", Integer.toString(categoryId));
		params.put("exclude", exclude);
		client.get("http://picdora.com:3000/images/top", params,
				new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(org.json.JSONArray response) {
						Util.log(response.toString());
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
		AsyncHttpClient client= new AsyncHttpClient();

		client.get("http://picdora.com:3000/categories",
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
		int numImages = json.length();
		for (int i = 0; i < numImages; i++) {
			try {
				Image image = new Image(json.getJSONObject((i)));
				image.save();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void saveCategoriesToDb(JSONArray json) {
		int numCategories = json.length();
		for (int i = 0; i < numCategories; i++) {
			try {
				Category cat = new Category(json.getJSONObject((i)));
				cat.save();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
