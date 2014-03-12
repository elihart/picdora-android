package com.picdora.channelDetail;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;

@EActivity(R.layout.activity_channel_detail)
public class ChannelDetailActivity extends PicdoraActivity {
	
	@AfterViews
	void initChannel() {
		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		Channel channel = Util.fromJson(json, Channel.class);
		
		setActionBarTitle(channel.getName().toUpperCase());
	}

}
