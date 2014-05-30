package se.emilsjolander.sprinkles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DbOpenHelper extends SQLiteOpenHelper {
	private int baseVersion;
	private Context context;
	private static String DB_NAME;

	protected DbOpenHelper(Context context, String databaseName, int baseVersion) {
		super(context, databaseName, null, Sprinkles.sInstance.mMigrations
				.size() + baseVersion);
		this.baseVersion = baseVersion;
		this.context = context;

		DB_NAME = databaseName;
	}

	/**
	 * Check if the database has been created yet and a file exists. If not we
	 * should copy our default one from assets.
	 * 
	 * @return
	 */
	private boolean doesDatabaseExist() {
		File dbFile = new File(getDBPath());
		return dbFile.exists();
	}

	/**
	 * Get the path where the database should be.
	 * 
	 * @return
	 */
	private String getDBPath() {
		return "/data/data/com.picdora/databases/" + DB_NAME;
	}

	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		/*
		 * Check if the db exists and then open it. We need it created first
		 * before we can copy over it (this sometimes seems to be the case but
		 * not always, so better to use it) otherwise sometimes there is a file doesn't exist error
		 */
		boolean doesDbExist = doesDatabaseExist();
		SQLiteDatabase db = super.getWritableDatabase();

		/*
		 * If a database doesn't yet exist then we should copy in our default db
		 * from assets.
		 */
		if (!doesDbExist) {
			/* Need to close db before copying over it. */
			db.close();
			try {
				/* Copy the db in and open it for use. */
				copyDatabase();
				db = super.getWritableDatabase();
			} catch (IOException ex) {
				Log.e("Database Log",
						"Failed to copy correctly. " + ex.getLocalizedMessage());
			}
		}

		return db;
	}

	/**
	 * Copy our db from assets to the internal db path. This should only be done
	 * once when the app is first installed.
	 * 
	 * @throws IOException
	 */
	private void copyDatabase() throws IOException {
		InputStream is = context.getAssets().open("databases/" + DB_NAME);
		OutputStream os = new FileOutputStream(getDBPath());

		byte[] buffer = new byte[1024];
		while (is.read(buffer) > 0) {
			os.write(buffer);
		}

		os.flush();
		os.close();
		is.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		/*
		 * Since we're copying our own db in we should only run sprinkles
		 * migrations on upgrades.
		 */
		// executeMigrations(db, baseVersion,
		// Sprinkles.sInstance.mMigrations.size() + baseVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		executeMigrations(db, oldVersion, newVersion);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		db.execSQL("PRAGMA foreign_keys=ON;");
	}

	private void executeMigrations(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		for (int i = oldVersion; i < newVersion; i++) {
			Sprinkles.sInstance.mMigrations.get(i).execute(db);
		}
	}
}
