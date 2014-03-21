package com.picdora.channelPlayer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
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
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelPlayer.ChannelPlayer.ChannelError;
import com.picdora.channelPlayer.ChannelPlayer.OnGetChannelImageResultListener;
import com.picdora.channelPlayer.ChannelPlayer.OnLoadListener;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.OnDownloadSpaceAvailableListener;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.ui.SatelliteMenu.SatelliteMenu;

@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends FragmentActivity implements
		OnDownloadSpaceAvailableListener {
	private static final int NUM_IMAGES_TO_PRELOAD = 5;

	// TODO: Sometimes loading seems to not begin.

	// TODO: Show icon notification icon in top right, such as like status,
	// download progress/completion, etc.

	// Cache the last player used and remember the user's spot so they can
	// resume quickly
	private static CachedPlayerState mCachedState;
	private boolean mShouldCache;
	private PicdoraImageLoader mIimageLoader;

	@ViewById
	protected RelativeLayout root;
	@ViewById
	protected PicdoraViewPager pager;
	@ViewById
	protected SatelliteMenu menu;
	
	@Bean
	protected ChannelPlayer mChannelPlayer;
	@Bean
	protected LikeGestureHandler mLikeGestureHandler;
	@Bean
	protected MenuManager mMenuManager;

	protected Activity mContext;

	// the fragment currently being viewed
	private ImageSwipeFragment mCurrFragment;

	// hold a copy of the player when orientation changes and the activity
	// recreates
	private static CachedPlayerState mOnConfigChangeState;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	// Loading dialog to show while channel initializes
	private Dialog busyDialog;

	/**
	 * Used when the user indicates a like or dislike on an image.
	 */
	public enum LIKE_EVENT {
		LIKED, DISLIKED;
	}

	@AfterViews
	void initChannel() {
		mContext = this;
		// show loading screen
		showBusyDialog("Loading Channel...");

		mMenuManager.initMenu(menu);

		// We don't use the Universal Image loader here, it's only used for
		// thumbnails, so lets clear out so memory and clear it's cache
		ImageLoader.getInstance().clearMemoryCache();

		mIimageLoader = PicdoraImageLoader.instance();

		mShouldCache = getIntent().getBooleanExtra("cache", false);

		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);

		// check if we can use the cached player, if not create a new one
		if (mOnConfigChangeState != null) {
			resumeState(mOnConfigChangeState);
		} else if (mShouldCache && mCachedState != null
				&& mCachedState.player.getChannel().equals(channel)) {
			resumeState(mCachedState);
		} else {
			/*
			 * We don't always want to cache what we're playing, as in the case
			 * of apreview. Cache the player if requested, overriding the old
			 * one. Otherwise leave the old one intact.
			 */
			if (mShouldCache) {
				mCachedState = new CachedPlayerState(mChannelPlayer, 0);
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

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// Give the touch event to our gesture detector first before passing it
		// on
		mLikeGestureHandler.checkTouchForLikeGesture(ev);
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * Check if the currently visible image is zoomed
	 * 
	 * @return
	 */
	public boolean isZoomed() {
		if (mCurrFragment == null) {
			return false;
		} else {
			return mCurrFragment.isZoomed();
		}
	}

	/**
	 * Set the current fragment being viewed. This should be called from the
	 * ImageSwipeFragment.
	 * 
	 * @param frag
	 */
	public void setCurrentFragment(ImageSwipeFragment frag) {
		mCurrFragment = frag;
	}

	

	private void resumeState(CachedPlayerState state) {
		mChannelPlayer = state.player;
		startChannel(state.position);
	}

	public static boolean hasCachedChannel() {
		return mCachedState != null;
	}

	public static Channel getCachedChannel() {
		if (hasCachedChannel()) {
			return mCachedState.player.getChannel();
		} else {
			return null;
		}
	}

	protected void handleChannelLoadError(ChannelError error) {
		// don't cache a failed channel
		if (mShouldCache) {
			mCachedState = null;
		}

		String msg = "Sorry! We failed to load your channel :(";
		switch (error) {
		case NO_IMAGES:
			msg = "No images matching your settings! Try changing the gif setting or adding categories";
			break;
		default:
			break;
		}
		Util.makeBasicToast(this, msg);
		finish();
	}

	public void getImage(int position, boolean replacement,
			final OnGetChannelImageResultListener listener) {
		mChannelPlayer.getImageAsync(position, replacement, listener);
	}

	protected void startChannel(int startingPosition) {
		// Instantiate a ViewPager and a PagerAdapter
		mPagerAdapter = new ChannelViewPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(mPagerAdapter);

		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int pos) {
				if (mShouldCache) {
					mCachedState.position = pos;
				}
				// close menu when image changes
				mMenuManager.closeMenu();
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
		busyDialog = new Dialog(this, R.style.picdora_dialog_style);
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
			} catch (Exception e) {
				// catch the "View not attached to Window Manager" errors
			}

		busyDialog = null;
	}

	@Override
	public void onPause() {
		super.onPause();
		mIimageLoader.unregisterOnDownloadSpaceAvailableListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mIimageLoader.registerOnDownloadSpaceAvailableListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// if we are being destroyed because of orientation change then we don't
		// need to clear downloads, otherwise the activity is exiting and we can
		// clear them to save memory
		if (isFinishing()) {
			mIimageLoader.clearDownloads();
			mChannelPlayer = null;
			mOnConfigChangeState = null;
		} else {
			mOnConfigChangeState = new CachedPlayerState(mChannelPlayer,
					pager.getCurrentItem());
		}

		dismissBusyDialog();
	}

	private class ChannelViewPagerAdapter extends FragmentStatePagerAdapter {

		public ChannelViewPagerAdapter(FragmentManager fm) {
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
			mChannelPlayer.getImageAsync(i, false,
					new OnGetChannelImageResultListener() {

						@Override
						public void onGetChannelImageResult(ChannelImage image) {
							mIimageLoader.preloadImage(image.getImage());
						}
					});
		}
	}

	/**
	 * Get the image fragment currently visible
	 * 
	 * @return
	 */
	public ImageSwipeFragment getCurrentFragment() {
		return mCurrFragment;
	}
	
	/**
	 * Get the image being shown by the currently visible fragment
	 * 
	 * @return The image being shown or null if not available.
	 */
	public ChannelImage getCurrentImage() {
		if(mCurrFragment != null){
			return mCurrFragment.getImage();
		}else{
			return null;
		}
	}

	/**
	 * Get the root viewgroup of the activity
	 * 
	 * @return
	 */
	public ViewGroup getRootView() {
		return root;
	}

}
