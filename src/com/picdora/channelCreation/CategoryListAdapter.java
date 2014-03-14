package com.picdora.channelCreation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.picdora.models.Category;
import com.picdora.ui.grid.ImageGridAdapter;
import com.picdora.ui.grid.PicdoraGridItem;
import com.picdora.ui.grid.PicdoraGridItem_;

@EBean
public class CategoryListAdapter extends ImageGridAdapter<Category> {

	@RootContext
	Context context;


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		PicdoraGridItem categoryView;
		if (convertView == null) {
			categoryView = PicdoraGridItem_.build(context);
		} else {
			categoryView = (PicdoraGridItem) convertView;
		}

		Category c = getItem(position);
		
		boolean highlight = getSelectedItems().contains(c);
		
		categoryView.bind(c.getName(), c.getPreviewUrl(), highlight);

		return categoryView;
	}

	@Override
	public int getCount() {
		return getItems().size();
	}

	@Override
	public Category getItem(int position) {
		return getItems().get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}
}
