package com.picdora.models;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.picdora.Util;

public class AssetDatabaseOpenHelper extends SQLiteOpenHelper {

	private Context context;
	private String databaseName;
	/** The directory in the assets folder that the database file is stored in. */
	private final static String ASSET_FOLDER = "databases/";

	public AssetDatabaseOpenHelper(Context context, String databaseName, int databaseVersion) {
		super(context, databaseName, null, databaseVersion);
		this.context = context;
		this.databaseName = databaseName;
	}
	
	@Override
	public void onCreate(SQLiteDatabase arg0) {
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		
	}

	public void copyDatabaseIfNotExists() {		
		File dbFile = context.getDatabasePath(databaseName);		
		if (!dbFile.exists()) {
			try {
				copyDatabase(dbFile);
				getWritableDatabase().close();
			} catch (IOException e) {
				Util.logException(e);
			}
		}
	}

	private void copyDatabase(File dbFile) throws IOException {
		InputStream is = context.getAssets().open(ASSET_FOLDER + databaseName);
		OutputStream os = new FileOutputStream(dbFile);

		byte[] buffer = new byte[1024];
		while (is.read(buffer) > 0) {
			os.write(buffer);
		}

		os.flush();
		os.close();
		is.close();
	}

	
}
