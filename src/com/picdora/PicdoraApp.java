package com.picdora;

import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.launch.LaunchActivity;

@EApplication
public class PicdoraApp extends Application {
	@Pref
	protected PicdoraPreferences_ mPrefs;

	public static final boolean DEBUG = true;
	public static final boolean SFW_VERSION = false;
	/** Whether new images should be retrieved for all categories. */
	public static final boolean SEED_IMAGE_DATABASE = false;
	/** Store the application context for use by static methods outside. */
	private static Context context;
	/** Tracker for Google Analytics */
	private Tracker mTracker;

	@Override
	public void onCreate() {
		super.onCreate();

		context = getApplicationContext();

		// resetApp();
		// clearCache();

		/* In the sfw version we make sure the nsfw preference is set to false. */
		if (SFW_VERSION) {
			mPrefs.showNsfw().put(false);
		}

		/* TODO: Custom error handler. */
		// Thread.setDefaultUncaughtExceptionHandler();

		initTracker();
	}

	/**
	 * Initialize the google analytics tracker.
	 * 
	 */
	private void initTracker() {
		Tracker tracker = getTracker();
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
			//analytics.setDryRun(DEBUG);
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
		boolean success = deleteDatabase(LaunchActivity.DB_NAME);
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
