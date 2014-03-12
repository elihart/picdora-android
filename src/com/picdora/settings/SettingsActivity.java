package com.picdora.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.ui.SlidingMenuHelper;

/**
 * This activity handles preferences. It also has links to things like logging out and the contact page. These are implemented as blank
 * preferences with click listeners, which makes them act like buttons
 *
 */
public class SettingsActivity extends PicdoraActivity {
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);



		setContentView(R.layout.activity_settings);

		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mContext = this;

		// set click listeners for preferences
		SettingsFragment settingsFragment = (SettingsFragment) getSupportFragmentManager()
				.findFragmentById(R.id.settings_fragment);

//		Preference tut = settingsFragment.findPreference(TutorialHelper
//				.prefKey(mContext));
//		if (tut != null) {
//			tut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//
//				@Override
//				public boolean onPreferenceClick(Preference preference) {
//					// set tutorials to show again
//					TutorialHelper.resetTutorials(mContext);
//
//					// set the welcome screen to show next time
//					WelcomeActivity.setShown(mContext, false);
//					
//					Utility.toast(mContext, "Tutorials will be shown!");
//					return true;
//				}
//			});
//		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	

}
