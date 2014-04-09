package com.picdora.collections;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.collections.CollectionUtil.OnCollectionCreatedListener;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragmentWithNew;

@EFragment(R.layout.fragment_selection_grid_with_new)
public class CollectionSelectionFragment extends SelectionFragmentWithNew
		implements OnCollectionCreatedListener {
	
	@Pref
	protected PicdoraPreferences_ mPrefs;
	
	@Bean
	protected CollectionUtil mUtils;

	@SuppressWarnings("unchecked")
	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		/*
		 * Some hacky casting to get the interface object back to a collection
		 * object...
		 */
		List<Collection> collections = new ArrayList<Collection>(
				(List<Collection>) (List<?>) selection);
		mUtils.delete(collections);
	}

	@Override
	protected void onClick(Selectable item) {
		Collection collection = (Collection) item;
		/* Show gallery fragment with the collection's images */

	}

	@Override
	protected List<Selectable> doItemLoad() {
		/** Get all collections from the db. */
		List<Collection> collections = CollectionUtil.getAllCollections(mPrefs
				.showNsfw().get());
		/** Cast them to selectables so they can be used in the grid. */
		return new ArrayList<Selectable>(collections);
	}

	@Override
	protected String getEmptyMessage() {
		return getResources().getString(R.string.collections_empty_message);

	}

	protected String getCreateButtonText() {
		return getResources().getString(R.string.collections_button_create_new);
	}

	@Override
	protected void createNew() {
		mUtils.showCollectionCreationDialog(getActivity(), this);
	}

	@UiThread
	@Override
	public void onSuccess(Collection collection) {
		Util.makeBasicToast(getActivity(), "Collection created!");
		refreshItemsAsync();
	}

	@UiThread
	@Override
	public void onFailure(CollectionUtil.CreationError error) {
		mUtils.alertCreationError(getActivity(), error, this);
	}

}
