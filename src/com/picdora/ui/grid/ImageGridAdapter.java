package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

@EBean
public abstract class ImageGridAdapter<T> extends BaseAdapter {
	@RootContext
	protected Context context;
	
	protected List<T> mAvailableItems = new ArrayList<T>();
	protected List<T> mSelectedItems = new ArrayList<T>();

	public void setAvailabledtItems(List<T> items) {
		if (items == null) {
			throw new IllegalArgumentException("Items can't be null");
		}

		mAvailableItems = items;
		notifyDataSetChanged();
	}

	public void setSelectedItems(List<T> items) {
		if (items == null) {
			throw new IllegalArgumentException("Items can't be null");
		}

		mSelectedItems = items;
		notifyDataSetChanged();
	}

	public List<T> getSelectedItems() {
		return mSelectedItems;
	}

	public List<T> getItems() {
		return mAvailableItems;
	}

	@Override
	public int getCount() {
		return getItems().size();
	}

	@Override
	public T getItem(int position) {
		return getItems().get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		GridItemView itemView;
		if (convertView == null) {
			itemView = buildItemView();
		} else {
			itemView = (GridItemView) convertView;
		}

		T item = getItem(position);

		boolean highlight = getSelectedItems().contains(item);

		itemView.bind(getText(item), getImageUrl(item), highlight);

		return itemView;
	}

	protected abstract String getImageUrl(T item);

	protected abstract String getText(T item);

	protected GridItemView buildItemView(){
		return GridItemView_.build(context);
	}

}
