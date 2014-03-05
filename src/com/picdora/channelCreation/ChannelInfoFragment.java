package com.picdora.channelCreation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.picdora.R;
import com.picdora.models.Channel.GifSetting;

@EFragment(R.layout.fragment_channel_info)
public class ChannelInfoFragment extends Fragment {
	@ViewById
	RadioGroup gifSetting;
	@ViewById
	ListView categoryList;
	@ViewById
	EditText channelName;

	@AfterViews
	void initViews() {

	}
	
	@Click
	void channelNameClicked(){
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
}