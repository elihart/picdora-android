package com.picdora.launch;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.Sprinkles;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.picdora.PicdoraApp;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.api.PicdoraApiService;
import com.picdora.channelSelection.ChannelSelectionActivity_;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.sync.PicdoraSyncService_;
import com.picdora.ui.FontHelper;
import com.picdora.ui.UiUtil;

@EActivity
public class LaunchActivity extends Activity {
	public static final String DB_NAME = "picdora.db";
	private static final int DB_VERSION = 2;

	@Pref
	protected PicdoraPreferences_ mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FontHelper.init(getApplicationContext());
		UiUtil.init(getApplicationContext());
		PicdoraImageLoader.init(this);
		initUniversalImageLoader();

		initDb();

		// only use crashlytics when not debugging
		if (!PicdoraApp.DEBUG) {
			Crashlytics.start(this);
		}

		/* Start the syncing service. */
		// startService(new Intent(this, PicdoraSyncService.class));
		PicdoraSyncService_.intent(this).start();

		/* Move on to the main activity. */
		startActivity(new Intent(this, ChannelSelectionActivity_.class));
		finish();
	}

	private void initUniversalImageLoader() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.showImageForEmptyUri(R.drawable.rect_white)
				.showImageOnLoading(R.drawable.rect_white)
				.showImageOnFail(R.drawable.rect_white).cacheInMemory(true)
				.cacheOnDisc(true).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(options)
				.build();

		ImageLoader.getInstance().init(config);
	}

	/**
	 * Run db migrations with sprinkles.
	 * 
	 */
	private void initDb() {
		/*
		 * Start up sprinkles and run any migrations we have to alter the
		 * default database.
		 */
		Sprinkles sprinkles = Sprinkles.init(getApplicationContext(), DB_NAME,
				DB_VERSION);

		/*
		 * Older migrations that were included in the now default db copied from
		 * assets.
		 */
		// create models
		// Migration addModelsMigration = new Migration();
		//
		// addModelsMigration.createTable(Image.class);
		// addModelsMigration.createTable(Category.class);
		// addModelsMigration.createTable(ImageCategory.class);
		//
		// addModelsMigration.createTable(Channel.class);
		// addModelsMigration.createTable(ChannelImage.class);
		// addModelsMigration.createTable(ChannelCategory.class);
		//
		// addModelsMigration.createTable(Collection.class);
		// addModelsMigration.createTable(CollectionItem.class);
		//
		// sprinkles.addMigration(addModelsMigration);
		//
		// Migration views = new Migration();
		//
		// views.addRawStatement("CREATE VIEW IF NOT EXISTS ImagesWithCategories AS SELECT * FROM Images JOIN ImageCategories ON Images.id = ImageCategories.imageId");
		// sprinkles.addMigration(views);
	}

}
