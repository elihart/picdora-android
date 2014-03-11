package com.picdora.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.json.JSONArray;
import org.json.JSONException;

import retrofit.client.Response;
import se.emilsjolander.sprinkles.Transaction;

import com.picdora.Util;
import com.picdora.models.Category;

@EBean
public class CategorySyncer extends Syncer {
	// TODO: Keep track of last sync time and only do an update when something
	// changes

	public void sync() {
		Util.log("Category sync");
		getCategories();
	}

	private void getCategories() {
		Response response = mApiService.categories();
		if(response == null || response.getBody() == null){
			onFailure();
			return;
		}
		
		try {
			String json = responseToString(response);
			JSONArray arr = new JSONArray(json);
			List<Category> categories = getCategoriesFromJson(arr);
			if (saveCategoriesToDb(categories)) {
				onSuccess();
				
			} else {
				onFailure();
			}
		} catch (IOException e) {
			onFailure();
		} catch (JSONException e) {
			onFailure();
		}
	}
	
	private void onFailure(){
		Util.log("Category Sync failure");
	}
	
	private void onSuccess(){
		Util.log("Category Sync success");
	}

	/**
	 * Parse a json array into a list of categories
	 * 
	 * @param arr
	 * @return
	 * @throws JSONException
	 */
	private List<Category> getCategoriesFromJson(JSONArray arr)
			throws JSONException {
		List<Category> categories = new ArrayList<Category>();

		for (int i = arr.length() - 1; i >= 0; i--) {
			// Use the Category JSONObject constructor to do the parsing
			Category cat = new Category(arr.getJSONObject((i)));
			categories.add(cat);
		}

		return categories;
	}

	private boolean saveCategoriesToDb(List<Category> categories) {
		Transaction t = new Transaction();
		try {
			for (Category c : categories) {
				c.save(t);
			}
			t.setSuccessful(true);
		} catch (Exception e) {
			Util.log("Exception thrown while saving categories");
			e.printStackTrace();
			t.setSuccessful(false);
		} finally {
			t.finish();
		}

		return t.isSuccessful();
	}
}
