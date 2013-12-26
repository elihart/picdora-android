package com.picdora;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Image;

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
		//Util.log("Delete : " + deleteDatabase("sprinkles.db"));
		Sprinkles sprinkles = Sprinkles.getInstance(getApplicationContext());

		// create models
		Migration addModelsMigration = new Migration();	
		addModelsMigration.createTable(Image.class);
		addModelsMigration.createTable(Category.class);
		addModelsMigration.createTable(Channel.class);
		sprinkles.addMigration(addModelsMigration);
		
//		Migration categoriesMigration = new Migration();		
//		categoriesMigration.createTable(Image.class);
//		sprinkles.addMigration(categoriesMigration);
//		
//		Migration channelsMigration = new Migration();		
//		channelsMigration.createTable(Image.class);
//		sprinkles.addMigration(channelsMigration);


		// get some images
		 ImageManager.getImagesFromServer(100);
		
		
		
		
	}

}
