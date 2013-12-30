package com.picdora;

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

@EBean
public class CategoryListAdapter extends BaseAdapter {
	List<CategoryItem> categoryItems;

	@RootContext
	Context context;

	@AfterInject
	void initAdapter() {
		categoryItems = new ArrayList<CategoryItem>();
		List<Category> categories = Util.all(Category.class);
		CategoryHelper.sortByName(categories);
		for(Category cat : categories){
			categoryItems.add(new CategoryItem(cat, false));
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		CategoryItemView categoryView;
		if (convertView == null) {
			categoryView = CategoryItemView_.build(context);
		} else {
			categoryView = (CategoryItemView) convertView;
		}

		categoryView.bind(getItem(position));

		return categoryView;
	}

	@Override
	public int getCount() {
		return categoryItems.size();
	}

	@Override
	public CategoryItem getItem(int position) {
		return categoryItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).category.getId();
	}

	public List<Category> getSelectedCategories() {
		List<Category> selectedCategories = new ArrayList<Category>();
		for(CategoryItem item : categoryItems){
			if(item.selected){
				selectedCategories.add(item.category);
			}
		}
		
		return selectedCategories;
	}

}
