package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import android.widget.BaseAdapter;

public abstract class ImageGridAdapter<T> extends BaseAdapter {
	protected List<T> mAvailableItems = new ArrayList<T>();
	protected List<T> mSelectedItems = new ArrayList<T>();
	
	public void setAvailabledtItems(List<T> items){
		if(items == null){
			throw new IllegalArgumentException("Items can't be null");
		}
		
		mAvailableItems = items;
		notifyDataSetChanged();
	}

	public void setSelectedItems(List<T> items){
		if(items == null){
			throw new IllegalArgumentException("Items can't be null");
		}
		
		mSelectedItems = items;
		notifyDataSetChanged();
	}
	
	public List<T> getSelectedItems(){
		return mSelectedItems;
	}
	
	public List<T> getItems(){
		return mAvailableItems;
	}
	
}
