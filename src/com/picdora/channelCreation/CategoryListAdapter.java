package com.picdora.channelCreation;

import java.util.List;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.Util;
import com.picdora.models.Category;

@EBean
public class CategoryListAdapter extends BaseAdapter {
	List<Category> categories;
	private List<Category> selectedCategories;

	@RootContext
	Context context;

	@AfterInject
	void initAdapter() {
		refreshCategories();
	}
	
	public void refreshCategories(){
		// TODO: Check nsfw preference
		categories = Util.all(Category.class);
		notifyDataSetChanged();
	}
	
	public void setSelectedCategories(List<Category> categories){
		selectedCategories = categories;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		CategoryItemView categoryView;
		if (convertView == null) {
			categoryView = CategoryItemView_.build(context);
		} else {
			categoryView = (CategoryItemView) convertView;
		}

		Category c = getItem(position);
		
		boolean highlight = false;
		if(selectedCategories != null){
			highlight = selectedCategories.contains(c); 
		}
		
		categoryView.bind(c.getName(), c.getPreviewUrl(), highlight);

		return categoryView;
	}

	@Override
	public int getCount() {
		return categories.size();
	}

	@Override
	public Category getItem(int position) {
		return categories.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}
}
