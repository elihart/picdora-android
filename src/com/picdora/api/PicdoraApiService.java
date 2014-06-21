package com.picdora.api;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.POST;

import com.picdora.PicdoraApp;
import com.picdora.Util;

public class PicdoraApiService implements PicdoraApi {
	private static final String BASE_URL = "http://picdora.com:3000/";
	public static final String DEBUG_URL = "http://192.168.1.101:3000/";

	/** A public client that can be used to make static calls to the api. */
	private volatile static PicdoraApi client;

	/**
	 * Initialization of a client that can be used to make static api calls.
	 * Synchronized method with null check to avoid concurrent initializations
	 * as it should only be done once.
	 * 
	 * 
	 */
	private synchronized static void initClient() {
		if (client == null) {
			String url;
			if (PicdoraApp.DEBUG) {
				url = DEBUG_URL;
			} else {
				url = BASE_URL;
			}

			RestAdapter restAdapter = new RestAdapter.Builder()
					.setEndpoint(url).build();

			client = restAdapter.create(PicdoraApi.class);
		}
	}

	/**
	 * Get a copy of the API client to make API calls with.
	 * 
	 * @return
	 */
	public static PicdoraApi getClient() {
		if (client == null) {
			initClient();
		}

		return client;
	}

	@Override
	public Response categories() {
		try {
			return client.categories();
		} catch (Exception e) {
			Util.logException(e);
			return null;
		}
	}

	@Override
	@POST("/users/login")
	public Response login(String key) {
		try {
			return client.login(key);
		} catch (Exception e) {
			Util.logException(e);
			return null;
		}
	}

	@Override
	public Response topImages(long categoryId, int count) {
		try {
			return client.topImages(categoryId, count);
		} catch (Exception e) {
			Util.logException(e);
			return null;
		}
	}

	@Override
	public Response getImageUpdates(int minimumId, long lastUpdated,
			long createdBefore, int limit) {
		try {
			return client.getImageUpdates(minimumId, lastUpdated,
					createdBefore, limit);
		} catch (Exception e) {
			Util.logException(e);
			return null;
		}
	}

	@Override
	public void updateImage(String key, long imageId, boolean reported,
			boolean deleted, boolean gif, Callback<Response> cb) {
		try {
			client.updateImage(key, imageId, reported, deleted, gif, cb);
		} catch (Exception e) {
			Util.logException(e);
		}

	}

}
