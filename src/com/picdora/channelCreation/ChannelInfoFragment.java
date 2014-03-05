package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;

@EFragment(R.layout.fragment_channel_info)
public class ChannelInfoFragment extends Fragment {
	@ViewById
	RadioGroup gifSetting;
	@ViewById
	RadioGroup nsfwSetting;
	@ViewById
	EditText channelName;
	@ViewById
	TextView nameLabel;
	@ViewById
	TextView gifLabel;
	@ViewById
	TextView nsfwLabel;
	@ViewById
	RadioButton gif_none;
	@ViewById
	RadioButton gif_allowed;
	@ViewById
	RadioButton gif_only;
	@ViewById
	RadioButton nsfw_yes;
	@ViewById
	RadioButton nsfw_no;
	@ViewById
	Button nextButton;

	private ChannelCreationActivity activity;

	@AfterViews
	void initViews() {
		FontHelper.setTypeFace(channelName, STYLE.REGULAR);
		FontHelper.setTypeFace(nameLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(gifLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(nsfwLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(gif_none, STYLE.REGULAR);
		FontHelper.setTypeFace(gif_allowed, STYLE.REGULAR);
		FontHelper.setTypeFace(gif_only, STYLE.REGULAR);
		FontHelper.setTypeFace(nsfw_yes, STYLE.REGULAR);
		FontHelper.setTypeFace(nsfw_no, STYLE.REGULAR);
		FontHelper.setTypeFace(nextButton, STYLE.MEDIUM);

		activity = (ChannelCreationActivity) getActivity();

		gifSetting.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				hideKeyboard();
			}
		});

		gifSetting.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				hideKeyboard();
				activity.onNsfwSettingChanged(nsfw_yes.isChecked());
			}
		});
	}

	protected void hideKeyboard() {
		channelName.clearFocus();
		InputMethodManager imm = (InputMethodManager) activity
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(channelName.getWindowToken(), 0);
		}
		// Also use this alternative method for hiding keyboard because
		// sometimes the first doesn't work
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Click
	void channelNameClicked() {
		channelName.setError(null);
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

	@Click
	void nextButtonClicked() {
		// TODO: Validate channel name
		activity.next();
		hideKeyboard();
	}
}