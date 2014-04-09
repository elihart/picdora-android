package com.picdora.channelCreation;

import org.androidannotations.annotations.EBean;

import com.picdora.models.Category;
import com.picdora.ui.grid.ModelGridAdapter;

@EBean
public class CategoryListAdapter extends ModelGridAdapter<Category> {

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	protected String getImgurId(Category item) {
		return item.getIconId();
	}

	@Override
	protected String getText(Category item) {
		return item.getName();
	}
}
