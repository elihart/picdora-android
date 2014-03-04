package com.picdora.channelSelection;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.models.Channel;

@EBean
public class ChannelListAdapter extends BaseAdapter {
	List<Channel> channels;

	@RootContext
	Context context;

	@AfterInject
	void initAdapter() {
		channels = new ArrayList<Channel>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ChannelItemView channelView;
		if (convertView == null) {
			channelView = ChannelItemView_.build(context);
		} else {
			channelView = (ChannelItemView) convertView;
		}

		channelView.bind(getItem(position));

		return channelView;
	}

	@Override
	public int getCount() {
		return channels.size();
	}

	@Override
	public Channel getItem(int position) {
		return channels.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	public void setChannels(List<Channel> channels) {
		this.channels = channels;
		notifyDataSetChanged();
	}

}
