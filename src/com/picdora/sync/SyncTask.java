package com.picdora.sync;

public interface SyncTask {
	/** Execute the sync task. */
	public void run(OnSyncTaskCompleteListener listener);
}
