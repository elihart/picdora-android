package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.grid.ModelGridSelector.OnGridItemClickListener;

/**
 * Basic implementation for creating a fragment to display items for selection.
 * 
 */
@EFragment(R.layout.fragment_basic_grid)
public abstract class SelectionFragment extends Fragment implements
		OnGridItemClickListener<Selectable> {

	@Pref
	protected PicdoraPreferences_ mPrefs;

	@ViewById
	protected ProgressBar progress;
	@ViewById
	protected TextView messageText;
	@ViewById
	protected FrameLayout gridContainer;

	protected SelectionAdapter mAdapter;
	protected ModelGridSelector<Selectable> mSelector;

	/** ActionMode for showing contextual options for selection */
	private ActionMode mActionMode;

	/** Whether an load task is currently running */
	private volatile boolean mLoadInProgress = false;
	/**
	 * The most recent id we gave to a load so when the task ends it can tell if
	 * it was most recent or not
	 */
	private volatile int mCurrentLoadBatchId = 0;

	/**
	 * Whether any items are currently selected. False to begin with since
	 * nothing is selected at start.
	 */
	private boolean mInSelection = false;

	/**
	 * If we should start a selection on a normal click. Defaults to false so
	 * that selection won't start until a long click.
	 */
	private boolean mSelectOnShortClick = false;

	/**
	 * Keep track of whether the fragment has been destroyed. For use in
	 * {@link #isDestroyed()}
	 */
	private volatile boolean mDestroyed = false;

	@AfterViews
	protected void init() {

		/*
		 * Retain state between config changes so we don't have to load items
		 * all over again.
		 */
		setRetainInstance(true);

		/* Let us add options to the action bar */
		setHasOptionsMenu(true);

		/*
		 * If we retained state then we shouldn't recreate these.
		 */
		if (mSelector == null) {
			/* Show progress bar until we show items */
			showProgress();

			mAdapter = SelectionAdapter_.getInstance_(getActivity());

			/*
			 * Start with empty list of items and nothing selected.
			 */
			mSelector = new ModelGridSelector<Selectable>(getActivity(),
					new ArrayList<Selectable>(), null, mAdapter);

			mSelector.setRequireLongClick(!mSelectOnShortClick);

			boolean pauseOnScroll = false;
			boolean pauseOnFling = true;

			PauseOnScrollListener listener = new PauseOnScrollListener(
					ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);

			mSelector.setScrollListener(listener);
			mSelector.setOnClickListener(this);
		}

		/*
		 * If this view was recreated then we need to remove our selector from
		 * the old view before adding it to the new one
		 */
		View v = mSelector.getView();
		ViewGroup parent = (ViewGroup) v.getParent();
		if (parent != null) {
			parent.removeView(v);
		}

		/* Add grid view to the fragment */
		gridContainer.addView(v);

		/* set the default grid size */
		GridSize size = GridSize.values()[mPrefs.gridSize().get()];
		setGridSize(size);

		/*
		 * If we have a lingering action mode or selected items then create a
		 * fresh action mode.
		 */
		if (mActionMode != null || !getSelection().isEmpty()) {
			/*
			 * Easiest way to recreate is just forget about the old one and
			 * remind ourselves of the selected items.
			 */
			mActionMode = null;
			onSelectionChanged(getSelection());
		}
	}

	/**
	 * Set whether the selection mode should start with a short click. Default
	 * is false, where selection doesn't start until a long click is noted.
	 * 
	 * @param select
	 *            True to select on short click.
	 */
	public void selectOnShortClick(boolean select) {
		mSelectOnShortClick = select;
		mSelector.setRequireLongClick(!mSelectOnShortClick);
	}

	/**
	 * Called when the contextual ActionMode for selected items is being
	 * created. Subclassed fragments can override this to add their own items to
	 * the ActionMode. If overridden be sure to call super.
	 * 
	 * @param inflater
	 * @param menu
	 */
	protected void onCreateSelectionMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.fragment_gallery_cab, menu);
	}

	/**
	 * Called when an item in the actionmode is clicked. Subclassed fragments
	 * can override this to listen for their items being clicked. This will be
	 * called only if the parent doesn't use the item.
	 * 
	 * @param item
	 * @return
	 */
	protected boolean onSelectionAction(MenuItem item) {
		return false;
	}

	/**
	 * Callback options for when the actionmode changes or an option is selected
	 */
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			onCreateSelectionMenu(inflater, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.select_all:
				selectAll();
				return true;
			case R.id.delete:
				doDeleteConfirmation();
				return true;

			}

			return onSelectionAction(item);
		}

		public void onDestroyActionMode(ActionMode mode) {
			/*
			 * If we are closing and items are still selected then deselect them
			 */
			if (!getSelection().isEmpty()) {
				clearSelection();
			}
		}
	};

	/**
	 * Show a confirmation dialog on whether the user wants to delete the
	 * selection. On confirmation do the deletion, on cancel do nothing.
	 * 
	 */
	private void doDeleteConfirmation() {
		new PicdoraDialog.Builder(getActivity())
				.setTitle(R.string.gallery_delete_confirmation_title)
				.setMessage(R.string.gallery_delete_confirmation_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.gallery_delete_confirmation_positive,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								deleteSelection();
							}
						}).show();
	}

	/**
	 * Remove the currently selected items from the grid and alert subclasses so
	 * they can handle the deletion as they choose.
	 * 
	 */
	private void deleteSelection() {
		/*
		 * Get all items and the selection. Create a copy of the selected items
		 * because we need to pass on the selection to subclasses, and the
		 * original selection will be cleared after this.
		 */
		List<Selectable> allItems = mSelector.getItems();
		List<Selectable> itemsToDelete = new ArrayList<Selectable>(
				getSelection());

		/* Duplicate all items list and remove the selected items */
		List<Selectable> result = new ArrayList<Selectable>(allItems);
		result.removeAll(itemsToDelete);

		/* Set the resulting list with the items removed */
		mSelector.setItems(result);

		/* Clear the selection */
		clearSelection();

		/* Pass the deleted items on to subclasses to handle cleanup */
		onSelectionDeleted(itemsToDelete);
	}

	/**
	 * Called when the user has chosen to delete the selection. The items will
	 * be removed from the grid automatically, but the subclass can handle any
	 * further cleanup it wants.
	 * 
	 * @param selection
	 */
	protected abstract void onSelectionDeleted(List<Selectable> selection);

	/**
	 * Select all items in the grid.
	 * 
	 */
	private void selectAll() {
		mSelector.selectAll();
		onSelectionChanged(getSelection());
	}

	/**
	 * The grid size to use when displaying the items.
	 * 
	 * @param size
	 */
	public void setGridSize(GridSize size) {
		mSelector.setGridSize(size);
	}

	public GridSize getGridSize() {
		return mSelector.getGridSize();
	}

	@Override
	public void onGridItemClick(GridItemView view, Selectable item) {
		/*
		 * If we're not in selection mode and the user doesn't want to start
		 * selections with a short click then acknowledge the click.
		 */
		if (!mInSelection && !mSelectOnShortClick) {
			/*
			 * Just a normal click with no selection going on.
			 */
			onClick(item);
		} else {
			/*
			 * Otherwise we are in selection mode so a click does the same thing
			 * as a long click.
			 */
			onGridItemLongClick(view, item);
		}
	}

	@Override
	public void onGridItemLongClick(GridItemView view, Selectable item) {
		List<Selectable> selected = getSelection();
		mInSelection = !selected.isEmpty();
		onSelectionChanged(selected);
	}

	/**
	 * Show the progress bar and hide the items grid.
	 */
	@UiThread
	protected void showProgress() {
		progress.setVisibility(View.VISIBLE);
		gridContainer.setVisibility(View.GONE);
		messageText.setVisibility(View.GONE);
	}

	/**
	 * Hide the grid and progress bars and show the given text
	 * 
	 * @param msg
	 *            The message to show
	 */
	@UiThread
	protected void showMessage(String msg) {
		messageText.setText(msg);

		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.GONE);
		messageText.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the items grid and hide the progress bar and message text
	 */
	@UiThread
	protected void showGrid() {
		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.VISIBLE);
		messageText.setVisibility(View.GONE);
	}

	/**
	 * Called when the set of selected items changes. This includes items being
	 * added or removed from the set. If the list is empty then the last
	 * selected item was removed and no items are currently selected. If you
	 * subclass this method be sure to call super.
	 * 
	 * @param selection
	 *            List of currently selected items. Empty if nothing is
	 *            selected.
	 */
	protected void onSelectionChanged(List<Selectable> selection) {
		/* Start an action mode to show options for the selected items */
		if (mActionMode == null && !selection.isEmpty()) {
			mActionMode = ((PicdoraActivity) getActivity())
					.startSupportActionMode(mActionModeCallback);
		}

		/* End the action mode if the selected items were cleared */
		else if (selection.isEmpty() && mActionMode != null) {
			mActionMode.finish();
			mActionMode = null;
		}

		/*
		 * If the action mode exists set the title to be the number of selected
		 * items
		 */
		if (mActionMode != null) {
			mActionMode.setTitle(Integer.toString(selection.size()));
		}
	}

	/**
	 * Called when an item is clicked not as part of a selection.
	 * 
	 * @param item
	 */
	protected abstract void onClick(Selectable item);

	/**
	 * Set the items to be displayed in the grid. Clears any currently selected
	 * items.
	 * 
	 * @param items
	 */
	@UiThread
	public void setItemsToShow(List<Selectable> items) {
		clearSelection();
		mSelector.setItems(items);
		showItems();
	}

	/**
	 * Start a background task to load items to show. {@link #doItemLoad()}
	 * should be overridden to implement the load logic.
	 * 
	 */
	public synchronized void loadSelectablesAsyc() {
		mLoadInProgress = true;
		showProgress();
		/* Increment the latest batch id */
		asyncLoad(++mCurrentLoadBatchId);
	}

	/**
	 * Call {@link #doItemLoad()} in the background. Display results on
	 * completion only if a more recent load hasn't started after us.
	 * 
	 * @param batchId
	 */
	@Background
	protected void asyncLoad(int batchId) {
		List<Selectable> items = doItemLoad();

		/*
		 * Update the display on the ui thread if the fragment wasn't destroyed
		 * while we were getting items and if our batch is the most recent
		 */
		if (isDestroyed()) {
			mLoadInProgress = false;
		} else if (batchId == mCurrentLoadBatchId) {
			mLoadInProgress = false;
			setItemsToShow(items);
		}
	}

	/**
	 * Override this to implement the loading logic for the items you want to
	 * display. This will be called from a background thread so you don't have
	 * to worry about handling that.
	 * 
	 * @return
	 */
	protected abstract List<Selectable> doItemLoad();

	/**
	 * Show the item grid if we have items, but if our item list is empty then
	 * show a message instead.
	 * 
	 */
	protected void showItems() {
		if (mLoadInProgress) {
			showProgress();
		} else if (isEmpty()) {
			showMessage(getEmptyMessage());
		} else {
			showGrid();
		}
	}

	/**
	 * Get a message to show when there are no items to show.
	 * 
	 * @return
	 */
	protected abstract String getEmptyMessage();

	/**
	 * Whether the list of items to show is empty.
	 * 
	 * @return True if we don't have any items set, false otherwise.
	 */
	public boolean isEmpty() {
		return mAdapter.getItems().isEmpty();
	}

	/**
	 * Get the list of currently selected items
	 * 
	 * @return Currently selected items. Empty if nothing is selected.
	 */
	public List<Selectable> getSelection() {
		return mSelector.getSelectedItems();
	}

	/**
	 * Clear the list of selected items. Calls {@link #onSelectionChanged(List)}
	 * once the list is cleared.
	 * 
	 */
	public void clearSelection() {
		mSelector.getSelectedItems().clear();
		mAdapter.notifyDataSetChanged();

		onSelectionChanged(mSelector.getSelectedItems());
	}

	/**
	 * Whether the onDestroy method has been called for this fragment instance.
	 * 
	 */
	public boolean isDestroyed() {
		return mDestroyed;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDestroyed = true;
	}
}
