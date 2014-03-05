package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;

@EActivity(R.layout.activity_channel_creation)
public class ChannelCreationActivity extends PicdoraActivity implements
		OnSharedPreferenceChangeListener {
	@ViewById
	ChannelCreationViewPager pager;
	@Pref
	PicdoraPreferences_ prefs;

	private PagerAdapter pagerAdapter;

	private OnFilterCategoriesListener categoryFilterListener;
	private NsfwSetting categoryFilter = NsfwSetting.NONE;

	// Whether the user has opted to show nsfw images in the settings, and a
	// listener to listen for this setting to change. If nsfw is turned off in
	// settings then we won't show the nsfw radio group
	private boolean allowNsfwPreference = false;
	private OnNsfwPreferenceChangeListener nsfwPreferenceChangeListener;

	// whether nsfw categories should be displayed. Don't include them, include
	// them and sfw, show only nsfw. If Allow nsfwPreference is false then this
	// will be NONE
	public enum NsfwSetting {
		NONE, ALLOWED, ONLY
	}

	// keep track of the info reported by the info fragment
	private ChannelCreationInfo channelInfo;

	@AfterViews
	void initViews() {

		pagerAdapter = new ChannelCreationPagerAdapter(
				getSupportFragmentManager());
		pager.setAdapter(pagerAdapter);

		// disable paging on the first screen, info, so the button has to be
		// pressed to continue. Enable it on the second screen, choosing
		// categories, so they can swipe to go back
		pager.setPagingEnabled(false);

		// listen for fragment page changes
		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// change title to this category
				if (position == 0) {
					setActionBarTitle("New Channel");
					pager.setPagingEnabled(false);
				} else {
					setActionBarTitle("Choose Categories");
					pager.setPagingEnabled(true);
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(prefs.showNsfw().key())) {
			checkNsfwPreference();
		}
	}

	private void checkNsfwPreference() {
		allowNsfwPreference = prefs.showNsfw().get();
		if (nsfwPreferenceChangeListener != null) {
			nsfwPreferenceChangeListener
					.onNsfwPreferenceChange(allowNsfwPreference);
		}

		if (!allowNsfwPreference) {
			categoryFilter = NsfwSetting.NONE;
			if(categoryFilterListener != null){
				categoryFilterListener.onFilterCategories(categoryFilter);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.registerOnSharedPreferenceChangeListener(this);
		checkNsfwPreference();
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	private class ChannelCreationPagerAdapter extends FragmentPagerAdapter {
		Fragment[] frags = { new ChannelInfoFragment_(),
				new CategorySelectFragment_() };

		public ChannelCreationPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return frags[position];
		}

		@Override
		public int getCount() {
			return frags.length;
		}
	}

	@Override
	public void onBackPressed() {
		if (pager.getCurrentItem() == 0) {
			// If the user is currently looking at the first step, allow the
			// system to handle the
			// Back button. This calls finish() on this activity and pops the
			// back stack.
			super.onBackPressed();
		} else {
			// Otherwise, select the previous step.
			pager.setCurrentItem(pager.getCurrentItem() - 1);
		}
	}

	public void submitChannelInfo(ChannelCreationInfo info) {
		channelInfo = info;
		categoryFilterListener.onFilterCategories(info.nsfwSetting);
		pager.setCurrentItem(1, true);
	}

	public void onNsfwSettingChanged(boolean showNsfw) {
		this.allowNsfwPreference = showNsfw;
		// TODO: update category list
	}

	// Listeners and methods to send nsfw settings info back and forth with the
	// info fragment

	public interface OnNsfwPreferenceChangeListener {
		public void onNsfwPreferenceChange(boolean showNsfw);
	}

	public void setOnNsfwChangeListener(OnNsfwPreferenceChangeListener listener) {
		nsfwPreferenceChangeListener = listener;
	}

	public boolean getNsfwPreference() {
		return allowNsfwPreference;
	}

	public void setCategoryFilter(NsfwSetting setting) {
		categoryFilter = setting;
		if (categoryFilterListener != null) {
			categoryFilterListener.onFilterCategories(setting);
		}
	}

	// set up interface for activity to tell us how to filter categories
	public interface OnFilterCategoriesListener {
		public void onFilterCategories(NsfwSetting setting);
	}

	public void setOnFilterCategoriesListener(
			OnFilterCategoriesListener listener) {
		categoryFilterListener = listener;
	}

	public NsfwSetting getCategoryFilter() {
		return categoryFilter;
	}

}
