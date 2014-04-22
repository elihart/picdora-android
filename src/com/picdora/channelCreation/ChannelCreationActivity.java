package com.picdora.channelCreation;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;

import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.models.Category;

/**
 * This activity guides the user through creating a new channel. It consists of
 * a viewpager with two fragments. The first collects info about the channel -
 * name, gif setting, and nsfw setting. The second allows the user to choose
 * which categories to include.
 * 
 */

@EActivity(R.layout.activity_channel_creation)
public class ChannelCreationActivity extends PicdoraActivity {
	@ViewById
	ChannelCreationViewPager pager;
	@Pref
	PicdoraPreferences_ prefs;
	@Bean
	ChannelCreationUtil mUtils;

	private ChannelCreationPagerAdapter pagerAdapter;

	/** The new channel info submitted from the info fragment. */
	private ChannelCreationInfo mInfo;

	/**
	 * Whether nsfw categories should be displayed.
	 */
	public enum NsfwSetting {
		/** Don't show anything nsfw. */
		NONE,
		/** Show both nsfw and sfw. */
		ALLOWED,
		/** Show only nsfw */
		ONLY
	}

	@AfterViews
	void initViews() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

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
				mUtils.setLoadingStatus(false);
				// change title to this category
				if (position == 0) {
					pager.setPagingEnabled(false);
				} else {
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
	public void onBackPressed() {
		// cancel loading if it is in progress
		if (mUtils.isLoading()) {
			mUtils.setLoadingStatus(false);
		} else if (pager.getCurrentItem() == 0) {
			// leave the activity if we're on the first page
			super.onBackPressed();
		} else {
			// Otherwise, select the previous step.
			pager.setCurrentItem(pager.getCurrentItem() - 1);
		}
	}

	/**
	 * Submit the channel info that the user entered and go on to the category
	 * select screen.
	 * 
	 * @param info
	 */
	public void submitChannelInfo(ChannelCreationInfo info) {
		mInfo = info;
		pager.setCurrentItem(pagerAdapter.getCategoryFragmentPosition(), true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * If the home (up) button is pressed then cancel and loading in
		 * progress first, otherwise go to the previous fragment, and if we are
		 * on the first fragment then go back (default).
		 */
		switch (item.getItemId()) {
		case android.R.id.home:
			// cancel loading
			mUtils.setLoadingStatus(false);

			/*
			 * If we're not on the first page then go back 1 page. Otherwise let
			 * it do the default (return to parent)
			 */
			int pos = pager.getCurrentItem();
			if (pos > 0) {
				pager.setCurrentItem(pos - 1, true);
				return true;
			}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Create a channel with the given categories.
	 * 
	 * @param categories
	 * @param preview
	 *            Whether the channel should just be a preview and not saved.
	 */
	public void setChannelCategories(List<Category> categories, boolean preview) {
		mInfo.categories = categories;
		mInfo.preview = preview;
		/*
		 * Try to create the channel, don't skip showing the low image count
		 * warning since this will be the first attempt at the creation.
		 */
		mUtils.createChannel(mInfo, false);
	}

	/**
	 * Get the nsfw setting the user has chosen.
	 * 
	 * @return
	 */
	public NsfwSetting getNsfwSetting() {
		if (mInfo == null) {
			return NsfwSetting.NONE;
		} else {
			return mInfo.nsfwSetting;
		}
	}
}
