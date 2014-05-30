package com.picdora.collections;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.PicdoraPreferences_;
import com.picdora.models.Collection;

/**
 * Show the names of a list of collections.
 * 
 */
@EBean
public class CollectionListAdapter extends BaseAdapter {
	@Pref
	protected PicdoraPreferences_ mPrefs;

    private List<Collection> collections = new ArrayList<Collection>();

    @RootContext
    Context context;
    
    public void setCollections(List<Collection> collections){
    	this.collections = collections;
    	notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        CollectionListItemView view;
        if (convertView == null) {
            view = CollectionListItemView_.build(context);
        } else {
            view = (CollectionListItemView) convertView;
        }

        view.bind(getItem(position));

        return view;
    }

    @Override
    public int getCount() {
        return collections.size();
    }

    @Override
    public Collection getItem(int position) {
        return collections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }
    
    @Override
    public boolean isEmpty(){
    	return collections.isEmpty();
    }
}
