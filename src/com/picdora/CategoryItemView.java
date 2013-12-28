package com.picdora;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.EViewGroup;
import com.googlecode.androidannotations.annotations.ViewById;
import com.picdora.models.Category;

@EViewGroup(R.layout.category_list_item)
public class CategoryItemView extends RelativeLayout{
	@ViewById
	TextView categoryName;
	@ViewById
	CheckBox checkBox;
	
	public CategoryItemView(Context context) {
		super(context);
	}
	
	public void bind(CategoryItem categoryItem) {
        categoryName.setText(categoryItem.category.getName());
        checkBox.setChecked(categoryItem.selected);
    }	
	
	public void setChecked(boolean checked){
		checkBox.setChecked(checked);
	}
	
	public void toggleChecked(){
		checkBox.toggle();
	}
	
	public boolean isChecked(){
		return checkBox.isChecked();
	}

}
