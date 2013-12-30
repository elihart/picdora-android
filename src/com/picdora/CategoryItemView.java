package com.picdora;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
