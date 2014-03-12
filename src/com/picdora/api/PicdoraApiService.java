package com.picdora.api;

import org.androidannotations.annotations.EBean;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

@EBean
public class PicdoraApiService implements PicdoraApi {
	 private static final String BASE_URL = "http://picdora.com:3000/";
	//public static final String BASE_URL = "http://192.168.1.6:3000/";

	private static PicdoraApi client;

	public PicdoraApiService() {
		if (client == null) {
			RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(
					PicdoraApiService.BASE_URL).build();

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
