package com.picdora.ui;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.ui.FontHelper.FontStyle;

/**
 * Use this adapter to add options to the listview in the sliding menu
 * 
 */
public class SlidingMenuAdapter extends ArrayAdapter<SlidingMenuItem> {

	public SlidingMenuAdapter(Context context, ArrayList<SlidingMenuItem> items) {
		super(context, R.layout.sliding_menu_item, items);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;

		if (v == null) {

			LayoutInflater vi;
			vi = LayoutInflater.from(getContext());
			v = vi.inflate(R.layout.sliding_menu_item, null);

		}

		SlidingMenuItem item = getItem(position);

		TextView label = (TextView) v.findViewById(R.id.label);
		ImageView icon = (ImageView) v.findViewById(R.id.icon);

		label.setText(item.getLabel());
		FontHelper.setTypeFace(label, FontStyle.REGULAR);
		icon.setImageResource(item.getIcon());

		return v;
	}
}
