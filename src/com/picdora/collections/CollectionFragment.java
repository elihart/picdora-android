package com.picdora.collections;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;

import android.os.Bundle;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Image;
import com.picdora.ui.gallery.GalleryFragment;
import com.picdora.ui.grid.Selectable;

/**
 * Display all of the images of a single Collection in gallery format.
 * 
 */
@EFragment(R.layout.fragment_basic_grid)
public class CollectionFragment extends GalleryFragment {
	/**
	 * Pass the Collection we are to display as json and we can convert it
	 * later.
	 */
	@FragmentArg
	protected String mCollectionAsJson;
	@Bean
	protected CollectionUtil mUtils;

	/** The Collection to display */
	private Collection mCollection;

	@AfterViews
	protected void initCollection() {
		/*
		 * If our Collection hasn't been init'd from json yet then do it now.
		 */
		getCollection();

		/*
		 * If we haven't loaded any images yet then do a refresh, otherwise make
		 * sure we are showing the images
		 */
		if (isEmpty()) {
			refreshItemsAsync();
		} else {
			showItems();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		/*
		 * Change thea activity title to the name of the collection we are
		 * displaying
		 */
		((PicdoraActivity) getActivity()).setActionBarTitle(getCollection()
				.getName());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		mUtils.deleteCollectionImages(mCollection,
				(List<Image>) (List<?>) selection);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Selectable> doItemLoad() {
		return (List<Selectable>) (List<?>) mUtils
				.loadCollectionImages(mCollection);
	}

	@Override
	protected String getEmptyMessage() {
		return getResources().getString(R.string.collection_no_images);
	}

	/**
	 * Get the collection that we are showing.
	 * 
	 * @return
	 */
	public Collection getCollection() {
		/*
		 * Init the collection from json if not done yet.
		 */
		if (mCollection == null) {
			mCollection = Util.fromJson(mCollectionAsJson, Collection.class);
		}
		return mCollection;
	}

}
