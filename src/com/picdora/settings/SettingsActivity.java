package com.picdora.settings;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;

import com.picdora.PicdoraActivity;
import com.picdora.PicdoraApp;
import com.picdora.R;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.SlidingMenuHelper;

/**
 * This activity handles preferences. It also has links to things like logging
 * out and the contact page. These are implemented as blank preferences with
 * click listeners, which makes them act like buttons
 * 
 */
@EActivity(R.layout.activity_settings)
public class SettingsActivity extends PicdoraActivity {
	/*
	 * TODO: Setup strings in resources, especially for preference titles and
	 * change those in the settings layout as well.
	 */
	@FragmentById(R.id.settings_fragment)
	protected SettingsFragment mSettingsFragment;
	private Context mContext;

	@AfterViews
	protected void init() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mContext = this;

		if (PicdoraApp.SFW_VERSION) {
			disableNsfw();
		}
	}

	@AfterViews
	protected void initContactOption() {
		Preference contactPref = mSettingsFragment.findPreference("pref_contact");
		contactPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// TODO Show contact info dialog.
				return true;
			}
		});
	}

	/**
	 * Don't allow the nsfw setting to be changed. If the user clicks it then
	 * show a dialog telling them that they don't have access to nsfw content.
	 */
	private void disableNsfw() {
		Preference showNsfw = mSettingsFragment.findPreference("showNsfw");

		showNsfw.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				showNsfwDisabledDialog();
				return true;
			}
		});

		showNsfw.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				/*
				 * We don't want to allow the user to turn nsfw on so return
				 * false to not allow the change.
				 */
				return false;
			}
		});
	}

	/**
	 * Show a dialog saying that nsfw content is disabled.
	 * 
	 */
	protected void showNsfwDisabledDialog() {
		new PicdoraDialog.Builder(mContext)
				.setTitle("Nsfw Disabled")
				.setMessage(
						"Sorry, but nsfw content isn't allowed in this version!")
				.setPositiveButton("Ok", null).show();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

}
