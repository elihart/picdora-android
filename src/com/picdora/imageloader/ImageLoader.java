package com.picdora.imageloader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.params.ClientPNames;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.picdora.Util;
import com.picdora.models.Image;

public class ImageLoader {
	// maximum images to download at once
	private static final int MAX_DOWNLOADS = 5;
	// the inititalized loader
	private static ImageLoader mLoader;

	private Map<String, Download> mDownloads;
	private AsyncHttpClient client;
	private Context mContext;
	
	// keep track of the largest dimension that the image should fit
	private int mMaxDimension;

	private ImageLoader(Context context) {
		mContext = context;

		// init caches

		// save screen size
		DisplayMetrics displaymetrics = mContext.getResources().getDisplayMetrics();
		int width = displaymetrics.widthPixels;
		int height = displaymetrics.heightPixels;
		mMaxDimension =  Math.max(width, height); 

		// init downloads map
		mDownloads = new HashMap<String, Download>();

		// setup async client
		client = new AsyncHttpClient();
		client.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
	}

	public static ImageLoader instance() {
		return mLoader;
	}

	public interface LoadCallbacks {
		public void onProgress(int percentComplete);

		public void onSuccess(Bitmap bm);

		public void onError(LoadError error);

	}

	public enum LoadError {
		UNKOWN, OUT_OF_MEMORY, DOWNLOAD_FAILURE, DOWNLOAD_CANCELED
	}

	/**
	 * Initialize the loader. Must be called before loading any images, and only
	 * needs to be called once.
	 * 
	 * @param context
	 */
	public static void init(Context context) {
		// only init once
		if (mLoader == null) {
			mLoader = new ImageLoader(context);
		}
	}

	/**
	 * Load the given image. Will attempt to load from cache if available,
	 * otherwise it will download it. If a current download of the same image is
	 * in progress then the callbacks are added to that download
	 * 
	 * @param image
	 * @param callbacks
	 */
	public synchronized void loadImage(Image image, LoadCallbacks callbacks) {
		if (image == null) {
			return;
		}

		// check cache first

		// on cache miss check if there is an existing download
		Download download = mDownloads.get(image.getImgurId());
		if (download != null) {
			// update the start time
			download.startTime = new Date().getTime();

			// add the callback
			download.listeners.add(callbacks);
		}

		// otherwise start a new download
		else {
			startDownload(image, callbacks);
		}
	}

	/**
	 * Start an image downloading and add it to the download list
	 * 
	 * @param image
	 * @param callbacks
	 */
	private void startDownload(Image image, LoadCallbacks callbacks) {
		final String imageId = new String(image.getImgurId());

		RequestHandle handle = client.get(image.getUrl(),
				new BinaryHttpResponseHandler() {
					@Override
					public void onProgress(int progress, int size) {
						handleProgress(imageId, progress, size);
					}

					@Override
					public void onSuccess(byte[] binaryData) {
						handleSuccess(imageId, binaryData);
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							byte[] binaryData, java.lang.Throwable error) {
						handleFailure(imageId, statusCode, error);
					}

				});

		addDownload(new Download(new Date().getTime(), callbacks, handle,
				new String(image.getImgurId())));

	}

	/**
	 * Handle a download that failed
	 * 
	 * @param imageId
	 * @param statusCode
	 * @param error
	 */
	protected void handleFailure(String imageId, int statusCode, Throwable error) {
		Download download = mDownloads.get(imageId);
		if (download == null) {
			Util.log("Failure for download that doesn't exist");
			return;
		}

		if (download.listeners == null) {
			return;
		}

		for (LoadCallbacks listener : download.listeners) {
			// TODO: Customize error
			if (listener != null) {
				listener.onError(LoadError.UNKOWN);
			}
		}

	}

	/**
	 * Handle a download that succeeded
	 * 
	 * @param imageId
	 * @param binaryData
	 */
	protected void handleSuccess(String imageId, byte[] binaryData) {
		Download download = mDownloads.get(imageId);
		if (download == null) {
			Util.log("Success for download that doesn't exist");
			return;
		}

		if (download.listeners == null) {
			return;
		}

		// process the image
		Bitmap bm = BitmapFactory.decodeByteArray(binaryData, 0,binaryData.length);

		// cache the image

		for (LoadCallbacks listener : download.listeners) {
			if (listener != null) {
				// pass the image back to listeners
				listener.onSuccess(bm);
			}
		}

		// remove download from list
		removeDownload(download);

	}

	/**
	 * Handle a download's progress
	 * 
	 * @param imageId
	 * @param progress
	 * @param size
	 */
	protected void handleProgress(String imageId, int progress, int size) {
		Download download = mDownloads.get(imageId);
		if (download == null) {
			Util.log("Progress for download that doesn't exist");
			return;
		}

		if (download.listeners == null) {
			return;
		}

		int percent = (int) (progress * 100f / size);

		for (LoadCallbacks listener : download.listeners) {
			if (listener != null) {
				listener.onProgress(percent);
			}
		}
	}

	public void unregisterCallbacks(String imageId, LoadCallbacks callbacks) {
		Download download = mDownloads.get(imageId);
		if (download == null || download.listeners == null) {
			return;
		}

		download.listeners.remove(callbacks);
	}

	/**
	 * Add the download to the download list. If the list is full, then cancel
	 * and remove the oldest download
	 * 
	 * @param download
	 */
	private void addDownload(Download download) {
		// check if download list is full
		if (mDownloads.size() > MAX_DOWNLOADS) {
			removeOldestDownload();
		}

		mDownloads.put(download.imageId, download);
	}

	/**
	 * Find the oldest download in the list and cancel and remove it.
	 */
	private void removeOldestDownload() {
		// find the oldest download
		Download oldest = null;
		for (Download download : mDownloads.values()) {
			if (oldest == null) {
				oldest = download;
			} else if (download.startTime < oldest.startTime) {
				oldest = download;
			}
		}

		if (oldest == null) {
			return;
		}

		cancelDownload(oldest);

	}

	/**
	 * Interrupt the download and cancel it. Alert any listeners that it's been
	 * canceled, then remove it from the list
	 * 
	 * @param oldest
	 */
	private void cancelDownload(Download download) {
		download.handle.cancel(true);

		if (download.listeners == null) {
			return;
		}

		for (LoadCallbacks listener : download.listeners) {
			if (listener != null) {
				listener.onError(LoadError.DOWNLOAD_CANCELED);
			}
		}

		removeDownload(download);
	}

	/**
	 * Remove a download from the list, clear its listeners, and clear its
	 * handle. The download is not stopped, so it should already be finished or
	 * canceled before this is called.
	 * 
	 * @param download
	 */
	private void removeDownload(Download download) {
		mDownloads.remove(download.imageId);

		if (download.listeners != null) {
			download.listeners.clear();
			download.listeners = null;
		}

		download.handle = null;
	}

	class Download {
		public long startTime;
		public List<LoadCallbacks> listeners;
		public RequestHandle handle;
		public String imageId;

		public Download(long startTime, LoadCallbacks listener,
				RequestHandle handle, String imageId) {
			this.startTime = startTime;
			this.handle = handle;
			this.imageId = imageId;

			listeners = new ArrayList<LoadCallbacks>();
			if (listener != null) {
				listeners.add(listener);
			}
		}

	}
}
