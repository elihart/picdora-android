package com.picdora.channelCreation;

import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.models.Channel.GifSetting;

public class ChannelCreationInfo {
	public GifSetting gifSetting;		
	public NsfwSetting nsfwSetting;
	public String channelName;
	
	public ChannelCreationInfo(GifSetting gifSetting,
			NsfwSetting nsfwSetting, String channelName) {
		this.gifSetting = gifSetting;
		this.nsfwSetting = nsfwSetting;
		this.channelName = channelName;
	}
}
