package com.picdora.launch;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.Migration;
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
import com.picdora.channelSelection.ChannelSelectionActivity_;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.models.Category;
import com.picdora.models.CategoryImage;
import com.picdora.models.Channel;
import com.picdora.models.ChannelCategory;
import com.picdora.models.ChannelImage;
import com.picdora.models.Collection;
import com.picdora.models.CollectionItem;
import com.picdora.models.Image;
import com.picdora.sync.PicdoraSyncManager;
import com.picdora.ui.FontHelper;
import com.picdora.ui.UiUtil;

@EActivity
public class LaunchActivity extends Activity {
	@Bean
	protected PicdoraSyncManager mSyncManager;
	@Pref
	protected PicdoraPreferences_ mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		FontHelper.init(getApplicationContext());		
		UiUtil.init(getApplicationContext());
		PicdoraImageLoader.init(this);
		initUniversalImageLoader();

		runMigrations();

		// only use crashlytics when not debugging
		if (!PicdoraApp.DEBUG) {
			Crashlytics.start(this);
		}

		if (mPrefs.firstLaunch().get()) {
			// TODO: Insert base data into db
		}

		mSyncManager.sync();
		
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
	private void runMigrations() {
		Sprinkles sprinkles = Sprinkles.init(getApplicationContext());

		// create models
		Migration addModelsMigration = new Migration();
		
		addModelsMigration.createTable(Image.class);
		addModelsMigration.createTable(Category.class);
		addModelsMigration.createTable(CategoryImage.class);
		
		addModelsMigration.createTable(Channel.class);
		addModelsMigration.createTable(ChannelImage.class);
		addModelsMigration.createTable(ChannelCategory.class);
		
		addModelsMigration.createTable(Collection.class);
		addModelsMigration.createTable(CollectionItem.class);
		
		sprinkles.addMigration(addModelsMigration);
	}

}
