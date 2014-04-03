package com.picdora.ui.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.grid.GridSize;

/**
 * Used for selecting Channel items from a Spinner.
 * 
 */
public class GridSizeArrayAdapter extends ArrayAdapter<GridSize> {
	private int mResource;
	private LayoutInflater mInflater;

	public GridSizeArrayAdapter(Context context, int resource,
			GridSize[] sizes) {
		super(context, resource, sizes);

		mResource = resource;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// use the same view as dropdown
		View v = getDropDownView(position, convertView, parent);

		// but set the font to be bolder
		FontHelper.setTypeFace(getHolder(v).text, FontStyle.MEDIUM);
		
		return v;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(mResource, null);
		}

		Holder holder = getHolder(convertView);

		GridSize size = getItem(position);

		holder.text.setText(size.getName());

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
