package com.picdora;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Fullscreen;
import com.googlecode.androidannotations.annotations.NoTitle;
import com.googlecode.androidannotations.annotations.ViewById;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.SlidingMenuHelper;

@NoTitle
@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends PicdoraActivity {

	 @ViewById
	ViewPager pager;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	private ChannelPlayer mChannelPlayer;
	
	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);		
		
		mChannelPlayer = new ChannelPlayer(channel);

		// Instantiate a ViewPager and a PagerAdapter
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(mPagerAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_view, menu);
		return true;
	}

	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			ImageSwipeFragment frag = new ImageSwipeFragment_();

			Image image = mChannelPlayer.getImage(position);
			image.markView();

			Bundle args = new Bundle();
			args.putString("imageJson", Util.toJson(image));
			frag.setArguments(args);

			return frag;
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}
	}

}
