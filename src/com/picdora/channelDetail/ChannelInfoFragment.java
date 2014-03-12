package com.picdora.channelDetail;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;

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
	protected RadioGroup gifSetting;
	@ViewById
	protected RadioButton gif_none;
	@ViewById
	protected RadioButton gif_allowed;
	@ViewById
	protected RadioButton gif_only;
	@ViewById
	protected RelativeLayout rootView;
	protected Channel mChannel;

	@AfterViews
	protected void initViews() {
		FontHelper.setTypeFace(rootView);
		gifSetting.setOnCheckedChangeListener(this);
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
	 * Show a warning to the user that the image settings they chose don't match very many images
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
}
