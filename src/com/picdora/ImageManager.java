package com.picdora;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.ManyQuery;
import se.emilsjolander.sprinkles.Query;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ImageManager {
	private ArrayList<Image> mImages;

	public ImageManager() {
		mImages = new ArrayList<Image>();
	}

	public Image getImage(int index) {
		if (index >= mImages.size()) {
			loadImageBatchFromDb(index - mImages.size() + 1, mImages);
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
		String query = "SELECT * FROM Images ORDER BY viewCount ASC LIMIT "
				+ Integer.toString(count);

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		images.addAll(list.asList());
		list.close();
	}

	public static void getImagesFromServer(int count) {
		AsyncHttpClient client = new AsyncHttpClient();

		RequestParams params = new RequestParams();
		params.put("count", Integer.toString(count));
		client.get("http://192.241.185.45:3000/images/random", params,
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
				boolean success = image.save();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
