package com.picdora.collections;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.collections.CollectionUtil.OnCollectionSelectedListener;
import com.picdora.models.Collection;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;

/**
 * Show a list of all collections in the database. Handles async loading,
 * progress, and message on empty list.
 * 
 */
@EViewGroup(R.layout.collection_list)
public class CollectionListView extends RelativeLayout {
	/* Progress defaults to shown and everything is hidden at first. */
	@ViewById
	TextView text;
	@ViewById
	ProgressBar progress;
	@ViewById
	ListView list;

	@Bean
	CollectionListAdapter mAdapter;
	@Pref
	PicdoraPreferences_ mPrefs;
	/** Callback for when a collection is selected. */
	private OnCollectionSelectedListener mListener;

	public CollectionListView(Context context) {
		super(context);

	}

	@AfterViews
	void init() {
		FontHelper.setTypeFace(text, FontStyle.REGULAR);
		loadCollections();
	}

	/**
	 * Load the collections from the db in the background.
	 * 
	 */
	@Background
	void loadCollections() {
		List<Collection> collections = CollectionUtil.getAllCollections(mPrefs
				.showNsfw().get());
		setAdapterItems(collections);
	}

	/**
	 * Set the collection load result and update the display.
	 * 
	 * @param collections
	 */
	@UiThread
	void setAdapterItems(List<Collection> collections) {
		mAdapter.setCollections(collections);
		list.setAdapter(mAdapter);

		progress.setVisibility(View.GONE);

		if (mAdapter.isEmpty()) {
			text.setVisibility(View.VISIBLE);
		} else {
			list.setVisibility(View.VISIBLE);
		}
	}

	@ItemClick
	public void listItemClicked(Collection collection) {
		if (mListener != null) {
			mListener.onCollectionSelected(collection);
		}
	}

	/**
	 * Set a listener to be called when a collection is selected from the list.
	 * 
	 * @param listener
	 */
	public void setOnCollectionSelectedListener(
			OnCollectionSelectedListener listener) {
		mListener = listener;
	}

}
