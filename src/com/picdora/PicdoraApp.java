package com.picdora;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.sync.PicdoraSyncManager;
import com.picdora.ui.FontHelper;

@EApplication
public class PicdoraApp extends Application {
	@Bean
	protected PicdoraSyncManager mSyncManager;
	@Pref
	protected PicdoraPreferences_ mPrefs;

	@Override
	public void onCreate() {
		super.onCreate();

		// resetApp();

		FontHelper.init(getApplicationContext());

		PicdoraImageLoader.init(this);

		initUniversalImageLoader();

		runMigrations();

		clearCache();

		mSyncManager.sync();
	}

	private void clearCache() {
		ImageLoader.getInstance().clearDiscCache();
		ImageLoader.getInstance().clearMemoryCache();

	}

	private void resetApp() {
		deleteDatabase("sprinkles.db");
		mPrefs.clear();
	}

	private void initUniversalImageLoader() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheInMemory(true).cacheOnDisc(true).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(options)
				.build();

		ImageLoader.getInstance().init(config);

	}

	// Run db migrations with sprinkles
	private void runMigrations() {
		Sprinkles sprinkles = Sprinkles.getInstance(getApplicationContext());

		// create models
		Migration addModelsMigration = new Migration();
		addModelsMigration.createTable(Image.class);
		addModelsMigration.createTable(Category.class);
		addModelsMigration.createTable(Channel.class);
		sprinkles.addMigration(addModelsMigration);

		// add category icons
		Migration addCatIcons = new Migration();
		addCatIcons.dropTable(Category.class);
		addCatIcons.createTable(Category.class);
		sprinkles.addMigration(addCatIcons);

		// add channel icons
		Migration addChannelIcons = new Migration();
		addChannelIcons.dropTable(Channel.class);
		addChannelIcons.createTable(Channel.class);
		sprinkles.addMigration(addChannelIcons);
	}

	@Override
	public void onLowMemory() {
		trimMemory();
	}

	private void trimMemory() {
		ImageLoader.getInstance().clearMemoryCache();
		System.gc();
	}

}
