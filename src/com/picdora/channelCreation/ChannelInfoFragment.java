package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.picdora.ChannelHelper;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;

/**
 * This fragment allows the user to enter a channel name, a gif setting, and a
 * nsfw setting. On creation the fragment checks with the activity to see if the
 * user has NSFW enabled, and if not it hides the nsfw setting and sets it to
 * none.
 * 
 */
@EFragment(R.layout.fragment_channel_info)
public class ChannelInfoFragment extends Fragment implements
		OnCheckedChangeListener {
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
	RadioButton nsfw_allowed;
	@ViewById
	RadioButton nsfw_none;
	@ViewById
	RadioButton nsfw_only;
	@ViewById
	Button nextButton;

	private ChannelCreationActivity activity;
	private ChannelCreationInfo info;

	@AfterViews
	void initViews() {
		FontHelper.setTypeFace(channelName, STYLE.REGULAR);
		FontHelper.setTypeFace(nameLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(gifLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(nsfwLabel, STYLE.MEDIUM);
		FontHelper.setTypeFace(gif_none, STYLE.REGULAR);
		FontHelper.setTypeFace(gif_allowed, STYLE.REGULAR);
		FontHelper.setTypeFace(gif_only, STYLE.REGULAR);
		FontHelper.setTypeFace(nsfw_allowed, STYLE.REGULAR);
		FontHelper.setTypeFace(nsfw_none, STYLE.REGULAR);
		FontHelper.setTypeFace(nsfw_only, STYLE.REGULAR);
		FontHelper.setTypeFace(nextButton, STYLE.MEDIUM);

		channelName.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (channelName != null) {
					if (!hasFocus) {
						validateChannelName(channelName.getText().toString());
						hideKeyboard();
					}
				}
			}
		});

		channelName.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				channelName.setError(null);

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});

		nsfwSetting.setOnCheckedChangeListener(this);
		gifSetting.setOnCheckedChangeListener(this);

		setChannelInfo();
	}

	private void setChannelInfo() {
		info = new ChannelCreationInfo(getGifSetting(), getNsfwSetting(),
				channelName.getText().toString());
	}

	@Override
	public void onActivityCreated(Bundle state) {
		super.onActivityCreated(state);

		activity = (ChannelCreationActivity) getActivity();
		setNsfwGroupVisibility(activity.getNsfwPreference());

		channelName.requestFocus();
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (!Util.isStringBlank(channelName.getText().toString())) {
			hideKeyboard();
		}

		setChannelInfo();
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

	private boolean validateChannelName(String name) {
		if (Util.isStringBlank(name)) {
			channelName.setError("You have to give your channel a name!");
			return false;
		} else if (ChannelHelper.isNameTaken(name)) {
			channelName.setError("You've already used that name!");
			return false;
		} else {
			return true;
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

	private NsfwSetting getNsfwSetting() {
		switch (nsfwSetting.getCheckedRadioButtonId()) {
		case R.id.nsfw_none:
			return NsfwSetting.NONE;
		case R.id.nsfw_allowed:
			return NsfwSetting.ALLOWED;
		case R.id.nsfw_only:
			return NsfwSetting.ONLY;
		default:
			return NsfwSetting.NONE;
		}
	}

	@Click
	void nextButtonClicked() {
		// TODO: Validate in background, put a progress bar, disable further
		// input, then come back here
		setChannelInfo();
		if (!validateChannelName(info.channelName)) {
			return;
		} else {

			activity.submitChannelInfo(info);
			hideKeyboard();
		}
	}

	/**
	 * Disable controls during validation
	 * 
	 * @param enable
	 * @param vg
	 */
	private void disableEnableControls(boolean enable, ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View child = vg.getChildAt(i);
			child.setEnabled(enable);
			if (child instanceof ViewGroup) {
				disableEnableControls(enable, (ViewGroup) child);
			}
		}
	}

	private void setNsfwGroupVisibility(boolean visible) {
		if (nsfwSetting == null) {
			return;
		}

		if (visible) {
			nsfwLabel.setVisibility(View.VISIBLE);
			nsfwSetting.setVisibility(View.VISIBLE);
		} else {
			nsfwSetting.setVisibility(View.GONE);
			nsfwLabel.setVisibility(View.GONE);
			// set no nsfw categories to be shown
			nsfwSetting.check(nsfw_none.getId());
		}
	}
}