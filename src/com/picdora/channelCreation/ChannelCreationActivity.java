package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
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
	
	@AfterViews
	void initViews() {

		pagerAdapter = new ChannelCreationPagerAdapter(getSupportFragmentManager());
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
				return new CategorySelectFragment_();
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

	@Click
	void createButtonClicked() {
		// GifSetting gif = getGifSetting();
		//
		// String name = channelName.getText().toString();
		//
		// List<Category> categories =
		// categoryListAdapter.getSelectedCategories();
		//
		// if (categories.isEmpty()) {
		// Util.makeBasicToast(this, "You must select at least one category!");
		// return;
		// } else if (Util.isStringBlank(name)) {
		// channelName.setError("You have to give this channel a name!");
		// return;
		// }
		//
		// Channel channel = new Channel(name, categories, gif);
		// boolean success = channel.save();
		// Util.log("Create channel success? " + success);
		//
		// if (success) {
		// ChannelHelper.playChannel(channel, this);
		// finish();
		// }
	}

}
