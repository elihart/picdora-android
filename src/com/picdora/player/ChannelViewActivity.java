package com.picdora.player;

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
import com.picdora.imageloader.PicdoraImageLoader.OnDownloadSpaceAvailableListener;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.player.ChannelPlayer.ChannelError;
import com.picdora.player.ChannelPlayer.OnGetImageResultListener;
import com.picdora.player.ChannelPlayer.OnLoadListener;

@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends FragmentActivity implements
		OnDownloadSpaceAvailableListener {
	private static final int NUM_IMAGES_TO_PRELOAD = 5;

	// Cache the last player used and remember the user's spot so they can
	// resume quickly
	private static CachedPlayerState cachedState;
	private boolean shouldCache;
	private PicdoraImageLoader loader;

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

		loader = PicdoraImageLoader.instance();

		shouldCache = getIntent().getBooleanExtra("cache", false);

		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);

		// check if we can use the cached player, if not create a new one
		if (shouldCache && cachedState != null
				&& cachedState.player.getChannel().equals(channel)) {
			mChannelPlayer = cachedState.player;
			startChannel(cachedState.position);
		} else {
			// we don't always want to cache what we're playing, as in the case
			// of apreview. Cache the player if requested, overriding the old
			// one.
			// Otherwise leave the old one intact
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

	public static boolean hasCachedChannel() {
		return cachedState != null;
	}

	public static Channel getCachedChannel() {
		if (hasCachedChannel()) {
			return cachedState.player.getChannel();
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

	public void getImage(int position, boolean replacement,
			OnGetImageResultListener listener) {
		mChannelPlayer.getImage(position, replacement, listener);
	}

	protected void startChannel(int startingPosition) {
		// Instantiate a ViewPager and a PagerAdapter
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(mPagerAdapter);

		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int pos) {
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

		dismissBusyDialog();
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
	public void onPause() {
		super.onPause();
		loader.unregisterOnDownloadSpaceAvailableListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		loader.registerOnDownloadSpaceAvailableListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// if we are being destroyed because of orientation change then we don't
		// need to clear downloads, otherwise the activity is exiting and we can
		// clear them to save memory
		if (isFinishing()) {
			loader.clearDownloads();
		}
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

	@Override
	public void onDownloadSpaceAvailable() {

		int next = pager.getCurrentItem() + 1;
		for (int i = next; i < next + NUM_IMAGES_TO_PRELOAD; i++) {
			mChannelPlayer.getImage(i, false, new OnGetImageResultListener() {

				@Override
				public void onGetImageResult(Image image) {
					loader.preloadImage(image);
				}
			});
		}
	}

}
