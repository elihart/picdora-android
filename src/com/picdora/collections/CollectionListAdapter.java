package com.picdora.collections;

import java.util.List;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.picdora.PicdoraPreferences_;

/**
 * Show the names of a list of collections.
 * 
 */
@EBean
public class CollectionListAdapter extends BaseAdapter {
	@Pref
	protected PicdoraPreferences_ mPrefs;

    private List<Collection> collections;

    @RootContext
    Context context;

    @AfterInject
    void initAdapter() {
        collections = CollectionUtil.getAllCollections(mPrefs.showNsfw().get());
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
}
