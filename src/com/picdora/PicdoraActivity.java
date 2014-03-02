package com.picdora;

import com.picdora.ui.FontHelper;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class PicdoraActivity extends ActionBarActivity {
	// we need to be able to access the drawer toggle at certain points in the
	// activity's life
	protected ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setActionBarTitle(null);
	}

	/**
	 * Set the action bar title
	 * 
	 * @param text
	 *            The text to set the title to. If null the default activity
	 *            title is used
	 */
	public void setActionBarTitle(String text) {
		try {
			String title = text != null ? text : getTitle().toString();
			getSupportActionBar().setTitle(
					FontHelper.styleString(title, FontHelper.STYLE.BOLD));
		} catch (Exception e) {
			e.printStackTrace();
			Util.log("Error setting title font");
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null) {
			mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void setDrawerToggle(ActionBarDrawerToggle toggle) {
		mDrawerToggle = toggle;
	}

}
