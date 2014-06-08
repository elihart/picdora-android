package com.picdora;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import android.content.Context;

/**
 * Helpers to get a key that uniquely identifies this device. The key is
 * generated if none exists and is stored in a file in the app's internal files directory.
 * 
 */
public class DeviceKeyUtils {
	/** The device key. Cache it here after retrieving from file. */
	private volatile static String key = null;
	/** File name to store the key */
	private static final String KEY_FILE = "INSTALLATION_KEY";

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
	public static synchronized String getDeviceKey(Context context) {
		/*
		 * Check if key has been retrieved yet and use it if available.
		 * Otherwise check the file system or create one.
		 */
		if (!Util.isStringBlank(key)) {
			return key;
		}

		/* Check the file system for a stored key. */
		File keyFile = new File(context.getFilesDir(), KEY_FILE);
		try {
			/* Create the keyfile if it doesn't exist. */
			if (!keyFile.exists()) {
				writeInstallationFile(keyFile);
			}
			key = readKeyFile(keyFile);
			return key;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String readKeyFile(File keyFile) throws IOException {
		RandomAccessFile f = new RandomAccessFile(keyFile, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	private static void writeInstallationFile(File installation)
			throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

}
