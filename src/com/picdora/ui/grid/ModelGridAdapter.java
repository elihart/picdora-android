package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.ImageUtil;
import com.picdora.Util;
import com.picdora.ImageUtil.ImgurSize;

/**
 * This adapter displays models for the {@link #ModelGridSelector}. It should be
 * subclassed to work with whatever model is needed. Each item needs an image
 * url and optionally text to display over the image.
 * 
 * @param <T>
 *            The item type
 */
@EBean
public abstract class ModelGridAdapter<T> extends BaseAdapter {
	@RootContext
	protected Context context;

	protected List<T> mAvailableItems = new ArrayList<T>();
	protected List<T> mSelectedItems = new ArrayList<T>();

	/** The image size to use */
	protected ImageUtil.ImgurSize mImageSize = ImgurSize.MEDIUM_THUMBNAIL;

	/** Whether to show text overlayed on the image. Default to true. */
	protected boolean mShowText = true;

	/**
	 * Set the items to show. Redraws the list after setting them.
	 * 
	 * @param items
	 */
	public void setAvailableItems(List<T> items) {
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

		itemView.setShowText(mShowText);

		T item = getItem(position);

		boolean highlight = getSelectedItems().contains(item);

		/*
		 * Get the imgurId of the item and use it to generate a url for the item
		 * to show. If the imgur id isn't available then pass a null url and the
		 * view won't load an image.
		 */
		String imgurId = getImgurId(item);
		String url = null;
		if (!Util.isStringBlank(imgurId)) {
			url = ImageUtil.getImgurLink(getImgurId(item), mImageSize);
		}

		itemView.bind(getText(item), url, highlight);

		return itemView;
	}

	/**
	 * Get the imgur id of the image that the item displays.
	 * 
	 * @param item
	 * @return
	 */
	protected abstract String getImgurId(T item);

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

	/**
	 * Get the image size to be downloaded.
	 * 
	 * @return
	 */
	public ImageUtil.ImgurSize getImageSize() {
		return mImageSize;
	}

	/**
	 * Set the image size to use when downloading images for the grid.
	 * 
	 * @param imageSize
	 */
	public void setImageSize(ImageUtil.ImgurSize imageSize) {
		/* Only update if the size is different. */
		if (imageSize.equals(mImageSize)) {
			mImageSize = imageSize;
			notifyDataSetInvalidated();
		}
	}

	/**
	 * Whether the grid items should show text overlaid on the image.
	 * 
	 * @param show
	 *            True to show text, false otherwise.
	 */
	public void setShowText(boolean show) {
		mShowText = show;
	}

}
