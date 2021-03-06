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
	/* Ignore the resource passed in and use these instead */
	private int mDropdownResource = R.layout.action_spinner_view_dropdown;
	private int mViewResource = R.layout.action_spinner_view;

	private LayoutInflater mInflater;

	public ChannelSelectArrayAdapter(Context context, int resource,
			List<Channel> objects) {
		super(context, resource, objects);

		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(mViewResource, null);
		}

		Holder holder = getHolder(convertView);

		Channel channel = getItem(position);

		holder.text.setText(channel.getName());
		FontHelper.setTypeFace(holder.text, FontStyle.MEDIUM);

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(mDropdownResource, null);
		}

		Holder holder = getHolder(convertView);

		Channel channel = getItem(position);

		holder.text.setText(channel.getName());

		return convertView;
	}

	/**
	 * Get a viewholder from a convert view. Either reuse one if possible or
	 * create a new one and initialize it. If one is created then it will be
	 * saved to the view as a tag.
	 * 
	 * @param view
	 *            The view to get the holder from, can't be null.
	 * @return
	 */
	private Holder getHolder(View view) {
		Holder holder = (Holder) view.getTag();
		if (holder == null) {
			holder = new Holder();
			holder.text = (TextView) view.findViewById(R.id.text);
			FontHelper.setTypeFace(holder.text, FontStyle.REGULAR);
			view.setTag(holder);
		}

		return holder;
	}

	private static class Holder {
		public TextView text;

	}

}
