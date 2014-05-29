package com.picdora.channelCreation;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.widget.TextView;

import com.picdora.ChannelUtil;
import com.picdora.R;
import com.picdora.models.Channel;
import com.picdora.models.ChannelPreview;
import com.picdora.ui.PicdoraDialog;

/**
 * Helper methods for the ChannelCreationActivity to handle the creation of the
 * channel.
 */
@EBean
public class ChannelCreationUtil {
	@RootContext
	ChannelCreationActivity mActivity;
	/**
	 * Whether a channel is currently being loaded. Don't let multiple button
	 * clicks start the process more than once.
	 */
	private boolean mIsLoadingChannel = false;
	/** Loading dialog to show while channel is created. */
	private Dialog busyDialog;

	/**
	 * Create a channel with the given categories and info.
	 * 
	 * @param info
	 */
	@Background
	public void createChannel(ChannelCreationInfo info,
			boolean ignoreLowImageCount) {

		/*
		 * If we're already loading, don't load again unless we're returning
		 * from the low image count dialog with a setting to ignore the low
		 * image warning.
		 */
		if (mIsLoadingChannel && !ignoreLowImageCount) {
			return;
		}
		/* Don't load if there aren't any categories. */
		else if (info.categories == null || info.categories.isEmpty()) {
			return;
		}
		/* Otherwise show the loading screen. */
		else {
			setLoadingStatus(true);
		}

		/*
		 * If we've already alerted the user to a low image count and they
		 * ignore it then go on to launch.
		 */
		if (!ignoreLowImageCount) {
			/*
			 * Check how many images these settings match and notify the user if
			 * the count is low so they can either modify the settings or ignore
			 * the warning.
			 */
			long count = ChannelUtil.getImageCount(info.gifSetting,
					info.categories);

			if (count == 0) {
				showNoImagesDialog();
				return;
			} else if (count < 100) {
				/*
				 * Show the warning. Allows them to ignore it and come back here
				 * again.
				 */
				showLowImageCountDialog(count, info);
				return;
			}
		}

		/* Create the channel. */
		Channel channel = null;
		if (info.preview) {
			channel = new ChannelPreview(info.categories, info.gifSetting);
		} else {
			/* Can't set the categories until the channel is saved. */
			channel = new Channel(info.channelName, info.gifSetting,
					info.categories);
			/*
			 * If it's not valid then break the loading. We've done validations
			 * on everything up until this point though so we should be good to
			 * go.
			 */
			if (!channel.isValid()) {
				setLoadingStatus(false);
				return;
			}
			/* Save to db so we get a channel id. */
			channel.save();
			/* Set and save the categories now that we have an id. */
			channel.saveCategoriesToDb();
		}

		launchChannel(channel, info.preview);
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void showNoImagesDialog() {
		PicdoraDialog dialog = new PicdoraDialog.Builder(mActivity)
				.setMessage(
						"The categories and settings you chose don't match any images! Try changing the gif setting or choosing more categories.")
				.setTitle("Warning!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						/* Stop the loading screen. */
						setLoadingStatus(false);
					}
				}).create();

		/* Cancel the loading screen if the dialog is canceled. */
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				setLoadingStatus(false);
			}
		});

		dialog.show();
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void showLowImageCountDialog(long count,
			final ChannelCreationInfo info) {
		String positive = "";
		if (info.preview) {
			positive = "Preview anyway";
		} else {
			positive = "Create anyway";
		}

		PicdoraDialog dialog = new PicdoraDialog.Builder(mActivity)
				.setMessage(
						"The categories and settings you chose only match "
								+ count + " images!")
				.setTitle("Warning!")
				.setPositiveButton(positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								/*
								 * Redo the channel creation, ignoring the low
								 * image count.
								 */
								createChannel(info, true);
							}
						})
				.setNegativeButton("Change settings",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								/*
								 * Stop the loading screen and let the user
								 * change the channel settings.
								 */
								setLoadingStatus(false);
							}
						}).create();

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				setLoadingStatus(false);
			}
		});

		dialog.show();
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void launchChannel(Channel channel, boolean preview) {
		// if the loading was canceled then don't keep going, otherwise clear
		// the loading screen
		if (!mIsLoadingChannel) {
			return;
		} else {
			setLoadingStatus(false);
		}

		/* Finish the activity if it's not a preview. */
		if (!preview) {
			mActivity.finish();
		}

		/*
		 * Play the channel. Don't need to update last played time to now since
		 * it was just created (or it's a preview.)
		 */
		ChannelUtil.playChannel(channel, mActivity, false);
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void setLoadingStatus(boolean loading) {
		/*
		 * If the status is already set to what we want then don't need to do it
		 * again.
		 */
		if (loading == mIsLoadingChannel) {
			return;
		}

		mIsLoadingChannel = loading;

		if (loading) {
			// show loading screen
			showBusyDialog("Creating Channel...");
		} else {
			dismissBusyDialog();
		}
	}

	@UiThread(propagation = Propagation.REUSE)
	public void showBusyDialog(String message) {
		busyDialog = new Dialog(mActivity, R.style.picdora_dialog_style);
		busyDialog.setContentView(R.layout.lightbox_dialog);
		((TextView) busyDialog.findViewById(R.id.dialogText)).setText(message);

		if (!mActivity.isFinishing()) {
			busyDialog.show();
			/*
			 * If the back button is pressed while the channel loads we need to
			 * turn the loading status off.
			 */
			busyDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					setLoadingStatus(false);
				}
			});
		}
	}

	@UiThread(propagation = Propagation.REUSE)
	public void dismissBusyDialog() {
		if (busyDialog != null) {
			try {
				busyDialog.dismiss();
			} catch (IllegalArgumentException e) {
				// catch the "View not attached to Window Manager" errors
			}

			busyDialog = null;
		}
	}

	/**
	 * Whether we are currently trying to create and load a new channel.
	 * 
	 * @return
	 */
	public boolean isLoading() {
		return mIsLoadingChannel;
	}

}
