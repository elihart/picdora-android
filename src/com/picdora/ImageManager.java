package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
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

import com.picdora.api.PicdoraApiService;
import com.picdora.loopj.JsonHttpResponseHandler;
import com.picdora.loopj.RequestParams;
import com.picdora.models.Category;
import com.picdora.models.Image;

public abstract class ImageManager {





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
