package com.picdora.ui.grid;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

import com.picdora.R;

/**
 * Adds a new button to the action bar and shows a Create New button on empty.
 * 
 */
@EFragment(R.layout.fragment_selection_grid_with_new)
public abstract class SelectionFragmentWithNew extends SelectionFragment {
	@ViewById(R.id.createButton)
	protected Button mCreateButton;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_selection_with_new, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_new:
			createNew();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@AfterViews
	protected void initCreateButton() {
		mCreateButton.setText(getCreateButtonText());
	}

	@Click
	protected void createButtonClicked() {
		createNew();
	}

	/**
	 * Called when the user clicks to create a new item.
	 * 
	 */
	protected abstract void createNew();

	/**
	 * Get the text to use in the Create Button. Override to customize.
	 * 
	 * @return The text you want shown on the Create button.
	 */
	protected String getCreateButtonText() {
		return getResources().getString(R.string.selection_button_create_new);
	}
}
