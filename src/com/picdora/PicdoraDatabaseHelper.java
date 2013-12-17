package com.picdora;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PicdoraDatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "picdora";
	private static final int DATABASE_VERSION = 1;

	public PicdoraDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE images (_id INTEGER PRIMARY KEY, ImgurId TEXT,RedditScore INTEGER,CategoryId INTEGER,AlbumId INTEGER,Reported BOOLEAN,Deleted BOOLEAN,Nsfw BOOLEAN,Porn BOOLEAN,Gif BOOLEAN,Landscape BOOLEAN,ViewCount INTEGER DEFAULT 0,LastViewed INTEGER DEFAULT 0,Liked BOOLEAN,Favorite BOOLEAN);");
		//db.execSQL("CREATE TABLE images (_id INTEGER PRIMARY KEY, ImgurId TEXT);");
		//db.execSQL("INSERT INTO images (_id,ImgurId) VALUES (654, 'eli');");		
		db.execSQL("INSERT INTO images VALUES (1, 'erweasd', 300, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);");		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS images");
		onCreate(db);
	}
}
