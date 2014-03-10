package com.picdora.api;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public interface PicdoraApi {
	@GET("/images/new")
	public void newImages(@Query("id") int afterId, @Query("time") long afterTime, @Query("limit") int batchSize, Callback<Response> cb);

}
