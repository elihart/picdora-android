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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.ImageUtils;
import com.picdora.ImageUtils.OnDownloadCompleteListener;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.imageloader.PicdoraImageLoader;
import com.picdora.imageloader.PicdoraImageLoader.OnDownloadSpaceAvailableListener;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelImage.LIKE_STATUS;
import com.picdora.player.ChannelPlayer.ChannelError;
import com.picdora.player.ChannelPlayer.OnGetChannelImageResultListener;
import com.picdora.player.ChannelPlayer.OnLoadListener;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.SatelliteMenu.SatelliteMenu;
import com.picdora.ui.SatelliteMenu.SatelliteMenu.SateliteClickedListener;
import com.picdora.ui.SatelliteMenu.SatelliteMenuItem;

@Fullscreen
@EActivity(R.layout.activity_channel_view)
public class ChannelViewActivity extends FragmentActivity implements
		OnDownloadSpaceAvailableListener {
	private static final int NUM_IMAGES_TO_PRELOAD = 5;

	// TODO: Show icon notification icon in bottom right, such as like status,
	// download progress/completion, etc.

	// Cache the last player used and remember the user's spot so they can
	// resume quickly
	private static CachedPlayerState cachedState;
	private boolean shouldCache;
	private PicdoraImageLoader loader;

	@ViewById
	protected RelativeLayout root;
	@ViewById
	protected PicdoraViewPager pager;
	@ViewById
	protected SatelliteMenu menu;
	@Bean
	protected ChannelPlayer mChannelPlayer;
	@Bean
	protected ChannelViewHelper mHelper;

	protected Activity mContext;

	// Gesture detection constants
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetectorCompat mDetector;

	// the fragment currently being viewed
	private ImageSwipeFragment mCurrFragment;
	// screen height in pixels
	private int mScreenHeight;

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
		mContext = this;
		// show loading screen
		showBusyDialog("Loading Channel...");

		mHelper.initScreenHeight(root);

		setupMenu();

		mDetector = new GestureDetectorCompat(this, new MyGestureListener());

		// We don't use the Universal Image loader here, it's only used for
		// thumbnails, so lets clear out so memory and clear it's cache
		ImageLoader.getInstance().clearMemoryCache();

		loader = PicdoraImageLoader.instance();

		shouldCache = getIntent().getBooleanExtra("cache", false);

		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);

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
	 * Set the height in pixels of the window showing this activity
	 * 
	 * @param height
	 */
	public void setWindowHeight(int height) {
		mScreenHeight = height;
	}

	/**
	 * Get the height in pixels of the window showing this activity
	 * 
	 * @return
	 */
	public int getWindowHeight() {
		return mScreenHeight;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// Give the touch event to our gesture detector first before passing it
		// on
		this.mDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * Check if the currently visible image is zoomed
	 * 
	 * @return
	 */
	private boolean isZoomed() {
		if (mCurrFragment == null) {
			return false;
		} else {
			return mCurrFragment.isZoomed();
		}
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			// if the line isn't vertical enough then abort
			if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}

			// don't register a fling if we're zoomed in
			if (isZoomed()) {
				return false;
			}
			// down to up
			else if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {

				setLikeStatus(getCurrentFragment(), LIKE_STATUS.LIKED);
			}
			// up to down
			else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				setLikeStatus(getCurrentFragment(), LIKE_STATUS.DISLIKED);
			}

			return true;
		}
	}

	public void setCurrentFragment(ImageSwipeFragment frag) {
		mCurrFragment = frag;
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
		items.add(new SatelliteMenuItem(R.id.sat_item_search,
				R.drawable.ic_sat_menu_item_search));

		menu.addItems(items);

		menu.setOnItemClickedListener(new SateliteClickedListener() {

			@Override
			public void eventOccured(int id) {
				switch (id) {
				// case R.id.sat_item_liked:
				// likeClicked();
				// break;
				// case R.id.sat_item_dislike:
				// dislikeClicked();
				// break;
				case R.id.sat_item_search:
					searchClicked();
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

	protected void searchClicked() {
		mChannelPlayer.getImageAsync(pager.getCurrentItem(), false,
				new OnGetChannelImageResultListener() {

					@Override
					public void onGetChannelImageResult(ChannelImage image) {
						ImageUtils.lookupImage(mContext, image.getImgurId());
					}
				});

	}

	protected void reportClicked() {
		new PicdoraDialog.Builder(mContext)
				.setTitle(R.string.channel_view_report_dialog_title)
				.setMessage(R.string.channel_view_report_dialog_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.channel_view_report_dialog_positive_button,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								reportCurrentImage();
							}
						}).show();

	}

	/**
	 * Report the current image as being miscategorized
	 */
	protected void reportCurrentImage() {
		// TODO: Mark image as reported in the database
		// TODO: Add image to server sync table
		// TODO: Attempt sync
	}

	protected void downloadClicked() {
		// TODO: Notify download start
		// get the image currently being viewed
		mChannelPlayer.getImageAsync(pager.getCurrentItem(), false,
				new OnGetChannelImageResultListener() {

					@Override
					public void onGetChannelImageResult(ChannelImage image) {
						ImageUtils.saveImgurImage(getApplicationContext(),
								image.getImgurId(),
								new OnDownloadCompleteListener() {

									@Override
									public void onDownloadComplete(
											boolean success) {
										// TODO: Notify download complete
									}
								});
					}
				});

	}

	protected void starClicked() {
		// TODO: Create collection select view

		new PicdoraDialog.Builder(mContext)
				.setTitle(R.string.channel_view_star_dialog_title)
				.setMessage(R.string.channel_view_star_dialog_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.channel_view_star_dialog_positive_button,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO: Add current image to selected
								// collections

							}
						}).show();
	}

	protected void shareClicked() {
		// get the image currently being viewed
		mChannelPlayer.getImageAsync(pager.getCurrentItem(), false,
				new OnGetChannelImageResultListener() {

					@Override
					public void onGetChannelImageResult(ChannelImage image) {
						ImageUtils.shareImage(mContext, image.getImgurId());
					}
				});

	}

	/**
	 * Toggle the like status of the image at the given position. If the image
	 * already has the status then nothing is changed. However, the status won't
	 * move directly from liked to disliked, it will be set to neutral first.
	 * @param frag The fragment holding the image that was liked

	 * @param status
	 *            The state to set
	 */
	@Background
	protected void setLikeStatus(ImageSwipeFragment frag, final LIKE_STATUS status) {
		if(frag == null){
			return;
		}
		ChannelImage image = frag.getImage();
		
		if(image == null){
			return;
		}
		
		LIKE_STATUS curr = image.getLikeStatus();

		// Don't have to change anything if the status is the same
		if (image.getLikeStatus() == status) {

		}
		// if we're currently neutral then change to the new status
		else if (curr == LIKE_STATUS.NEUTRAL) {
			image.setLikeStatus(status);
			image.save();
		}
		// otherwise we're going from one extreme to the another, so set to
		// neutral
		else {
			image.setLikeStatus(LIKE_STATUS.NEUTRAL);
			image.save();
		}

		// show an indication on screen of the current status
		frag.setLikeStatus(image.getLikeStatus());
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
			final OnGetChannelImageResultListener listener) {
		mChannelPlayer.getImageAsync(position, replacement,
				listener);
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

	/**
	 * Get the image fragment currently visible
	 * @return
	 */
	public ImageSwipeFragment getCurrentFragment() {
		return mCurrFragment;		
	}

	/**
	 * Get the root viewgroup of the activity
	 * @return
	 */
	public ViewGroup getRootView() {
		return root;
	}

}
