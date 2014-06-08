package com.picdora;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.Query;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.picdora.api.PicdoraApiService;
import com.picdora.models.Channel;
import com.picdora.models.Image;

public abstract class ImageUtil {
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
	public static void reportImage(Image image) {
		/* Only report if not already reported. */
		if(!image.isReported()){
			image.setReported(true);
			image.saveAsync();
			updateImageOnServer(image, false);
		}
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

	/**
	 * Set the gif status of the given image. This method will only change the
	 * gif value for images stored in the database. Any changes will be reported
	 * to the server. This method should always be used instead of calling
	 * setGif() directly on the image to ensure the server is updated as well.
	 * 
	 * @param image
	 * @param gif
	 *            True if the image is a gif, false otherwise.
	 */
	public static void setGifStatus(Image image, boolean gif) {
		/*
		 * If the new status isn't different from the old one then don't bother continuing.
		 */
		if (gif == image.isGif()) {
			return;
		}

		image.setGif(gif);
		image.saveAsync();

		updateImageOnServer(image, true);

	}

	/**
	 * Mark the selected image as deleted in the local database and queue the
	 * deletion to be reported to the main server asynchronously.
	 * 
	 * @param image
	 */
	public static void markImageDeleted(Image image) {
		/*
		 * If the image is already marked as deleted then don't do it again.
		 */
		if (!image.isDeleted()) {
			image.setDeleted(true);
			image.saveAsync();
			updateImageOnServer(image, false);
		}
	}

	/**
	 * Send a PUT request to update the information of a certain image on the
	 * server.
	 * 
	 * @param image The image to update
	 * @param updateGif Whether the gif status should be reviewed.
	 */
	private static void updateImageOnServer(Image image, boolean updateGif) {
		PicdoraApiService.getClient().updateImage(
				DeviceKeyUtils.getDeviceKey(PicdoraApp.getAppContext()),
				image.getId(), image.isReported(), image.isDeleted(),
				updateGif, null);
	}

	/**
	 * Get a comma separated list of Image ids for use in db queries.
	 * 
	 * @param images
	 * @return
	 */
	public static String getImageIds(List<Image> images) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Image i : images) {
			ids.add((int) i.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}

	/**
	 * Get the date of the most recent update in all the images, in unix time.
	 * Returns 0 if there are no images.
	 * 
	 * @return
	 */
	public static long getLastUpdated() {
		final String query = "SELECT MAX(lastUpdated) FROM Images";

		return DbUtils.simpleQueryForLong(query, 0);
	}

	/**
	 * Get the date in unix time of the creation of our newest image. Returns 0
	 * if there are no images.
	 * 
	 * @return
	 */
	public static long getNewestImageDate() {
		final String query = "SELECT MAX(createdAt) FROM Images";

		return DbUtils.simpleQueryForLong(query, 0);
	}

	/**
	 * Get the Image with the given id, or null if it doesn't exist.
	 * 
	 * @param imageId
	 *            The id of the image to get.
	 * @return The matching image, or null on no match.
	 */
	public static Image getImageById(long imageId) {
		return Query.one(Image.class, "SELECT * FROM Images WHERE id=?",
				imageId).get();
	}
}
