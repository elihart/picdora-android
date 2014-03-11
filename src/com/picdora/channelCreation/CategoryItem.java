package com.picdora.channelCreation;

import com.picdora.models.Category;

public class CategoryItem {
	public Category category;
	public boolean selected;
	
	public CategoryItem(Category category, boolean selected){
		this.category = category;
		this.selected = selected;
	}

}
