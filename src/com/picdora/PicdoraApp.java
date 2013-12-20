package com.picdora;

import java.io.IOException;
import java.io.InputStream;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.decode.ImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecodingInfo;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;

import net.frakbot.imageviewex.ImageViewNext;

public class PicdoraApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		runMigrations();

		initImageLoader();

		initGifLoader();
	}

	private void initGifLoader() {

	}

	private void initImageLoader() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheInMemory(true).cacheOnDisc(true).build();

		// Create global configuration and initialize ImageLoader with this
		// configuration
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(options)
				.build();
		
		ImageLoader.getInstance().init(config);
	}

	// Run db migrations with sprinkles
	private void runMigrations() {
		Sprinkles sprinkles = Sprinkles.getInstance(getApplicationContext());

		// create Images
		Migration initialMigration = new Migration();
		initialMigration.createTable(Image.class);
		sprinkles.addMigration(initialMigration);

		// get some images
		ImageManager.getImagesFromServer(100);
	}

}
