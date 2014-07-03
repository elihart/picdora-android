package com.picdora.sync;

import java.util.LinkedList;
import java.util.Queue;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.picdora.PicdoraPreferences_;
import com.picdora.api.NetworkChecker;

/**
 * This class keeps our local databases up to date with the server.
 * 
 */
@EService
public class PicdoraSyncService extends Service implements
		OnSyncTaskCompleteListener {
	@Bean
	NetworkChecker mNetworkChecker;

	/** Whether the service is currently doing a sync. */
	private static boolean mSyncing = false;

	/** The sync jobs that need to be performed. */
	private Queue<SyncTask> mSyncTasks;

	@Pref
	protected PicdoraPreferences_ mPrefs;

	@Override
	public IBinder onBind(Intent intent) {
		// We don't allow binding.
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/*
		 * Start the sync if we're not already doing it and we have a network
		 * connection.
		 */
		if (!mSyncing && mNetworkChecker.isNetworkConnected()) {
			mSyncing = true;
			startSync();
		}

		/*
		 * We don't need to be restarted if we're destroyed. We'll just wait
		 * till the next sync.
		 */
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSyncing = false;
	}

	/**
	 * Sync our local data with the server as necessary. Stop the service when
	 * done.
	 * 
	 */
	private void startSync() {
		mSyncTasks = new LinkedList<SyncTask>();

		/* Login before starting the main syncs. */
		mSyncTasks.add(LoginTask_.getInstance_(this));
		/*
		 * Sync the categories first, then the images and channels. Categories
		 * need to be before images because we base what images we need based on
		 * the state of the categories.
		 */
		mSyncTasks.add(CategorySyncer_.getInstance_(this));
		mSyncTasks.add(ImageSyncer_.getInstance_(this));
		mSyncTasks.add(ChannelSyncer_.getInstance_(this));

		doNextSyncTask();
	}

	/**
	 * Execute the next sync task in the queue if there is one and if we are
	 * still set to sync. If not then we are done syncing.
	 * 
	 */
	private void doNextSyncTask() {
		if (!mSyncTasks.isEmpty() && mSyncing) {
			/* Remove and start the next task in the queue. */
			SyncTask task = mSyncTasks.remove();
			task.run(this);
		} else {
			// Done!
			mSyncing = false;
		}
	}

	@Override
	public void onSyncTaskComplete() {
		/* Try to run the next task. */
		doNextSyncTask();
	}

}
