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
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;
import com.picdora.sync.PicdoraSyncManager;
import com.picdora.ui.FontHelper;
import com.picdora.ui.UiUtil;

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

		UiUtil.init(getApplicationContext());

		FontHelper.init(getApplicationContext());

		PicdoraImageLoader.init(this);

		initUniversalImageLoader();

		runMigrations();

		// clearCache();

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
				.showImageForEmptyUri(R.drawable.rect_white)
				.showImageOnLoading(R.drawable.rect_white)
				.showImageOnFail(R.drawable.rect_white).cacheInMemory(true)
				.cacheOnDisc(true).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(options)
				.build();

		ImageLoader.getInstance().init(config);

	}

	// Run db migrations with sprinkles
	private void runMigrations() {
		Sprinkles sprinkles = Sprinkles.init(getApplicationContext());

		// create models
		Migration addModelsMigration = new Migration();
		addModelsMigration.createTable(Image.class);
		addModelsMigration.createTable(Category.class);
		addModelsMigration.createTable(Channel.class);
		addModelsMigration.createTable(ChannelImage.class);
		sprinkles.addMigration(addModelsMigration);
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
