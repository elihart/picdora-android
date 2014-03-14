package com.picdora.ui.grid;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;

public class ImageGridSelector<T> {
	protected List<T> mAvailableItems, mSelectedItems;
	protected Context mContext;
	protected GridView mGrid;
	protected ImageGridAdapter<T> mAdapter;

	public ImageGridSelector(Context context, List<T> availableItems,
			List<T> selectedItems, ImageGridAdapter<T> adapter) {
		mContext = context;
		mAvailableItems = availableItems;
		mSelectedItems = selectedItems;

		mGrid = (GridView) LayoutInflater.from(context).inflate(
				R.layout.image_grid, null);
		mAdapter = adapter;
		mAdapter.setSelectedItems(mSelectedItems);
		mAdapter.setAvailabledtItems(mAvailableItems);

		// tell the image loader to pause on fling scrolling
		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;
		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		mGrid.setOnScrollListener(listener);

		// setup click listener that selects/deselects image and reports click
		// to listener
		mGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {

			}
		});
	}
}
