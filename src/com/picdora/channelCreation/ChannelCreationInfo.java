package com.picdora.channelCreation;

import java.util.List;

import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.models.Category;
import com.picdora.models.Channel.GifSetting;

public class ChannelCreationInfo {
	public GifSetting gifSetting;		
	public NsfwSetting nsfwSetting;
	public String channelName;
	public List<Category> categories;
	/** Whether the channel should be saved to the db or if the user is just previewing it. */
	public boolean preview;
	
	public ChannelCreationInfo(GifSetting gifSetting,
			NsfwSetting nsfwSetting, String channelName) {
		this.gifSetting = gifSetting;
		this.nsfwSetting = nsfwSetting;
		this.channelName = channelName;
	}
}
