package com.picdora.likes;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.models.Channel;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;

/**
 * Used for selecting Channel items from a Spinner.
 * 
 */
public class ChannelSelectArrayAdapter extends ArrayAdapter<Channel> {
	private int mResource;
	private LayoutInflater mInflater;

	public ChannelSelectArrayAdapter(Context context, int resource,
			List<Channel> objects) {
		super(context, resource, objects);

		mResource = resource;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent){
		// use the same view as dropdown
		return getDropDownView(position, convertView, parent);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(mResource, null);

			holder = new Holder();
			holder.text = (TextView) convertView.findViewById(R.id.text);
			
			FontHelper.setTypeFace(holder.text, FontStyle.REGULAR);

			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		Channel channel = getItem(position);

		holder.text.setText(channel.getName());

		return convertView;
	}

	private static class Holder {
		public TextView text;

	}

}
