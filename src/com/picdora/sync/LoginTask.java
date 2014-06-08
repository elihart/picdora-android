package com.picdora.sync;

import org.androidannotations.annotations.EBean;

import retrofit.client.Response;

import com.picdora.DeviceKeyUtils;
import com.picdora.Util;

/**
 * Sends a "login" post to the server. This isn't an authenticated login in the
 * conventional sense, but simply tells the service that this device is lauching
 * the app so it can keep track of usage stats. A UUID is used to identify this
 * app installation. It will be generated the first time the login is run, and
 * will be stored in a file for future use.
 * 
 */
@EBean
public class LoginTask extends Syncer {
	

	@Override
	protected void sync() {
		/* Get the device key and then send a login request with it. */

		String key = null;
		try {
			key = DeviceKeyUtils.getDeviceKey(mContext);
		} catch (RuntimeException e) {
			Util.logException(e);
			doneSyncing();
		}

		/*
		 * We don't really care about the response... we're not going to retry
		 * if the login fails, since it's really only for tracking usage data.
		 */
		Response response = mApiService.login(key);

		doneSyncing();
	}
}
