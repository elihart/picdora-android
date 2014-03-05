package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.picdora.PicdoraActivity;
import com.picdora.R;

@EActivity(R.layout.activity_channel_creation)
public class ChannelCreationActivity extends PicdoraActivity {
	@ViewById
	ViewPager pager;

	private PagerAdapter pagerAdapter;
	private boolean includeNsfw = false;

	@AfterViews
	void initViews() {

		pagerAdapter = new ChannelCreationPagerAdapter(
				getSupportFragmentManager());
		pager.setAdapter(pagerAdapter);

		// listen for fragment page changes
		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// change title to this category
				if (position == 0) {
					setActionBarTitle("New Channel");
				} else {
					setActionBarTitle("Choose Categories");
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	private class ChannelCreationPagerAdapter extends FragmentStatePagerAdapter {
		public ChannelCreationPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return new ChannelInfoFragment_();
			} else {
				CategorySelectFragment frag = new CategorySelectFragment_();
				
				return frag;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// call super so that the fragments get the result
		super.onActivityResult(requestCode, resultCode, data);
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

	public void next() {
		// TODO: Validate channel name

		pager.setCurrentItem(1, true);
	}

	public void onNsfwSettingChanged(boolean showNsfw) {
		includeNsfw = showNsfw;
		// TODO: update category list
	}

}
