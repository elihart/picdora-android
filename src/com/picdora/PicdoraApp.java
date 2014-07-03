package com.picdora;

import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.sync.PicdoraSyncService_;
import com.picdora.ui.FontHelper;
import com.picdora.ui.UiUtil;

@EApplication
public class PicdoraApp extends Application {
	@Pref
	protected PicdoraPreferences_ mPrefs;

	public static final boolean DEBUG = true;
	public static final boolean SFW_VERSION = false;
	/** Whether new images should be retrieved for all categories. */
	public static final boolean SEED_IMAGE_DATABASE = false;
	public static final String DB_NAME = "picdora.db";
	private static final int DB_VERSION = 2;
	/** Store the application context for use by static methods outside. */
	private static Context context;
	/** Tracker for Google Analytics */
	private Tracker mTracker;

	@Override
	public void onCreate() {
		super.onCreate();

		initCrashlytics();
		initGA();
		FontHelper.init(getApplicationContext());
		UiUtil.init(getApplicationContext());
		PicdoraImageLoader.init(this);
		initUniversalImageLoader();
		initDb();

		context = getApplicationContext();

		/* In the sfw version we make sure the nsfw preference is set to false. */
		if (SFW_VERSION) {
			mPrefs.showNsfw().put(false);
		}

		/* TODO: Custom error handler. */
		// Thread.setDefaultUncaughtExceptionHandler();

		/* Start the syncing service. */
		PicdoraSyncService_.intent(this).start();
	}

	private void initCrashlytics() {
		// use specific tag for crashlytics when debugging
		Crashlytics.start(this);
		if (PicdoraApp.DEBUG) {
			Crashlytics.setBool("debug", true);
		}
	}

	private void initGA() {
		// init GA tracking
		Tracker tracker = getTracker();
		tracker.set("nsfw", Boolean.toString(mPrefs.showNsfw().get()));
	}

	private void initUniversalImageLoader() {
		// TODO: Maybe a special loading/failure image
		// DisplayImageOptions options = new DisplayImageOptions.Builder()
		// .showImageForEmptyUri(R.drawable.rect_black)
		// .showImageOnLoading(R.drawable.rect_black)
		// .showImageOnFail(R.drawable.rect_black).cacheInMemory(true)
		// .cacheOnDisc(true).build();
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheInMemory(true).cacheOnDisc(true).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(options)
				.build();

		ImageLoader.getInstance().init(config);
	}

	/**
	 * Run db migrations with sprinkles.
	 * 
	 */
	private void initDb() {
		/*
		 * Start up sprinkles and run any migrations we have to alter the
		 * default database.
		 */
		Sprinkles sprinkles = Sprinkles.init(getApplicationContext(), DB_NAME,
				DB_VERSION);

		/*
		 * Older migrations that were included in the now default db copied from
		 * assets.
		 */
		// create models
		// Migration addModelsMigration = new Migration();
		//
		// addModelsMigration.createTable(Image.class);
		// addModelsMigration.createTable(Category.class);
		// addModelsMigration.createTable(ImageCategory.class);
		//
		// addModelsMigration.createTable(Channel.class);
		// addModelsMigration.createTable(ChannelImage.class);
		// addModelsMigration.createTable(ChannelCategory.class);
		//
		// addModelsMigration.createTable(Collection.class);
		// addModelsMigration.createTable(CollectionItem.class);
		//
		// sprinkles.addMigration(addModelsMigration);
		//
		// Migration views = new Migration();
		//
		// views.addRawStatement("CREATE VIEW IF NOT EXISTS ImagesWithCategories AS SELECT * FROM Images JOIN ImageCategories ON Images.id = ImageCategories.imageId");
		// sprinkles.addMigration(views);
	}

	/**
	 * Get the Google Analytics tracker.
	 * 
	 * @return
	 */
	public synchronized Tracker getTracker() {
		/* Initialize the tracker if needed and return. */
		if (mTracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			/* Don't send data when in debug mode. */
			analytics.setDryRun(DEBUG);
			mTracker = analytics.newTracker(R.xml.google_analytics_tracker);
		}
		return mTracker;
	}

	/**
	 * Clear the memory caches of Ion and the UniversalImageLoader.
	 * 
	 */
	public void clearMemoryCaches() {
		/* Not using ion right now. */
		// Ion.getDefault(this).getBitmapCache().clear();
		ImageLoader.getInstance().clearMemoryCache();
	}

	/** Clear the database and reset all preferences. */
	private void resetApp() {
		boolean success = deleteDatabase(DB_NAME);
		mPrefs.clear();
	}

	@Override
	public void onLowMemory() {
		trimMemory();
	}

	private void trimMemory() {
		clearMemoryCaches();
		System.gc();
	}

	/**
	 * Get the application context. Will return null before the App's onCreate
	 * method is called.
	 * 
	 * @return
	 */
	public static Context getAppContext() {
		return context;
	}

}
