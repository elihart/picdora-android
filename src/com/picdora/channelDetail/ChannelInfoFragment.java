package com.picdora.channelDetail;

import java.text.SimpleDateFormat;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.picdora.CategoryUtils;
import com.picdora.ChannelUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelCreation.CategoryListAdapter;
import com.picdora.channelCreation.CategoryListAdapter_;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.grid.ImageGridSelector;

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
		int count = ChannelUtils.getNumImagesViewed(mChannel);
		setImageViewCount(count);
	}

	@UiThread
	protected void setImageViewCount(int count) {
		if (isAdded()) {
			imagesViewed.setText(getResources().getString(
					R.string.channel_detail_images_seen, count));
		}
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
		long imageCount = ChannelUtils.getImageCount(mChannel, false);
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
		LinearLayout container = (LinearLayout) LayoutInflater.from(mActivity)
				.inflate(R.layout.edit_text_for_dialog, null);
		final EditText channelName = (EditText) container
				.findViewById(R.id.edit_text);
		channelName.setText(mChannel.getName());
		FontHelper.setTypeFace(channelName, STYLE.REGULAR);

		final PicdoraDialog dialog = new PicdoraDialog.Builder(mActivity)
				.setTitle(R.string.channel_detail_name_dialog_title)
				.setView(container)
				.setPositiveButton(
						R.string.channel_detail_name_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								validateChannelName(channelName.getText()
										.toString());
							}
						})
				.setNegativeButton(R.string.dialog_default_negative, null)

				.create();

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
		} else if (ChannelUtils.isNameTaken(name)) {
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
		String msg = getResources().getString(R.string.channel_detail_change_name_taken_error, name);
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mActivity, msg, duration);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();

	}

	@Background
	@Click
	protected void changeCategoriesButtonClicked() {
		List<Category> availableCategories = CategoryUtils.getAll(mChannel
				.isNsfw());
		CategoryUtils.sortByName(availableCategories);
		final List<Category> selectedCategories = mChannel.getCategories();
		CategoryListAdapter adapter = CategoryListAdapter_
				.getInstance_(mActivity);
		ImageGridSelector<Category> selector = new ImageGridSelector<Category>(
				mActivity, availableCategories, selectedCategories, adapter);

		showSetCategoriesDialog(selector);
	}

	@UiThread
	protected void showSetCategoriesDialog(
			final ImageGridSelector<Category> selector) {
		new PicdoraDialog.Builder(mActivity).showTitle(false)
				.setFullScreen(true).setView(selector.getView())
				.setPositiveButton(R.string.channel_detail_change_categories_dialog_positive, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						List<Category> selected = selector.getSelectedItems();
						if (selected.isEmpty()) {
							showEmptyCategoriesDialog();
						} else {
							mChannel.setCategories(selected);
							mChannel.saveAsync();
						}
					}
				}).setNegativeButton(R.string.dialog_default_negative, null).show();
	}

	/**
	 * Show a dialog saying the user can't select no categories
	 */
	@UiThread
	protected void showEmptyCategoriesDialog() {
		new PicdoraDialog.Builder(mActivity)
				.setTitle(R.string.channel_detail_change_categories_empty_dialog_title)
				.setMessage(
						R.string.channel_detail_change_categories_empty_dialog_message)
				.setPositiveButton(R.string.dialog_default_positive, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// reopen the categories dialog so they can choose again
						changeCategoriesButtonClicked();						
					}
				})
				.show();
	}
}
