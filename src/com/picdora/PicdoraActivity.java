package com.picdora;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.picdora.ui.FontHelper;

public class PicdoraActivity extends ActionBarActivity {
	// we need to be able to access the drawer toggle at certain points in the
	// activity's life
	protected ActionBarDrawerToggle mDrawerToggle;

	/** Use this fragment to retain state on config changes */
	private RetainedFragment mRetainedFragment;
	/**
	 * Tag to use when adding {@link #mRetainedFragment} to the fragment manager
	 */
	private static final String RETAINED_FRAGMENT_TAG = "retainedFragment";

	/**
	 * Keep track of whether the activity has been destroyed for use in
	 * {@link #isDestroyed()}
	 */
	private volatile boolean mDestroyed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setActionBarTitle(null);

		// find the retained fragment on activity restart
		FragmentManager fm = getSupportFragmentManager();
		mRetainedFragment = (RetainedFragment) fm
				.findFragmentByTag(RETAINED_FRAGMENT_TAG);

		// create the fragment and data the first time
		if (mRetainedFragment == null) {
			// add the fragment
			mRetainedFragment = new RetainedFragment();
			fm.beginTransaction().add(mRetainedFragment, RETAINED_FRAGMENT_TAG)
					.commit();
		}

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
					FontHelper.styleString(title, FontHelper.FontStyle.MEDIUM));
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

	/**
	 * Whether the onDestroy method has been called for this activity instance.
	 * Does the same thing as {@link #isDestroyed()} but works for all api
	 * levels.
	 */
	public boolean isDestroyedCompat() {
		return mDestroyed;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDestroyed = true;

		if (!isFinishing()) {
			mRetainedFragment.setData(onRetainState());
		}
	}

	/**
	 * Called when the activity is being destroyed in order to be recreated.
	 * Lets you save state on config changes. This is similar to the deprecated
	 * {@link #onRetainNonConfigurationInstance()} but this method is customized
	 * to use fragments. Override this to save your custom state object.
	 * 
	 * @return Any object you want to save for when the activity is recreated.
	 */
	protected Object onRetainState() {
		return null;
	}

	/**
	 * Get an object returned in {@link #onRetainState())}. This will persist
	 * across config changes, but not if the activity is finished().
	 * 
	 * @return The saved state, or null if nothing was saved.
	 */
	protected Object getRetainedState() {
		return mRetainedFragment.getData();
	}

	/**
	 * Fragment used to saved a state object when a fragment is recreated for
	 * config changes.
	 */
	public static class RetainedFragment extends Fragment {

		// data object we want to retain
		private Object data;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// retain this fragment
			setRetainInstance(true);
		}

		public void setData(Object data) {
			this.data = data;
		}

		public Object getData() {
			return data;
		}
	}
}
