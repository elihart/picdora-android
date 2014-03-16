package com.picdora;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.picdora.models.Image;

public abstract class ImageUtils {
	// the imgur api endpoint for getting images
	private static final String IMGUR_BASE_URL = "http://i.imgur.com/";
	// an extension must be included to get direct access to the image. It
	// doesn't seem to matter what the extension is; you can reach a gif with a
	// jpg extension
	private static final String IMGUR_BASE_EXTENSION = ".jpg";

	/**
	 * List of IMGUR api thumbnail sizes. The keys should be inserted after the
	 * image id and before the file extension
	 */
	public enum IMGUR_SIZE {
		/**
		 * 90X90 - Crops image
		 */
		SMALL_SQUARE("s"),
		/**
		 * 160X160 - Crops image
		 */
		BIG_SQUARE("b"),
		/**
		 * 160x160
		 */
		SMALL_THUMBNAIL("t"),
		/**
		 * 320X320
		 */
		MEDIUM_THUMBNAIL("m"),
		/**
		 * 640X640
		 */
		LARGE_THUMBNAIL("l"),
		/**
		 * 1024X1024
		 */
		HUGE_THUMBNAIL("h"),
		/**
		 * Original image size
		 */
		FULL("");

		private String key;

		private IMGUR_SIZE(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	}
	
	public static String getImgurLink(String imgurId, IMGUR_SIZE size) {
		return IMGUR_BASE_URL + imgurId + size.key + IMGUR_BASE_EXTENSION;
	}

	public static String getImgurLink(Image image, IMGUR_SIZE size) {
		return getImgurLink(image.getImgurId(), size);
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
