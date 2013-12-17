package com.picdora;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ImageManager {
	private AsyncHttpClient mClient;
	private ArrayList<String> mImages;

	public ImageManager() {
		mClient = new AsyncHttpClient();

		mImages = new ArrayList<String>();

		retrieveMoreImages();
	}

	public String getImage(int index) {
		if(mImages.isEmpty()){
		 return "http://i.imgur.com/asnpvjM.gif";
		}
		else if (index < mImages.size()) {
		
			return mImages.get(index);
		} else {
			retrieveMoreImages();
			return mImages.get(mImages.size() - 1);
		}
	}

	private void retrieveMoreImages() {
		RequestParams params = new RequestParams();
		params.put("count", "20");
		mClient.get("http://www.google.com", params,
				new JsonHttpResponseHandler() {
					@Override
					public void onSuccess(org.json.JSONArray response) {
						parseJson(response, mImages);
					}

					@Override
					public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
						Util.log("Get images failed");
					}
				});
	}
	
	/**
	 * Parse a json array of url strings and add them to the arraylist
	 * @param json
	 * @param images
	 */
	private void parseJson(JSONArray json, ArrayList<String> images){
		int numImages = json.length();
		for(int i = 0; i < numImages; i++){
			try {
				images.add(json.getString(i));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
