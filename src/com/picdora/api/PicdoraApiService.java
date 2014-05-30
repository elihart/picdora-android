package com.picdora.api;

import org.androidannotations.annotations.EBean;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

import com.picdora.PicdoraApp;

@EBean
public class PicdoraApiService implements PicdoraApi {
	private static final String BASE_URL = "http://picdora.com:3000/";
	public static final String DEBUG_URL = "http://192.168.1.100:3000/";

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
	@GET("/categories")
	public Response categories() {
		try {
			return client.categories();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@POST("/users/login")
	public Response login(@Query("key") String key) {
		try {
			return client.login(key);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@GET("/images/top")
	public Response topImages(@Query("category_id") long categoryId,
			@Query("count") int count) {
		try {
			return client.topImages(categoryId, count);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@GET("/images/update")
	public Response updateImages(@Query("after_id") int minimumId,
			@Query("last_updated") long lastUpdated,
			@Query("created_before") long createdBefore,
			@Query("limit") int limit) {
		try {
			return client.updateImages(minimumId, lastUpdated, createdBefore,
					limit);
		} catch (Exception e) {
			return null;
		}
	}

}
