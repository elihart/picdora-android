package com.picdora.channelCreation;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.models.Category;
import com.picdora.ui.PicdoraGridItem;
import com.picdora.ui.PicdoraGridItem_;

@EBean
public class CategoryListAdapter extends BaseAdapter {
	List<Category> categories;
	private List<Category> selectedCategories;

	@RootContext
	Context context;

	@AfterInject
	void initAdapter() {
		selectedCategories = new ArrayList<Category>();
		categories = new ArrayList<Category>();
	}
	
	public void setCategoryList(List<Category> categories){
		this.categories = categories;
		notifyDataSetChanged();
	}
	
	public void setSelectedCategories(List<Category> categories){
		selectedCategories = categories;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		PicdoraGridItem categoryView;
		if (convertView == null) {
			categoryView = PicdoraGridItem_.build(context);
		} else {
			categoryView = (PicdoraGridItem) convertView;
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
