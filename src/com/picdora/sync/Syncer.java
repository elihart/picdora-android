package com.picdora.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.io.IOUtils;

import retrofit.client.Response;

import com.picdora.PicdoraPreferences_;
import com.picdora.api.PicdoraApiService;

@EBean
public abstract class Syncer {
	@Pref
	protected PicdoraPreferences_ mPrefs;
	@Bean
	protected PicdoraApiService mApiService;

	/**
	 * Sync the local db with the server if it is out of date. Network and DB
	 * access is done synchronously, so this must done be called on the ui
	 * thread
	 */
	public abstract void sync();

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
//		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//		StringBuilder total = new StringBuilder(is.available());
//		String line;
//		while ((line = reader.readLine()) != null) {
//			total.append(line);
//		}
//		is.close();
//
//		return reader.toString();
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
