package com.picdora.player;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.OnDownloadSpaceAvailableListener;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelImage.LIKE_STATUS;
import com.picdora.models.Image;
import com.picdora.player.ChannelPlayer.ChannelError;
import com.picdora.player.ChannelPlayer.OnGetChannelImageResultListener;
import com.picdora.player.ChannelPlayer.OnLoadListener;
import com.picdora.ui.SatelliteMenu.SatelliteMenu;
import com.picdora.ui.SatelliteMenu.SatelliteMenu.SateliteClickedListener;
import com.picdora.ui.SatelliteMenu.SatelliteMenuItem;

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
	protected PicdoraViewPager pager;
	@ViewById
	protected SatelliteMenu menu;
	@Bean
	protected ChannelPlayer mChannelPlayer;

	// hold a copy of the player when orientation changes and the activity
	// recreates
	private static CachedPlayerState mOnConfigChangeState;

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

		setupMenu();

		// We don't use the Universal Image loader here, it's only used for
		// thumbnails, so lets clear out so memory and clear it's cache
		ImageLoader.getInstance().clearMemoryCache();

		loader = PicdoraImageLoader.instance();

		shouldCache = getIntent().getBooleanExtra("cache", false);

		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);

		channel.setLastUsed(new Date());
		channel.saveAsync();

		// check if we can use the cached player, if not create a new one
		if (mOnConfigChangeState != null) {
			resumeState(mOnConfigChangeState);
		} else if (shouldCache && cachedState != null
				&& cachedState.player.getChannel().equals(channel)) {
			resumeState(cachedState);
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

	/**
	 * Create a satellite menu to provide access to options for each picture
	 */
	private void setupMenu() {
		List<SatelliteMenuItem> items = new ArrayList<SatelliteMenuItem>();
		items.add(new SatelliteMenuItem(R.id.sat_item_report,
				R.drawable.ic_sat_menu_item_report));
		items.add(new SatelliteMenuItem(R.id.sat_item_download,
				R.drawable.ic_sat_menu_item_download));
		items.add(new SatelliteMenuItem(R.id.sat_item_star,
				R.drawable.ic_sat_menu_item_star));
		items.add(new SatelliteMenuItem(R.id.sat_item_share,
				R.drawable.ic_sat_menu_item_share));
		items.add(new SatelliteMenuItem(R.id.sat_item_dislike,
				R.drawable.ic_sat_menu_item_dislike));
		items.add(new SatelliteMenuItem(R.id.sat_item_liked,
				R.drawable.ic_sat_menu_item_like));

		menu.addItems(items);

		menu.setOnItemClickedListener(new SateliteClickedListener() {

			@Override
			public void eventOccured(int id) {
				switch (id) {
				case R.id.sat_item_liked:
					likeClicked();
					break;
				case R.id.sat_item_dislike:
					dislikeClicked();
					break;
				case R.id.sat_item_share:
					shareClicked();
					break;
				case R.id.sat_item_star:
					starClicked();
					break;
				case R.id.sat_item_download:
					downloadClicked();
					break;
				case R.id.sat_item_report:
					reportClicked();
					break;
				}
			}
		});
	}

	protected void reportClicked() {
		// TODO Auto-generated method stub

	}

	protected void downloadClicked() {
		// TODO Auto-generated method stub

	}

	protected void starClicked() {
		// TODO Auto-generated method stub

	}

	protected void shareClicked() {
		// TODO Auto-generated method stub

	}

	protected void dislikeClicked() {
		// TODO: Change button image to indicate status
		toggleLikedStatus(pager.getCurrentItem(), LIKE_STATUS.DISLIKED);
	}

	protected void likeClicked() {
		// TODO: Change button image to indicate status
		toggleLikedStatus(pager.getCurrentItem(), LIKE_STATUS.LIKED);
	}

	/**
	 * Toggle the like status of the image at the given position. If the image already
	 * has the that status then return the status to neutral, otherwise give
	 * the image the new status.
	 * 
	 * @param imagePos
	 *            The position of the image to toggle
	 * @param disliked
	 *            The state to toggle
	 */
	@Background
	protected void toggleLikedStatus(int imagePos, final LIKE_STATUS status) {
		ChannelImage image = mChannelPlayer.getImage(imagePos, false);
		
		if(image.getLikeStatus() == status){
			image.setLikeStatus(LIKE_STATUS.NEUTRAL);
		} else {
			image.setLikeStatus(status);
		}
		image.save();
		
		// TODO: Update the icons in the menu
	}

	private void resumeState(CachedPlayerState state) {
		mChannelPlayer = state.player;
		startChannel(state.position);
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
		// don't cache a failed channel
		if (shouldCache) {
			cachedState = null;
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
			final OnGetImageResultListener listener) {
		mChannelPlayer.getImageAsync(position, replacement,
				new OnGetChannelImageResultListener() {

					@Override
					public void onGetChannelImageResult(ChannelImage image) {
						listener.onGetImageResult(image.getImage());

					}
				});
	}

	public interface OnGetImageResultListener {
		public void onGetImageResult(Image image);
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
				// close menu when image changes
				menu.close();
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
			mChannelPlayer = null;
			mOnConfigChangeState = null;
		} else {
			mOnConfigChangeState = new CachedPlayerState(mChannelPlayer,
					pager.getCurrentItem());
		}

		// close the menu manually, otherwise it'll think it's still open when
		// it is recreated but actually it will be closed (bug). We could try to
		// save the open state and restore it as open
		if (menu != null) {
			menu.close();
		}
		
		dismissBusyDialog();
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
			mChannelPlayer.getImageAsync(i, false,
					new OnGetChannelImageResultListener() {

						@Override
						public void onGetChannelImageResult(ChannelImage image) {
							loader.preloadImage(image.getImage());
						}
					});
		}
	}

}
