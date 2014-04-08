package com.picdora.collections;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EFragment;

import com.picdora.R;
import com.picdora.Util;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragmentWithNew;

@EFragment(R.layout.fragment_selection_grid_with_new)
public class CollectionSelectionFragment extends SelectionFragmentWithNew {

	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		Util.log("delete");

	}

	@Override
	protected void onClick(Selectable item) {
		Util.log("click");

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
		Util.log("new");
	}

}
