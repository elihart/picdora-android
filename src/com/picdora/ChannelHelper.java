package com.picdora;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;

@EBean
public class ChannelHelper {
	@RootContext
	Context context;
	
	public static List<Channel> getAll(){
		List<Category> categories = new ArrayList<Category>();
		categories.add(CategoryHelper.getCategoryById(1));

		List<Channel> channels = new ArrayList<Channel>();
		Channel channel = new Channel(1, "testChannel", categories, GifSetting.ALLOWED);
		channel.save();
		channels.add(channel);
		
		return channels;
	}
}
