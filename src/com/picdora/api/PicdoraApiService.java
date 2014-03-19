package com.picdora.api;

import org.androidannotations.annotations.EBean;

import com.picdora.PicdoraApp;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

@EBean
public class PicdoraApiService implements PicdoraApi {
	private static final String BASE_URL = "http://picdora.com:3000/";
	public static final String DEBUG_URL = "http://192.168.1.5:3000/";

	private static PicdoraApi client;

	public PicdoraApiService() {
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

	@Override
	@GET("/images/new")
	public Response newImages(@Query("id") int afterId,
			@Query("time") long afterTime, @Query("limit") int batchSize) {
		try {
			return client.newImages(afterId, afterTime, batchSize);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@GET("/categories")
	public Response categories() {
		try {
			return client.categories();
		} catch (Exception e) {
			return null;
		}
	}

}
