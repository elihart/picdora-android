package com.picdora.collections;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EFragment;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.R;
import com.picdora.Util;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragment;

@EFragment(R.layout.fragment_basic_grid)
public class CollectionSelectionFragment extends SelectionFragment {

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_collection_selection, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_new_collection:
			createNewCollection();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void createNewCollection() {
		// TODO Auto-generated method stub
		
	}

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
		return "No collections.";
	}

}
