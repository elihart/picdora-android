package com.picdora.channelCreation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.picdora.models.Category;
import com.picdora.ui.grid.GridItemView;
import com.picdora.ui.grid.GridItemView_;
import com.picdora.ui.grid.ImageGridAdapter;

@EBean
public class CategoryListAdapter extends ImageGridAdapter<Category> {

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	protected String getImageUrl(Category item) {
		return item.getPreviewUrl();
	}

	@Override
	protected String getText(Category item) {
		return item.getName();
	}
}
