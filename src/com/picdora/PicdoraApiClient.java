package com.picdora;

import com.picdora.loopj.AsyncHttpClient;
import com.picdora.loopj.AsyncHttpResponseHandler;
import com.picdora.loopj.RequestParams;

public class PicdoraApiClient {
//	private static final String BASE_URL = "http://picdora.com:3000/";
	private static final String BASE_URL = "http://192.168.1.15:3000/";

	private static AsyncHttpClient client = new AsyncHttpClient();

	public static void get(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		client.get(getAbsoluteUrl(url), params, responseHandler);
	}

	public static void get(String url, AsyncHttpResponseHandler responseHandler) {
		client.get(getAbsoluteUrl(url), responseHandler);
	}

	public static void post(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		client.post(getAbsoluteUrl(url), params, responseHandler);
	}

	private static String getAbsoluteUrl(String relativeUrl) {
		return BASE_URL + relativeUrl;
	}

}
