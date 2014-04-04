package com.picdora;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
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
	 * image id and before the file extension.
	 * <p>
	 * https://api.imgur.com/models/image
	 */
	public enum ImgurSize {
		/**
		 * 90X90 - Crops image
		 */
		SMALL_SQUARE("s", 90),
		/**
		 * 160X160 - Crops image
		 */
		BIG_SQUARE("b", 160),
		/**
		 * 160x160
		 */
		SMALL_THUMBNAIL("t", 160),
		/**
		 * 320X320
		 */
		MEDIUM_THUMBNAIL("m", 320),
		/**
		 * 640X640
		 */
		LARGE_THUMBNAIL("l", 640),
		/**
		 * 1024X1024
		 */
		HUGE_THUMBNAIL("h", 1024),
		/**
		 * Original image size. No key is used and size is unknown.
		 */
		FULL("", 1024);

		/**
		 * The letter that needs to be appended to an imgur id in a get request
		 * to receive the desired size
		 */
		private String key;
		private int size;

		private ImgurSize(String key, int size) {
			this.key = key;
			this.size = size;
		}

		/** Get the character suffix needed for this thumbnail size */
		public String getKey() {
			return key;
		}

		/** The edge length of the thumbnail square in pixels. */
		public int getSize() {
			return size;
		}
	}

	/**
	 * Get the url to the given imgur image
	 * 
	 * @param imgurId
	 *            The id of the imgur image
	 * @param size
	 *            The size of the image to get
	 * @return
	 */
	public static String getImgurLink(String imgurId, ImgurSize size) {
		return IMGUR_BASE_URL + imgurId + size.key + IMGUR_BASE_EXTENSION;
	}

	/**
	 * Get the url to the given imgur image
	 * 
	 * @param image
	 *            The image to get
	 * @param size
	 *            The size of the image to get
	 * @return
	 */
	public static String getImgurLink(Image image, ImgurSize size) {
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

	/**
	 * Download the full size version of the imgur image with the given id and
	 * save it to the user's public images
	 * 
	 * @param imgurId
	 * @param listener
	 *            Optional listener that will be called when the download
	 *            completes
	 */
	public static void saveImgurImage(final Context context, String imgurId,
			final OnDownloadCompleteListener listener) {
		// store the image in the public pictures directory
		File pictureDirectory = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

		File folder = new File(pictureDirectory, "Picdora/");
		// Make sure the Pictures directory exists.
		folder.mkdirs();

		File file = new File(folder, imgurId + ".jpg");

		String url = getImgurLink(imgurId, ImgurSize.FULL);

		Ion.with(context, url).write(file)
				.setCallback(new FutureCallback<File>() {
					@Override
					public void onCompleted(Exception e, File file) {
						if (e == null) {
							// alert media gallery to new file
							Uri uri = Uri.fromFile(file);
							Intent scanFileIntent = new Intent(
									Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
							context.sendBroadcast(scanFileIntent);
							// alert listener
							if (listener != null) {
								listener.onDownloadComplete(true);
							}
						} else {
							if (listener != null) {
								listener.onDownloadComplete(false);
							}
						}
					}
				});

	}

	public static void lookupImage(Activity context, String imgurId) {
		String query = "https://www.google.com/searchbyimage?&image_url="
				+ getImgurLink(imgurId, ImgurSize.FULL);

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(query));
		context.startActivity(i);
	}

	/**
	 * Callback for when an image download completes
	 */
	public interface OnDownloadCompleteListener {
		/**
		 * Called when the image downloads.
		 * 
		 * @param success
		 *            True if the download was downloaded successfully and false
		 *            otherwise
		 */
		public void onDownloadComplete(boolean success);
	}

	/**
	 * Mark the given image as reported and notify the server of the report.
	 * 
	 * @param currentImage
	 */
	public static void reportImage(ChannelImage currentImage) {
		// TODO Auto-generated method stub

	}

	/**
	 * Launch a chooser dialog to select an app to share the image with.
	 * 
	 * @param activity
	 * @param imgurId
	 */
	public static void shareImage(Activity activity, String imgurId) {
		List<String> toShare = new ArrayList<String>();
		toShare.add(imgurId);
		share(activity, toShare);
	}

	/**
	 * Launch a chooser dialog to select an app to share the images with.
	 * 
	 * @param activity
	 * @param selectedImages
	 */
	public static void shareImages(Activity activity, List<Image> selectedImages) {
		List<String> toShare = new ArrayList<String>();

		for (Image i : selectedImages) {
			toShare.add(i.getImgurId());
		}

		share(activity, toShare);
	}

	private static void share(Activity activity, List<String> imgurIds) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);

		/*
		 * Create a string of html links out of the imgur ids so that we can
		 * share them all as an extra
		 */
		StringBuilder extraBuilder = new StringBuilder();
		for (String id : imgurIds) {
			extraBuilder.append(getImgurLink(id, ImgurSize.FULL));
			extraBuilder.append(" ");
		}
		sendIntent.putExtra(Intent.EXTRA_TEXT, extraBuilder.toString());

		sendIntent.setType("text/plain");
		activity.startActivity(Intent.createChooser(sendIntent, "Share image"));
	}

	/**
	 * Create a parenthesized, comma separated list of the imgur ids of the
	 * given images for use in db queries.
	 * 
	 * @param images
	 * @return Id list - "("we1asd", "oij23j")"
	 */
	public static String getImgurIds(List<Image> images) {
		StringBuilder builder = new StringBuilder();
		builder.append("(");

		/* Add imgurid in quotes and end with a comma*/
		for (Image i : images) {
			builder.append("\"");
			builder.append(i.getImgurId());
			builder.append("\",");
		}
		
		/* Replace last comma with closing parenthesis */
		builder.deleteCharAt(builder.length() - 1);
		builder.append(")");
		String result = builder.toString();
		return result;
	}

	/**
	 * Create a parenthesized, comma separated list of the ids of the given
	 * channels for use in db queries.
	 * 
	 * @param channels
	 * @return Id list - "(1,2,3)"
	 */
	public static String getChannelIds(List<Channel> channels) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Channel c : channels) {
			ids.add((int) c.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}
}
