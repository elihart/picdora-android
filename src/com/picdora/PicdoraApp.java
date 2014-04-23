package com.picdora;

import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Application;

import com.nostra13.universalimageloader.core.ImageLoader;

@EApplication
public class PicdoraApp extends Application {
	@Pref
	protected PicdoraPreferences_ mPrefs;

	public static final boolean DEBUG = true;
	public static final boolean SFW_VERSION = false;

	@Override
	public void onCreate() {
		super.onCreate();

		//resetApp();
		// clearCache();

		/* In the sfw version we make sure the nsfw preference is set to false. */
		if (SFW_VERSION) {
			mPrefs.showNsfw().put(false);
		}
	}

	/**
	 * Clear the memory caches of Ion and the UniversalImageLoader.
	 * 
	 */
	public void clearMemoryCaches() {
		/* Not using ion right now. */
		//Ion.getDefault(this).getBitmapCache().clear();
		ImageLoader.getInstance().clearMemoryCache();
	}

	/** Clear the database and reset all preferences. */
	private void resetApp() {
		deleteDatabase("sprinkles.db");
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

}
