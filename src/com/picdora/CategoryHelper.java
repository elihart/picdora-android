package com.picdora;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Transaction;

import com.picdora.ImageManager.OnResultListener;
import com.picdora.loopj.JsonHttpResponseHandler;
import com.picdora.models.Category;

public abstract class CategoryHelper {

	public static Category getCategoryById(int categoryId) {
		return Query.one(Category.class, "SELECT * FROM Categories WHERE id=?",
				categoryId).get();
	}

	public static void sortByName(List<Category> categories) {
		Collections.sort(categories, new Comparator<Category>() {

			@Override
			public int compare(Category lhs, Category rhs) {
				return lhs.getName().compareToIgnoreCase(rhs.getName());
			}
		});
	}

	public static void syncCategoriesWithServer(final OnResultListener listener) {
		PicdoraApiClient.get("categories", new JsonHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					org.json.JSONArray response) {
				saveCategoriesToDb(response);
				listener.onSuccess();
			}

			@Override
			public void onFailure(int statusCode,
					org.apache.http.Header[] headers,
					java.lang.String responseBody, java.lang.Throwable e) {
				listener.onFailure();
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, JSONObject errorResponse) {
				listener.onFailure();
			}
		});
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
	
	public static void sortCategoryListAlphabetically(List<Category> categories){
		Collections.sort(categories, new CategoryAlphabeticalComparator());
	}
	
    /**
	 * Basic comparator to sort categories alphabetically by name
	 * 
	 */
	private static class CategoryAlphabeticalComparator implements Comparator<Category> {
		public int compare(Category left, Category right) {
			return left.getName().toLowerCase()
					.compareTo(right.getName().toLowerCase());
		}
	}

}
