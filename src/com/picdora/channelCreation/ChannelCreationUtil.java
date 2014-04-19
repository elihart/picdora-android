package com.picdora.channelCreation;

import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.TextView;

import com.picdora.ChannelUtil;
import com.picdora.R;
import com.picdora.models.Category;
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
	 * @param categories
	 * @param info
	 * @param preview
	 *            Whether the channel should just be a preview and not saved.
	 */
	@Background
	public void createChannel(List<Category> categories,
			ChannelCreationInfo info, boolean preview) {

		// if we're already loading, don't load again
		if (mIsLoadingChannel) {
			return;
		}
		/* Don't load if there aren't any categories. */
		else if (categories == null || categories.isEmpty()) {
			return;
		} else {
			setLoadingStatus(true);
		}

		Channel channel = null;
		if (preview) {
			channel = new ChannelPreview(categories, info.gifSetting);
		} else {
			channel = new Channel(info.channelName, categories, info.gifSetting);
		}

		/*
		 * Stop if the channel is invalid. TODO: Maybe show an error message?
		 * This really shouldn't happen though because we have validations and
		 * locks before this point.
		 */
		if (!preview && !channel.isValid()) {
			setLoadingStatus(false);
			return;
		}

		long count = ChannelUtil.getImageCount(channel, false);
		if (count == 0) {
			showNoImagesDialog();
		} else if (count < 100) {
			showLowImageCountDialog(count, channel, preview);
		} else {
			// TODO: Maybe a confirmation dialog with settings reviewed
			launchChannel(channel, preview);
		}
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void showNoImagesDialog() {
		new PicdoraDialog.Builder(mActivity)
				.setMessage(
						"The categories and settings you chose don't match any images! Try changing the gif setting or choosing more categories.")
				.setTitle("Warning!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						setLoadingStatus(false);
					}
				})

				.show();
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void showLowImageCountDialog(long count, final Channel channel,
			final boolean preview) {
		String positive = "";
		if (preview) {
			positive = "Preview anyway";
		} else {
			positive = "Create anyway";
		}
		new PicdoraDialog.Builder(mActivity)
				.setMessage(
						"The categories and settings you chose only match "
								+ count + " images!")
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
						})

				.show();
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
		 * If we're doing a preview then don't save the channel
		 */
		ChannelUtil.playChannel(channel, mActivity, !preview);
	}

	@UiThread(propagation = Propagation.REUSE)
	protected void setLoadingStatus(boolean loading) {
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

		busyDialog.show();
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
