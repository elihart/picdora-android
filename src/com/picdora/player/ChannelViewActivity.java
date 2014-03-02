package com.picdora.player;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.Util;
import com.picdora.imageloader.ImageLoader;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.player.ChannelPlayer.ChannelError;
import com.picdora.player.ChannelPlayer.OnReadyListener;

@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends FragmentActivity {
	private static final int NUM_IMAGES_TO_PRELOAD = 5;

	@ViewById
	PicdoraViewPager pager;
	@Bean
	ChannelPlayer mChannelPlayer;

	private DataFragment dataFragment;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	// Loading dialog to show while channel initializes
	private Dialog busyDialog;

	@AfterViews
	void initChannel() {

		FragmentManager fm = getSupportFragmentManager();
		dataFragment = (DataFragment) fm.findFragmentByTag("data");

		// create the fragment and data the first time
		if (dataFragment == null) {
			// show loading screen
			showBusyDialog("Loading Channel...");

			// add the fragment
			dataFragment = new DataFragment();
			fm.beginTransaction().add(dataFragment, "data").commit();

			dataFragment.setData(mChannelPlayer);

			// Load channel and play when ready
			String json = getIntent().getStringExtra("channel");
			Channel channel = Util.fromJson(json, Channel.class);

			mChannelPlayer.loadChannel(channel, new OnReadyListener() {

				@Override
				public void onReady() {
					dismissBusyDialog();
					startChannel();
				}

				@Override
				public void onError(ChannelError error) {
					handleChannelLoadError(error);
				}
			});

		} else {
			mChannelPlayer = dataFragment.getData();
			startChannel();
		}

	}

	protected void handleChannelLoadError(ChannelError error) {
		String msg = "Sorry! We failed to load your channel :(";
		switch (error) {
		case NO_IMAGES:
			msg = "Uh oh! Unable to load images for this channel :(";
			break;
		default:
			break;
		}
		Util.makeBasicToast(this, msg);
		finish();
	}

	protected void startChannel() {
		// Instantiate a ViewPager and a PagerAdapter
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(mPagerAdapter);
		
		preloadImages(0, NUM_IMAGES_TO_PRELOAD - 1);

		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int pos) {
				preloadImages(pos + 1, pos + NUM_IMAGES_TO_PRELOAD);

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

	/**
	 * Tell the imageloader to preload the images with positions
	 * between start and end, inclusive
	 * 
	 * @param startPos Load the images in the range starting with this position
	 * @param endPos The end of the image range, inclusive. Must be greater than start pos or nothing is done
	 */
	protected void preloadImages(int startPos, int endPos) {
		if(endPos < startPos){
			return;
		}
		
		for(int i = endPos; i >= startPos; i--){
			Image image = mChannelPlayer.getImage(i);
			ImageLoader.instance().preloadImage(image);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_view, menu);
		return true;
	}

	public void showBusyDialog(String message) {
		busyDialog = new Dialog(this, R.style.lightbox_dialog);
		busyDialog.setContentView(R.layout.lightbox_dialog);
		((TextView) busyDialog.findViewById(R.id.dialogText)).setText(message);

		// if the user presses back while the loading dialog is up we want to
		// cancel the whole activity and go back. Otherwise just the dialog will
		// be canceled and they'll be left with a blank screen
		busyDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				finish();
			}
		});

		busyDialog.show();
	}

	public void dismissBusyDialog() {
		if (busyDialog != null)
			try {
				busyDialog.dismiss();
			} catch (IllegalArgumentException e) {
				// catch the "View not attached to Window Manager" errors
			}

		busyDialog = null;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		ImageLoader.instance().clearDownloads();
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
