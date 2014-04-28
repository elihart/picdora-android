package com.picdora.sync;

import java.io.IOException;
import java.io.InputStream;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.io.IOUtils;

import retrofit.client.Response;
import android.content.Context;

import com.picdora.PicdoraPreferences_;
import com.picdora.api.PicdoraApiService;

@EBean
public abstract class Syncer implements SyncTask {
	@Pref
	protected PicdoraPreferences_ mPrefs;
	@Bean
	protected PicdoraApiService mApiService;
	@RootContext
	protected Context mContext;

	private OnSyncTaskCompleteListener mOnCompleteListener;

	/**
	 * Turn a Retrofit response body into a string
	 * 
	 * @param response
	 * @return
	 * @throws IOException
	 */
	protected String responseToString(Response response) throws IOException {
		InputStream is = response.getBody().in();
		String result = IOUtils.toString(is);
		is.close();
		return result;
	}

	@Override
	public void run(OnSyncTaskCompleteListener listener) {
		mOnCompleteListener = listener;
		sync();
	}

	/**
	 * Implement this to run whatever tasks you need to do your sync. It will
	 * run in the background.
	 * 
	 */
	@Background
	protected abstract void sync();

	/**
	 * Call this when we're done syncing to alert the service that the task is
	 * done. This will be run on the ui thread.
	 * 
	 */
	@UiThread(propagation = Propagation.REUSE)
	protected void doneSyncing() {
		mOnCompleteListener.onSyncTaskComplete();
	}

	// TODO: Use a unified gson approach instead of parsing json ourselves
	// /**
	// * Get a JsonReader out of a Retrofit response. The returned reader must
	// be closed by the caller when they are finished with it
	// * @param response
	// * @return
	// * @throws IOException
	// */
	// protected JsonReader getResponseReader(Response response)
	// throws IOException {
	// InputStream is = response.getBody().in();
	// return new JsonReader(new InputStreamReader(is, "UTF-8"));
	// }
}
