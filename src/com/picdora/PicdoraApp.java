package com.picdora;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import android.graphics.Bitmap;

public class PicdoraApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Util.log("Creating app at " + new Date());

		runMigrations();

		initImageLoader();

		initGifLoader();
	}

	private void initGifLoader() {

	}

	private void initImageLoader() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
				.build();

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
		// ImageManager.getImagesFromServer(100);
	}

}
