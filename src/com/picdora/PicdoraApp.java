package com.picdora;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;
import android.graphics.Bitmap;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.picdora.ImageManager.OnResultListener;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.FontHelper;

@EApplication
public class PicdoraApp extends Application {
	@Bean
	protected ImageUpdater mImageUpdater;

	@Override
	public void onCreate() {
		super.onCreate();

		FontHelper.init(getApplicationContext());

		PicdoraImageLoader.init(this);

		initUniversalImageLoader();

		runMigrations();

		//syncDb();
		// TODO: Sync uses lots of memory and can interfere with image loading on small heaps
	}

	private void initUniversalImageLoader() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
        .cacheInMemory(true) 
        .cacheOnDisc(true)
        .bitmapConfig(Bitmap.Config.RGB_565) 
        .build();
		
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
        .defaultDisplayImageOptions(options) 
        .build();
		
    ImageLoader.getInstance().init(config);
		
	}

	private void syncDb() {
		CategoryHelper.syncCategoriesWithServer(new OnResultListener() {

			@Override
			public void onSuccess() {
				Util.log("Category sync success");
				mImageUpdater.getNewImages();
			}

			@Override
			public void onFailure() {
				Util.log("Category sync failure");
				mImageUpdater.getNewImages();
			}
		});

	}

	// Run db migrations with sprinkles
	private void runMigrations() {
		// Util.log("Delete : " + deleteDatabase("sprinkles.db"));
		Sprinkles sprinkles = Sprinkles.getInstance(getApplicationContext());

		// create models
		Migration addModelsMigration = new Migration();
		addModelsMigration.createTable(Image.class);
		addModelsMigration.createTable(Category.class);
		addModelsMigration.createTable(Channel.class);
		sprinkles.addMigration(addModelsMigration);

	}

	@Override
	public void onLowMemory() {
		// TODO: Contact system activities and tell them to cut back memory
	}

}
