package com.picdora.imageloader;

import java.io.ByteArrayInputStream;
import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.picdora.Util;
import com.picdora.models.Image;

public class PicdoraImageCache {
	private static final String DISK_CACHE_PATH = "/Picdora-BitmapCache";
	private static final long DISK_CACHE_SIZE = 1024 * 1024 * 100;
	private static BitmapLruCache mBitmapCache;
	private Context mContext;

	public PicdoraImageCache(Context context) {
		mContext = context;

		if (mBitmapCache == null) {
			initBitmapCache();
		}
	}

	private void initBitmapCache() {
		File cacheLocation;

		// If we have external storage use it for the disk cache. Otherwise we
		// use
		// the cache dir
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			cacheLocation = new File(Environment.getExternalStorageDirectory()
					+ DISK_CACHE_PATH);
		} else {
			cacheLocation = new File(mContext.getFilesDir() + DISK_CACHE_PATH);
		}

		cacheLocation.mkdirs();

		boolean diskCacheExists = cacheLocation.exists();
		Util.log("disk cache exists : " + diskCacheExists);

		BitmapLruCache.Builder builder = new BitmapLruCache.Builder(mContext);
		builder.setMemoryCacheEnabled(false);
		builder.setRecyclePolicy(BitmapLruCache.RecyclePolicy.ALWAYS);

		builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheLocation)
				.setDiskCacheMaxSize(DISK_CACHE_SIZE);

		mBitmapCache = builder.build();
	}

	public Drawable get(Image image) {
		String key = image.getImgurId();
		if (mBitmapCache.contains(key)) {
			Util.log("cache hit!");
			return mBitmapCache.get(key);
		} else {
			Util.log("cache miss");
			return null;
		}

	}

	public void put(final String imageId, final Bitmap bm) {
		mBitmapCache.put(imageId, bm);
		
	}

	public Drawable put(final String imageId, ByteArrayInputStream byteArrayInputStream, BitmapFactory.Options options) {
		return mBitmapCache.put(imageId, byteArrayInputStream, options);
		
	}

}
