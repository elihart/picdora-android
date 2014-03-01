package com.picdora.imageloader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;

import com.jakewharton.disklrucache.DiskLruCache;
import com.picdora.Util;
import com.picdora.models.Image;

public class PicdoraImageCache {
	private Context mContext;

	//private static BitmapLruCache mBitmapCache;

	// Disk cache stuff
	private static final String DISK_CACHE_NAME = "PicdoraDiskCache";
	private static final long DISK_CACHE_SIZE = 1024 * 1024 * 30;
	private static final int DISK_CACHE_FLUSH_DELAY_SECS = 5;
	private static DiskLruCache mDiskCache;
	private HashMap<String, ReentrantLock> mDiskCacheEditLocks;
	private ScheduledThreadPoolExecutor mDiskCacheFlusherExecutor;
	private DiskCacheFlushRunnable mDiskCacheFlusherRunnable;
	private ScheduledFuture<?> mDiskCacheFuture;

	public PicdoraImageCache(Context context) {
		mContext = context;

		// init caches in background
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
//				if (mBitmapCache == null) {
//					initBitmapCache();
//				}

				try {
					if (mDiskCache == null) {
						initDiskCache();
					}
				} catch (IOException e) {
					// failed to open disk cache
					mDiskCache = null;
				}

				return null;
			}

		}.execute();

	}

	private void initDiskCache() throws IOException {
		File cacheDir = getDiskCacheDir(mContext, DISK_CACHE_NAME);

		int appVersion = 0;
		int valueCount = 1;
		mDiskCache = DiskLruCache.open(cacheDir, appVersion, valueCount,
				DISK_CACHE_SIZE);

		mDiskCacheEditLocks = new HashMap<String, ReentrantLock>();
		mDiskCacheFlusherExecutor = new ScheduledThreadPoolExecutor(1);
		mDiskCacheFlusherRunnable = new DiskCacheFlushRunnable(mDiskCache);
	}

	// Creates a unique subdirectory of the designated app cache directory.
	// Tries to use external
	// but if not mounted, falls back on internal storage.
	private static File getDiskCacheDir(Context context, String uniqueName) {
		// Check if media is mounted or storage is built-in, if so, try and use
		// external cache dir
		// otherwise use internal cache dir
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) ? context.getExternalCacheDir()
				.getPath() : context.getCacheDir().getPath();

		return new File(cachePath + File.separator + uniqueName);
	}

	private void initBitmapCache() {
		// TODO: Create memcache for bitmaps
	}

	public byte[] get(Image image) {
		byte[] result = null;
		String key = getKeyForImage(image);
		// First try Memory Cache
		// result = getFromMemoryCache(url);

		if (result == null) {
			// Memory Cache failed, so try Disk Cache
			result = getFromDiskCache(key);
		}

		return result;

	}

	private String getKeyForImage(Image image) {
		return image.getImgurId().toLowerCase(Locale.US);
	}

	private byte[] getFromDiskCache(String key) {
		if (mDiskCache == null) {
			return null;
		}

		checkNotOnMainThread();

		try {
			DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
			if (snapshot != null) {
				// TODO: put into memory cache
				// if (null != result) {
				// if (null != mMemoryCache) {
				// mMemoryCache.put(result);
				// }

				return IOUtils.toByteArray(snapshot.getInputStream(0));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void put(Image image, final byte[] data) {
		String key = getKeyForImage(image);

		// if (null != mMemoryCache) {
		// mMemoryCache.put(d);
		// }

		if (null != mDiskCache) {
			checkNotOnMainThread();

			final ReentrantLock lock = getLockForDiskCacheEdit(key);
			lock.lock();

			OutputStream os = null;

			try {
				DiskLruCache.Editor editor = mDiskCache.edit(key);
				os = editor.newOutputStream(0);
				os.write(data);
				os.flush();
				editor.commit();
				os.close();
			} catch (IOException e) {
				Util.log("Error while writing to disk cache");
			} finally {
				lock.unlock();
				scheduleDiskCacheFlush();
			}
		}

	}

	static final class DiskCacheFlushRunnable implements Runnable {

		private final DiskLruCache mDiskCache;

		public DiskCacheFlushRunnable(DiskLruCache cache) {
			mDiskCache = cache;
		}

		public void run() {
			// Make sure we're running with a background priority
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			try {
				mDiskCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ReentrantLock getLockForDiskCacheEdit(String url) {
		synchronized (mDiskCacheEditLocks) {
			ReentrantLock lock = mDiskCacheEditLocks.get(url);
			if (null == lock) {
				lock = new ReentrantLock();
				mDiskCacheEditLocks.put(url, lock);
			}
			return lock;
		}
	}

	private void scheduleDiskCacheFlush() {
		// If we already have a flush scheduled, cancel it
		if (null != mDiskCacheFuture) {
			mDiskCacheFuture.cancel(false);
		}

		// Schedule a flush
		mDiskCacheFuture = mDiskCacheFlusherExecutor.schedule(
				mDiskCacheFlusherRunnable, DISK_CACHE_FLUSH_DELAY_SECS,
				TimeUnit.SECONDS);
	}

	/**
	 * @throws IllegalStateException
	 *             if the calling thread is the main/UI thread.
	 */
	private static void checkNotOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			throw new IllegalStateException(
					"This method should not be called from the main/UI thread.");
		}
	}

}
