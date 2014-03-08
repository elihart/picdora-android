package com.picdora.channelCreation;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;
import android.widget.TextView;

import com.picdora.ChannelHelper;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.player.ChannelPlayer;

/**
 * This activity guides the user through creating a new channel. It consists of
 * a viewpager with two fragments. The first collections info about the channel
 * - name, gif setting, and nsfw setting. The second allows the user to choose
 * which categories to include
 * 
 * 
 */

@EActivity(R.layout.activity_channel_creation)
public class ChannelCreationActivity extends PicdoraActivity{
	@ViewById
	ChannelCreationViewPager pager;
	@Pref
	PicdoraPreferences_ prefs;

	private PagerAdapter pagerAdapter;

	private int mCurrentPage;

	// store the selected categories so we can restore them after coming back
	// from Preview if the activity was destroyed
	private static List<Category> selectedCategoriesState;
	// keep track of the info reported by the info fragment and make it static
	// so we can save state
	private static ChannelCreationInfo channelInfoState;

	// Loading dialog to show while channel is created
	private Dialog busyDialog;

	private OnFilterCategoriesListener categoryFilterListener;

	// Whether the user has opted to show nsfw images in the settings, and a
	// listener to listen for this setting to change. If nsfw is turned off in
	// settings then we won't show the nsfw radio group
	private boolean allowNsfwPreference;

	// whether nsfw categories should be displayed. Don't include them, include
	// them and sfw, show only nsfw. If Allow nsfwPreference is false then this
	// will be NONE
	public enum NsfwSetting {
		NONE, ALLOWED, ONLY
	}

	// keep track of when we are loading the created channel so we don't allow
	// duplicates
	private boolean loadingChannel = false;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		if (state == null) {
			clearSavedState();
		}
	}

	private void clearSavedState() {
		selectedCategoriesState = null;
		channelInfoState = null;
	}

	private void saveState(ChannelCreationInfo info) {
		channelInfoState = info;
	}

	private void saveState(List<Category> categories) {
		selectedCategoriesState = categories;
	}

	@AfterViews
	void initViews() {
		allowNsfwPreference = prefs.showNsfw().get();

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
				setLoadingStatus(false);
				mCurrentPage = position;
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

		mCurrentPage = 0;
	}

	private class ChannelCreationPagerAdapter extends FragmentPagerAdapter {
		Fragment[] frags = { new ChannelInfoFragment_(),
				new CategorySelectFragment_() };

		public ChannelCreationPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return frags[position];
		}

		@Override
		public int getCount() {
			return frags.length;
		}
	}

	@Override
	public void onBackPressed() {
		// cancel loading
		if (loadingChannel) {
			setLoadingStatus(false);
		} else if (pager.getCurrentItem() == 0) {
			// clear state since we're leaving the activity
			clearSavedState();
			// leave the activity if we're on the first page
			super.onBackPressed();
		} else {
			// Otherwise, select the previous step.
			pager.setCurrentItem(pager.getCurrentItem() - 1);
		}
	}

	public void submitChannelInfo(ChannelCreationInfo info) {
		saveState(info);

		// if the settings preference is no NSFW then let's override this, just
		// in case...
		if (!allowNsfwPreference) {
			channelInfoState.nsfwSetting = NsfwSetting.NONE;
		}

		categoryFilterListener.onFilterCategories(info.nsfwSetting);
		pager.setCurrentItem(1, true);
	}

	public boolean getNsfwPreference() {
		return allowNsfwPreference;
	}

	// set up interface for activity to tell us how to filter categories
	public interface OnFilterCategoriesListener {
		public void onFilterCategories(NsfwSetting setting);
	}

	public void setOnFilterCategoriesListener(
			OnFilterCategoriesListener listener) {
		categoryFilterListener = listener;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// cancel loading
			setLoadingStatus(false);

			// if we're on the second page, return to the first page on up
			// pressed. Otherwise let it do the default (return to parent)
			if (mCurrentPage == 1) {
				pager.setCurrentItem(0, true);
				return true;
			} else {
				// clear state since we're leaving the activity
				clearSavedState();
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Background
	public void submitChannelCategories(List<Category> categories, boolean preview) {
		// if we're already loading, don't load again
		if (loadingChannel) {
			return;
		} else {
			setLoadingStatus(true);
		}

		if (categories == null || categories.isEmpty()) {
			setLoadingStatus(false);
			return;
		} else {
			saveState(categories);
		}


		Channel channel = new Channel(channelInfoState.channelName, categories,
				channelInfoState.gifSetting);

		long count = ChannelHelper.getImageCount(channel, false);
		if (count == 0) {
			showNoImagesDialog();
		} else if (count < 100) {
			showLowImageCountDialog(count, channel, preview);
		} else {
			// TODO: Maybe a confirmation dialog with settings reviewed
			launchChannel(channel, preview);
		}
	}

	public static List<Category> getSelectedCategoriesState() {
		return selectedCategoriesState;
	}

	@UiThread
	protected void showNoImagesDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"The categories and settings you chose don't match any images! Try changing the gif setting or choosing more categories.")
				.setTitle("Warning!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						setLoadingStatus(false);
					}
				});

		builder.create().show();
	}

	@UiThread
	protected void showLowImageCountDialog(long count, final Channel channel,
			final boolean preview) {
		String positive = "";
		if (preview) {
			positive = "Preview anyway";
		} else {
			positive = "Create anyway";
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"The categories and settings you chose only match " + count
						+ " images!")
				.setTitle("Warning!")
				.setPositiveButton(positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								launchChannel(channel, preview);
							}
						})
				.setNegativeButton("Change settings",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setLoadingStatus(false);
							}
						});

		builder.create().show();
	}

	@UiThread
	protected void launchChannel(Channel channel, boolean preview) {
		// if the loading was canceled then don't keep going
		if (!loadingChannel) {
			return;
		}

		ChannelHelper.playChannel(channel, !preview, this);

		if (!preview) {
			clearSavedState();
			channel.save();
			finish();
		}

		setLoadingStatus(false);

	}

	@UiThread
	protected void setLoadingStatus(boolean loading) {
		loadingChannel = loading;
		// pager.setPagingEnabled(!loading);

		if (loading) {
			// show loading screen
			showBusyDialog("Creating Channel...");
		} else {
			dismissBusyDialog();
		}
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

	public static NsfwSetting getNsfwFilter() {
		if (channelInfoState == null) {
			return NsfwSetting.NONE;
		} else {
			return channelInfoState.nsfwSetting;
		}
	}
}
