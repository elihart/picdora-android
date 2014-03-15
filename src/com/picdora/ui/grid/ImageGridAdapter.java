package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * This adapter displays items for the ImageGridSelector. It should be
 * subclassed to work with whatever model is needed. Each item needs an image
 * url and text to display over the image.
 * 
 * @param <T>
 *            The item type
 */
@EBean
public abstract class ImageGridAdapter<T> extends BaseAdapter {
	@RootContext
	protected Context context;

	protected List<T> mAvailableItems = new ArrayList<T>();
	protected List<T> mSelectedItems = new ArrayList<T>();

	/**
	 * Set the items to show. Redraws the list after setting them.
	 * 
	 * @param items
	 */
	public void setAvailabledtItems(List<T> items) {
		if (items == null) {
			throw new IllegalArgumentException("Items can't be null");
		}

		mAvailableItems = items;
		notifyDataSetChanged();
	}

	/**
	 * Sets the items that should be shown as selected.
	 * 
	 * @param items
	 */
	public void setSelectedItems(List<T> items) {
		if (items == null) {
			throw new IllegalArgumentException("Items can't be null");
		}

		mSelectedItems = items;
		notifyDataSetChanged();
	}

	/**
	 * Get selected items
	 * 
	 * @return
	 */
	public List<T> getSelectedItems() {
		return mSelectedItems;
	}

	/**
	 * Get all items shown
	 * 
	 * @return
	 */
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

	/**
	 * Get the url of the image to display
	 * 
	 * @param item
	 * @return
	 */
	protected abstract String getImageUrl(T item);

	/**
	 * Get the text to shown over the image
	 * 
	 * @param item
	 * @return
	 */
	protected abstract String getText(T item);

	/**
	 * Get the view to use to display the item. This can be overridden to
	 * provide a subclass of GridItemView for further customization
	 * 
	 * @return
	 */
	protected GridItemView buildItemView() {
		return GridItemView_.build(context);
	}

}
