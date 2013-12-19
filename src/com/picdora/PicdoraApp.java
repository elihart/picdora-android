package com.picdora;

import se.emilsjolander.sprinkles.Migration;
import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Application;

public class PicdoraApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		Util.log("Creating app");

		runMigrations();
	}

	// Run db migrations with sprinkles
	private void runMigrations() {
		Sprinkles sprinkles = Sprinkles.getInstance(getApplicationContext());

		// create Images
        Migration initialMigration = new Migration();
        initialMigration.createTable(Image.class);
        sprinkles.addMigration(initialMigration);
        
        // get some images
        ImageManager.getImagesFromServer(100);
	}

}
