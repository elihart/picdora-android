package com.picdora.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.androidannotations.annotations.EBean;

import retrofit.client.Response;
import android.content.Context;

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
	private static final String KEY_FILE = "INSTALLATION_KEY";

	@Override
	protected void sync() {
		/* Get the device key and then send a login request with it. */

		String key = null;
		try {
			key = getDeviceKey(mContext);
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

	/**
	 * Get a key that uniquely identifies this device. If we already generated
	 * one then use that, otherwise generate a new one.
	 * <p>
	 * This is taken from
	 * http://android-developers.blogspot.com/2011/03/identifying
	 * -app-installations.html
	 * 
	 * @param context
	 * @return
	 */
	private synchronized String getDeviceKey(Context context) {
		File keyFile = new File(context.getFilesDir(), KEY_FILE);
		try {
			if (!keyFile.exists())
				writeInstallationFile(keyFile);
			return readKeyFile(keyFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String readKeyFile(File keyFile) throws IOException {
		RandomAccessFile f = new RandomAccessFile(keyFile, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	private void writeInstallationFile(File installation) throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

}
