package com.picdora.channelDetail;

import java.text.SimpleDateFormat;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.picdora.ChannelHelper;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.FontHelper;

@EFragment(R.layout.fragment_channel_detail_info)
public class ChannelInfoFragment extends Fragment implements
		OnCheckedChangeListener {
	// if the user chooses channel settings that match less than this many
	// images then show a warning
	private static final long LOW_IMAGE_THRESHOLD = 100;
	@ViewById
	protected TextView createdAt;
	@ViewById
	protected TextView lastUsed;
	@ViewById
	protected TextView imagesViewed;
	@ViewById
	protected RadioGroup gifSetting;
	@ViewById
	protected RadioButton gif_none;
	@ViewById
	protected RadioButton gif_allowed;
	@ViewById
	protected RadioButton gif_only;
	@ViewById
	protected RelativeLayout rootView;

	protected ChannelDetailActivity mActivity;

	protected Channel mChannel;

	@AfterViews
	protected void initViews() {
		FontHelper.setTypeFace(rootView);
		gifSetting.setOnCheckedChangeListener(this);

		mActivity = (ChannelDetailActivity) getActivity();
	}

	/**
	 * Set the channel whose info we should display
	 * 
	 * @param channel
	 */
	public void setChannel(Channel channel) {
		mChannel = channel;
		updateInfo();
	}

	/**
	 * Update the displayed info to match the set channel
	 */
	private void updateInfo() {
		// set gif setting
		switch (mChannel.getGifSetting()) {
		case ALLOWED:
			gif_allowed.setChecked(true);
			break;
		case NONE:
			gif_none.setChecked(true);
			break;
		case ONLY:
			gif_only.setChecked(true);
			break;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("E, MMM d, yyyy");

		String created = dateFormat.format(mChannel.getCreatedAt());
		createdAt.setText(getResources().getString(
				R.string.channel_detail_created_at, created));

		String used = dateFormat.format(mChannel.getLastUsed());
		lastUsed.setText(getResources().getString(
				R.string.channel_detail_last_viewed, used));

		// Set the image count to 0 until we get the count from the db in the
		// background
		imagesViewed.setText(getResources().getString(
				R.string.channel_detail_images_seen, 0));

		getAndSetImageViewCount();
	}

	/**
	 * Retrieve the image view count from the database and update the images
	 * seen stat.
	 */
	@Background
	protected void getAndSetImageViewCount() {
		int count = ChannelHelper.getNumImagesViewed(mChannel);
		setImageViewCount(count);
	}

	@UiThread
	protected void setImageViewCount(int count) {
		imagesViewed.setText(getResources().getString(
				R.string.channel_detail_images_seen, count));
	}

	/**
	 * Get the gif setting the user has chosen in the gifSetting radio group
	 * 
	 * @return
	 */
	private GifSetting getGifSetting() {
		switch (gifSetting.getCheckedRadioButtonId()) {
		case R.id.gif_none:
			return GifSetting.NONE;
		case R.id.gif_allowed:
			return GifSetting.ALLOWED;
		case R.id.gif_only:
			return GifSetting.ONLY;
		default:
			return GifSetting.ALLOWED;
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (group.equals(gifSetting)) {
			mChannel.setGifSetting(getGifSetting());
			saveChannel();
		}

	}

	@Background(serial = "update")
	protected void saveChannel() {
		long imageCount = ChannelHelper.getImageCount(mChannel, false);
		mChannel.save();

		if (imageCount < LOW_IMAGE_THRESHOLD) {
			showLowImageCountWarning(imageCount);
		}
	}

	/**
	 * Show a warning to the user that the image settings they chose don't match
	 * very many images
	 * 
	 * @param imageCount
	 */
	@UiThread
	protected void showLowImageCountWarning(long imageCount) {
		int strId;
		if (imageCount == 0) {
			strId = R.string.channel_detail_no_image_count_warning;
		} else {
			strId = R.string.channel_detail_low_image_count_warning;
		}

		String msg = getResources().getString(strId, imageCount);
		Util.makeBasicToast(getActivity(), msg);
	}

	/**
	 * When the user clicks the change name icon show them a dialog where they
	 * can change the name of the channel
	 */
	@Click
	protected void changeNameButtonClicked() {
		// TODO: Style dialog better
		
		final EditText channelName = new EditText(mActivity);
		channelName.setText(mChannel.getName());

		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(R.string.channel_detail_name_dialog_title)
				.setView(channelName)
				.setPositiveButton(
						R.string.channel_detail_name_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								validateChannelName(channelName.getText()
										.toString());
							}
						})
				.setNegativeButton(R.string.dialog_default_negative,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// canceled, don't do anything
							}
						});

		final AlertDialog dialog = builder.create();

		// show the keyboard when the dialog pops up
		channelName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					dialog.getWindow()
							.setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});

		// move the cursor to the end of the text
		channelName.setSelection(channelName.getText().length());
		
		dialog.show();
	}

	@Background
	protected void validateChannelName(String name) {
		// if the name is the same don't do anything
		if (Util.isStringBlank(name) || name.equals(mChannel.getName())) {
			return;
		} else if (ChannelHelper.isNameTaken(name)) {
			showNameTakenError(name);
		} else {
			updateChannelName(name);
		}
	}

	/**
	 * Update our copy of the channel and tell the parent activity to update
	 * it's copy
	 * 
	 * @param name
	 */
	private void updateChannelName(String name) {
		mChannel.setName(name);
		mActivity.updateChannelName(name);
	}

	@UiThread
	protected void showNameTakenError(String name) {
		String msg = "There's already a channel called " + name + "!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mActivity, msg, duration);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();

	}

	@Click
	protected void changeCategoriesButtonClicked() {

	}
}
