package com.picdora.sync;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.picdora.PicdoraPreferences_;

/**
 * This class keeps our local databases up to date with the server.
 * 
 */
@EBean
public class PicdoraSyncManager {
	// TODO: More efficient syncing. Instead of doing it everytime, maybe get
	// timestamps from the server on when images and categories were last
	// updated, and only tell them to sync if they're needed. We can have the
	// syncers return true or false on success

	// TODO: Way down the line we can worry about channel syncing. This might
	// just be anonymous like/dislike info to build a recommendation system
	@Bean
	protected ImageSyncer mImageSyncer;
	@Bean
	protected CategorySyncer mCategorySyncer;
	@Bean
	protected ChannelSyncer mChannelSyncer;

	@Pref
	protected PicdoraPreferences_ mPrefs;

	/**
	 * Checks if our local db needs updating, and syncs the tables that are out
	 * of date
	 */
	public void sync() {
		doCategorySync();
		doImageSync();
		doChannelSync();
	}

	@Background(serial = "sync")
	protected void doCategorySync() {
		mCategorySyncer.sync();
	}

	@Background(serial = "sync")
	protected void doImageSync() {
		mImageSyncer.sync();
	}

	@Background(serial = "sync")
	protected void doChannelSync() {
		mChannelSyncer.sync();
	}

	public interface OnSyncResultListener {
		public void onSuccess();

		public void onFailure();
	}

}
