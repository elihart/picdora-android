package com.picdora.channelCreation;

import org.androidannotations.annotations.EBean;

import com.picdora.ImageUtils.IMGUR_SIZE;
import com.picdora.models.Category;
import com.picdora.ui.grid.ImageGridAdapter;

@EBean
public class CategoryListAdapter extends ImageGridAdapter<Category> {

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	protected String getImageUrl(Category item) {
		return item.getPreviewUrl(IMGUR_SIZE.MEDIUM_THUMBNAIL);
	}

	@Override
	protected String getText(Category item) {
		return item.getName();
	}
}
