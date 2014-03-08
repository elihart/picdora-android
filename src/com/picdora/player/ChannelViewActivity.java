package com.picdora.player;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.player.ChannelPlayer.ChannelError;
import com.picdora.player.ChannelPlayer.OnGetImageResultListener;
import com.picdora.player.ChannelPlayer.OnLoadListener;

@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends FragmentActivity {
	private static final int NUM_IMAGES_TO_PRELOAD = 5;

	// Cache the last player used and remember the user's spot so they can
	// resume quickly
	private static CachedPlayerState cachedState;
	private boolean shouldCache;

	@ViewById
	PicdoraViewPager pager;
	@Bean
	ChannelPlayer mChannelPlayer;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	// Loading dialog to show while channel initializes
	private Dialog busyDialog;

	@AfterViews
	void initChannel() {
		// show loading screen
		showBusyDialog("Loading Channel...");

		// check if we should resume
		if (getIntent().getBooleanExtra("resume", false)) {

			if (cachedState == null) {
				// nothing to resume from...
				finish();
				return;
			} else {
				shouldCache = true;
				mChannelPlayer = cachedState.player;
				startChannel(cachedState.position);
				return;
			}
		}

		// Load channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);

		// we don't always want to cache what we're playing, as in the case of a preview
		shouldCache = getIntent().getBooleanExtra("cache", false);

		if (cachedState != null
				&& cachedState.player.getChannel().equals(channel)) {
			mChannelPlayer = cachedState.player;
			startChannel(cachedState.position);
		} else {
			// cache the player if requested
			if (shouldCache) {
				cachedState = new CachedPlayerState(mChannelPlayer, 0);
			}

			mChannelPlayer.loadChannel(channel, new OnLoadListener() {
				@Override
				public void onSuccess() {
					startChannel(0);
				}

				@Override
				public void onFailure(ChannelError error) {
					handleChannelLoadError(error);
				}
			});
		}
	}

	public static boolean hasCachedPlayer() {
		return cachedState != null;
	}

	public static String getCachedPlayerChannelName() {
		if (hasCachedPlayer()) {
			return cachedState.player.getChannel().getName();
		} else {
			return null;
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

	public void getImage(int position, boolean replacement, OnGetImageResultListener listener) {
		mChannelPlayer.getImage(position, replacement, listener);
	}
	

	protected void startChannel(int startingPosition) {
		// Instantiate a ViewPager and a PagerAdapter
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(mPagerAdapter);

		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int pos) {
				//preloadImages(pos + 1, pos + NUM_IMAGES_TO_PRELOAD);

				if (shouldCache) {
					cachedState.position = pos;
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});

		pager.setCurrentItem(startingPosition);
		//preloadImages(startingPosition, startingPosition
		//		+ NUM_IMAGES_TO_PRELOAD - 1);

		dismissBusyDialog();
	}

	/**
	 * Tell the imageloader to preload the images with positions between start
	 * and end, inclusive
	 * 
	 * @param startPos
	 *            Load the images in the range starting with this position
	 * @param endPos
	 *            The end of the image range, inclusive. Must be greater than
	 *            start pos or nothing is done
	 */
//	protected void preloadImages(int startPos, int endPos) {
//		if (endPos < startPos) {
//			return;
//		}
//
//		List<Image> images = new ArrayList<Image>();
//
//		// add the images to the list with the earlier images at the front so
//		// that they will be loaded first
//		for (int i = startPos; i <= endPos; i++) {
//			mChannelPlayer.getImage(i, false, new OnGetImageResultListener() {
//				
//				@Override
//				public void onGetImageResult(Image image) {
//					
//					
//				}
//			});
//		}
//
//		PicdoraImageLoader.instance().preloadImages(images);
//	}

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
	public void onDestroy() {
		super.onDestroy();

		PicdoraImageLoader.instance().clearDownloads();
	}

	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return ImageSwipeFragment_.builder().fragPosition(position).build();
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}
	}

	private class CachedPlayerState {
		public ChannelPlayer player;
		public int position;

		public CachedPlayerState(ChannelPlayer player, int position) {
			this.player = player;
			this.position = position;
		}
	}

}
