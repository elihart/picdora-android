package com.picdora.imageloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.params.ClientPNames;

import pl.droidsonroids.gif.GifDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import com.picdora.Util;
import com.picdora.loopj.AsyncHttpClient;
import com.picdora.loopj.BinaryHttpResponseHandler;
import com.picdora.loopj.RequestHandle;
import com.picdora.models.Image;

public class ImageLoader {
	// TODO: Optimize preloading and max connections based on internet speed and
	// speed that the user is going through the pictures

	// maximum images to download at once
	private static final int MAX_DOWNLOADS = 5;
	// the number of times to attempt a method that might fail due to an out of
	// memory error
	private static final int MAX_OOM_ATTEMPTS = 3;
	// the inititalized loader
	private static ImageLoader mLoader;

	private Map<String, Download> mDownloads;
	private AsyncHttpClient client;
	private Context mContext;
	// hold images that we had to kick out of the download list to make room for
	// more recent ones. If we have space we can come back to these
	private LinkedList<Image> mCanceledImages;
	// hold a queue of upcoming images that can be preloaded if there is space
	// in the download list
	private LinkedList<Image> mUpcomingImages;

	// keep track of the largest dimension that the image should fit
	private int mMaxDimension;
	// allow the image to be slightly bigger than the max size if the
	// alternative is to downsample a lot
	private static final float SIZE_ALLOWANCE = 1.1f;

	private PicdoraImageCache mCache;

	private ImageLoader(Context context) {
		mContext = context;

		// init cache
		mCache = new PicdoraImageCache(mContext);

		// save screen size
		DisplayMetrics displaymetrics = mContext.getResources()
				.getDisplayMetrics();
		int width = displaymetrics.widthPixels;
		int height = displaymetrics.heightPixels;

		// if the height or width don't come out right, use a fallback value
		mMaxDimension = (int) (Math.max(Math.max(width, height), 480) * SIZE_ALLOWANCE);

		// init downloads map
		mDownloads = new HashMap<String, Download>();

		// init queues
		mCanceledImages = new LinkedList<Image>();
		mUpcomingImages = new LinkedList<Image>();

		// setup async client
		client = new AsyncHttpClient();
		client.getHttpClient().getParams()
				.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		client.setMaxConnections(MAX_DOWNLOADS);
	}

	public static ImageLoader instance() {
		return mLoader;
	}

	public interface LoadCallbacks {
		public void onProgress(int percentComplete);

		public void onSuccess(Drawable drawable);

		public void onError(LoadError error);

	}

	public enum LoadError {
		UNKOWN, OUT_OF_MEMORY, DOWNLOAD_FAILURE, DOWNLOAD_CANCELED, FAILED_DECODE
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
	 * Cancel all downloads in the download list
	 */
	public void clearDownloads() {
		mUpcomingImages.clear();
		mCanceledImages.clear();

		// need to make a copy of the collection because the cancel method
		// removes them from the list
		List<Download> downloads = new ArrayList<Download>(mDownloads.values());

		for (Download d : downloads) {
			cancelDownload(d);
		}

		System.gc();
	}

	/**
	 * Load the given image. Will attempt to load from cache if available,
	 * otherwise it will download it. If a current download of the same image is
	 * in progress then the callbacks are added to that download.
	 * <p>
	 * If callbacks is null then this method checks if the image is already
	 * cached, and downloads it if it isn't, but no callback is made on
	 * completion; this essentially makes the method act as a preloader.
	 * 
	 * @param image
	 * @param callbacks
	 */
	public synchronized void loadImage(final Image image,
			final LoadCallbacks callbacks) {
		// if the image has already been downloaded and cached, and there is no
		// callback, then we are done
		if (mCache.contains(image) && callbacks == null) {
			return;
		}

		// query the cache in the background. On hit return the image, on miss
		// download it
		new AsyncTask<Void, Void, Drawable>() {

			@Override
			protected Drawable doInBackground(Void... params) {
				// try to get the image from cache and create a drawable out of
				// it. Both the cache access and the drawable creation can cause
				// out of memory errors so we need to catch those and possibly
				// retry

				byte[] data = null;

				int attempts = 0;

				while (attempts < MAX_OOM_ATTEMPTS) {
					try {
						// if we are retrying due to OOM the data may already be
						// loaded. In that case we don't need to get it again
						if (data == null) {
							data = mCache.get(image);
						}

						// if there was a cache hit then attempt a drawable
						if (data != null) {
							return createDrawable(data);
						} else {
							return null;
						}
					} catch (OutOfMemoryError e) {
						// attempt a garbage collection to clean up memory and
						// then wait briefly before trying again
						// TODO: app wide memory management system
						attempts++;
						System.gc();
						SystemClock.sleep(100);
					}
				}

				// maxed out OOM attempts so give up
				return null;
			}

			protected void onPostExecute(Drawable d) {
				// on cache hit return the result
				if (d != null) {
					if (callbacks != null) {
						callbacks.onSuccess(d);
					}
					return;
				}

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
		}.execute();
	}

	/**
	 * Start downloading and caching the given image in anticipation of needing
	 * it soon
	 * 
	 * @param image
	 */
	public void preloadImage(Image image) {
		// if it's in the cache we're already done
		if (mCache.contains(image)) {
			return;
		}

		// check if it's already downloading
		if (mDownloads.containsKey(image.getImgurId())) {
			return;
		}

		// otherwise remove it from any queue it's in now and start it if room
		// or add it to the queue
		clearFromQueue(image);

		if (isDownloadsFull()) {
			mUpcomingImages.addFirst(image);
		} else {
			loadImage(image, null);
		}
	}

	/**
	 * Whether or not we are maxed out on our number of concurrent downloads
	 * 
	 * @return
	 */
	private boolean isDownloadsFull() {
		return mDownloads.size() >= MAX_DOWNLOADS;
	}

	// tell the binary response handler to allow all content
	private static final String[] ALLOWED_CONTENT_TYPES = new String[] { ".*" };

	/**
	 * Start an image downloading and add it to the download list
	 * 
	 * @param image
	 * @param callbacks
	 */
	private void startDownload(Image image, LoadCallbacks callbacks) {
		// remove it from the queue if it's there
		clearFromQueue(image);

		final Download download = new Download(new Date().getTime(), callbacks,
				null, image);

		RequestHandle handle = client.get(image.getUrl(),
				new BinaryHttpResponseHandler(ALLOWED_CONTENT_TYPES) {
					@Override
					public void onProgress(int progress, int size) {
						handleProgress(download, progress, size);
					}

					@Override
					public void onFailure(int statusCode,
							org.apache.http.Header[] headers,
							byte[] binaryData, java.lang.Throwable error) {
						handleFailure(download,
								reasonForDownloadFailure(statusCode, error));
						downloadFinished(download);

					}

					@Override
					public void onSuccess(int statusCode, Header[] headers,
							byte[] binaryData) {
						handleSuccess(download, binaryData);
						downloadFinished(download);

					}

					@Override
					public void onCancel() {
						downloadFinished(download);
					}
				});

		download.handle = handle;

		addDownload(download);

	}

	protected void downloadFinished(Download download) {
		removeDownload(download);

		// preload an upcoming image, or restart a canceled image if there is room
		if (isDownloadsFull()) {
			return;
		} else if (!mUpcomingImages.isEmpty()) {
			loadImage(mUpcomingImages.removeFirst(), null);
		} else if (!mCanceledImages.isEmpty()) {
			loadImage(mCanceledImages.removeFirst(), null);
		}
	}

	// remove the given image from all queues
	private void clearFromQueue(Image image) {
		mUpcomingImages.remove(image);
		mCanceledImages.remove(image);
	}

	/**
	 * Return an ImageLoad error for a failed download based on the given
	 * onFailure data
	 * 
	 * @param statusCode
	 * @param error
	 * @return
	 */
	protected LoadError reasonForDownloadFailure(int statusCode, Throwable error) {
		// TODO: Specific reasons for download failure
		return LoadError.DOWNLOAD_FAILURE;
	}

	/**
	 * Handle a download that failed
	 * 
	 * @param imageId
	 * @param statusCode
	 * @param error
	 */
	protected void handleFailure(Download download, LoadError error) {

		if (download.listeners == null) {
			return;
		}

		// alert listeners to error

		for (LoadCallbacks listener : download.listeners) {
			if (listener != null) {
				listener.onError(error);
			}
		}
	}

	/**
	 * Handle a download that succeeded
	 * 
	 * @param imageId
	 * @param binaryData
	 */
	protected void handleSuccess(Download download, byte[] binaryData) {
		download.data = binaryData;

		// cache data
		new CacheDownloadsAsync().execute(download);

		// if there are no listeners waiting for the result then there is no
		// point in decoding the image
		if (download.listeners == null || download.listeners.isEmpty()) {
			return;
		}

		// decode data and pass it to listeners.
		new CreateDrawableFromDownloadAsync().execute(download);
	}

	/**
	 * Create a drawable out of a byte array. Will be either an animated
	 * drawable if the data is a gif, or a bitmapdrawable for an image. On error
	 * null will be returned
	 * 
	 * @param binaryData
	 * @return
	 * @throws OutOfMemoryError
	 */
	private Drawable createDrawable(byte[] binaryData) throws OutOfMemoryError {
		// try to decode it as a gif
		// TODO: Update the Image to have the correct gif status in database
		try {
			return new GifDrawable(binaryData);
		} catch (IOException e) {
			// not a bitmap
		}

		// decode as a bitmap if the gif decode fails

		// get image dimensions
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory
				.decodeByteArray(binaryData, 0, binaryData.length, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		Bitmap bm = BitmapFactory.decodeByteArray(binaryData, 0,
				binaryData.length, options);

		if (bm == null) {
			// error decoding data
			return null;
		} else {
			return new BitmapDrawable(mContext.getResources(), bm);
		}
	}

	private int calculateInSampleSize(BitmapFactory.Options options) {

		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > mMaxDimension || width > mMaxDimension) {

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((height / inSampleSize) > mMaxDimension
					|| (width / inSampleSize) > mMaxDimension) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * Handle a download's progress
	 * 
	 * @param imageId
	 * @param progress
	 * @param size
	 */
	protected void handleProgress(Download download, int progress, int size) {

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
		if (isDownloadsFull()) {
			removeOldestDownload();
		}

		mDownloads.put(download.image.getImgurId(), download);
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

		mCanceledImages.addFirst(oldest.image);
	}

	/**
	 * Interrupt the download and cancel it. Alert any listeners that it's been
	 * canceled, then remove it from the list
	 * 
	 * @param oldest
	 */
	private void cancelDownload(Download download) {
		removeDownload(download);

		download.handle.cancel(true);

		for (LoadCallbacks listener : download.listeners) {
			if (listener != null) {
				listener.onError(LoadError.DOWNLOAD_CANCELED);
			}
		}
	}

	/**
	 * Remove a download from the list, clear its listeners, and clear its
	 * handle. The download is not stopped, so it should already be finished or
	 * canceled before this is called.
	 * 
	 * @param download
	 */
	private void removeDownload(Download download) {
		mDownloads.remove(download.image.getImgurId());
	}

	class Download {
		public long startTime;
		public List<LoadCallbacks> listeners;
		public RequestHandle handle;
		public Image image;

		// the resulting data and decoded drawable of the download. These can be
		// set manually once they are available
		public Drawable drawable;
		public byte[] data;
		// An error that caused this download to fail
		public LoadError error;

		public Download(long startTime, LoadCallbacks listener,
				RequestHandle handle, Image image) {
			this.startTime = startTime;
			this.handle = handle;
			this.image = image;

			listeners = new ArrayList<LoadCallbacks>();
			if (listener != null) {
				listeners.add(listener);
			}
		}

	}

	private class CacheDownloadsAsync extends AsyncTask<Download, Void, Void> {
		protected Void doInBackground(Download... downloads) {
			int count = downloads.length;
			for (int i = 0; i < count; i++) {
				Download d = downloads[i];
				mCache.put(d.image, d.data);
			}
			return null;
		}
	}

	private class CreateDrawableFromDownloadAsync extends
			AsyncTask<Download, Void, Download> {

		protected Download doInBackground(Download... downloads) {
			Download download = downloads[0];

			int attempts = 0;

			while (attempts < MAX_OOM_ATTEMPTS) {
				// reset the error message and try to decode the download
				download.error = null;

				try {
					download.drawable = createDrawable(download.data);
					if (download.drawable == null) {
						download.error = LoadError.FAILED_DECODE;
					}
					break;
				} catch (OutOfMemoryError e) {
					// attempt a garbage collection to clean up memory and
					// then wait briefly before trying again
					// TODO: app wide memory management system
					download.error = LoadError.OUT_OF_MEMORY;
					attempts++;
					System.gc();
					SystemClock.sleep(100);
				}
			}

			return download;
		}

		protected void onPostExecute(Download result) {
			if (result.error != null) {
				handleFailure(result, result.error);
			} else {
				// pass drawable to listeners
				if (result.listeners != null) {
					for (LoadCallbacks listener : result.listeners) {
						if (listener != null) {
							listener.onSuccess(result.drawable);
						}
					}
				}
			}

			// passed the drawable on, don't need it anymore
			result.drawable = null;
		}
	}
}
