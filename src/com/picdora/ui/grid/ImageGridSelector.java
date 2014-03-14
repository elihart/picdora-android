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
	protected OnGridItemClickListener<T> mClickListener;

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
		
		mGrid.setAdapter(mAdapter);

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
				itemClicked((GridItemView) view, mAdapter.getItem(pos));
			}
		});
	}
	
	public void setItems(List<T> items){
		mAvailableItems = items;
		mAdapter.setAvailabledtItems(items);
	}
	
	public List<T> getSelectedItems(){
		return mSelectedItems;
	}
	
	public View getView(){
		return mGrid;
	}

	public void setOnClickListener(OnGridItemClickListener<T> listener) {
		mClickListener = listener;
	}

	protected void itemClicked(GridItemView view, T item) {

		// highlight/unhighlight item and add/remove it to the list
		if (mSelectedItems.contains(item)) {
			mSelectedItems.remove(item);
			view.setHighlighted(false);
		} else {
			mSelectedItems.add(item);
			view.setHighlighted(true);
		}

		if(mClickListener != null){
			mClickListener.OnGridItemClick(view, item);
		}
	}

	public interface OnGridItemClickListener<T> {
		public void OnGridItemClick(GridItemView view, T item);
	}
}
