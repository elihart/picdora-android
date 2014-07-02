package com.picdora.launch;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

@EBean
public class NetworkChecker {
	@RootContext
	Context context;

	/**
	 * Get info about the current default network.
	 * 
	 * @return
	 */
	private NetworkInfo getNetworkInfo() {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo();
	}

	/**
	 * Check if the device is connected to the internet.
	 * 
	 * @return
	 */
	public boolean isNetworkConnected() {
		return isNetworkConnected(getNetworkInfo());
	}

	/**
	 * Check if the network with the given info is connected or connecting.
	 * 
	 * @param info
	 * @return
	 */
	private boolean isNetworkConnected(NetworkInfo info) {
		return info != null && info.isConnectedOrConnecting();
	}

	/**
	 * Check if the device is using a mobile data network.
	 * 
	 * @return
	 */
	public boolean isUsingMobileNetwork() {
		NetworkInfo info = getNetworkInfo();
		/* False if not connected to any network. */
		if(!isNetworkConnected(info)) {
			return false;
		}
		
		return info.getType() == ConnectivityManager.TYPE_MOBILE;
	}
}
