package com.picdora.collections;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;

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
	protected String mImageAsJson;
	@Bean
	protected CollectionUtil mUtils;

	/** The Collection to display */
	private Collection mCollection;

	@AfterViews
	protected void initCollection() {
		/**
		 * If our Collection hasn't been init'd from json yet then do it and
		 * load the images
		 */
		if (mCollection == null) {
			mCollection = Util.fromJson(mImageAsJson, Collection.class);
			refreshItemsAsync();
		}
	}

	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		mUtils.deleteCollectionImages(mCollection, (List<Image>) (List<?>) selection);
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

}
