package com.picdora;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.EViewGroup;
import com.googlecode.androidannotations.annotations.ViewById;
import com.picdora.models.Channel;

@EViewGroup(R.layout.channel_list_item)
public class ChannelItemView extends RelativeLayout{
	@ViewById
	TextView channelName;
	
	public ChannelItemView(Context context) {
		super(context);
	}
	
	public void bind(Channel channel) {
        channelName.setText(channel.getName());
    }	

}
