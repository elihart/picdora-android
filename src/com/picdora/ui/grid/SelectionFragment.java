package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;
import org.androidannotations.annotations.ViewById;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.grid.ExtendedRelativeLayout.OnDispatchTouchListener;
import com.picdora.ui.grid.ModelGridSelector.OnGridItemClickListener;

/**
 * Basic implementation for creating a fragment to display items for selection.
 * 
 */
@EFragment(R.layout.fragment_basic_grid)
public abstract class SelectionFragment extends Fragment implements
		OnGridItemClickListener<Selectable> {
	@ViewById
	protected ExtendedRelativeLayout root;
	@ViewById
	protected ProgressBar progress;
	@ViewById
	protected TextView messageText;
	@ViewById(R.id.message_container)
	protected RelativeLayout mMessageContainer;
	@ViewById
	protected FrameLayout gridContainer;

	protected SelectionAdapter mAdapter;
	protected ModelGridSelector<Selectable> mSelector;
	/** The default grid size */
	private static final GridSize DEFAULT_GRID_SIZE = GridSize.MEDIUM;

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
	 * True to show text overlaid on the item image, false otherwise. Defaults
	 * to true.
	 */
	private boolean mShowText = true;

	/**
	 * If we should start a selection on a normal click. Defaults to false so
	 * that selection won't start until a long click.
	 */
	private boolean mSelectOnShortClick = false;

	/**
	 * Whether the action mode should be shown when a selection is made. Default
	 * is true.
	 */
	private boolean mShowActionMode = true;

	/**
	 * Keep track of whether the fragment has been destroyed. For use in
	 * {@link #isDestroyed()}
	 */
	private volatile boolean mDestroyed = false;
	/**
	 * Whether the view has been destroyed. Use this to check for restarts due
	 * to config changes.
	 */
	private boolean mViewDestroyed = false;
	
	/** A copy of the selected items used to restore selected state after refreshing the items to show. */
	private List<Selectable> mSavedSelection;

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
			mAdapter.setShowText(mShowText);

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

			/* set the default grid size */
			setGridSize(DEFAULT_GRID_SIZE);
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

		/*
		 * Setup a dispatch listener so we can tell when there is a touch
		 * anywhere in the fragment. This will let us know when to collapse
		 * spinners in the action bar.
		 */
		root.setOnDispatchListener(new OnDispatchTouchListener() {

			@Override
			public void onDispatchTouch(MotionEvent ev) {
				onFragmentTouch();
			}
		});
	}

	/**
	 * Called when there is a touch event somewhere in the fragment. Does
	 * nothing by default, but override to get a hook into the event.
	 * 
	 */
	protected void onFragmentTouch() {

	}

	@Override
	public void onResume() {
		super.onResume();
		/*
		 * The fragment is being shown so let's make sure we have the most up to
		 * date items. However, if the view was destroyed and the fragment
		 * wasn't destroyed then we were restarted due to config changes so
		 * let's not refresh the images in that case but just make sure they are
		 * showing.
		 */
		if (mViewDestroyed && !mDestroyed) {
			mViewDestroyed = false;
			showItems();
		} else {
			refreshItemsAsync();
		}
	}

	/**
	 * Whether the grid items should show text overlaid on the image.
	 * 
	 * @param show
	 *            True to show text, false otherwise.
	 */
	protected void setShowText(boolean show) {
		mShowText = show;
		if (mAdapter != null) {
			mAdapter.setShowText(show);
		}
	}

	/**
	 * Set whether the selection mode should start with a short click. Default
	 * is false, where selection doesn't start until a long click is noted.
	 * 
	 * @param select
	 *            True to select on short click.
	 */
	public void setSelectOnShortClick(boolean select) {
		mSelectOnShortClick = select;
		mSelector.setRequireLongClick(!mSelectOnShortClick);
	}

	/**
	 * Set whether the action mode options bar should be shown for a selection.
	 * Default is true.
	 * 
	 * @param b
	 */
	protected void setShowSelectionOptions(boolean show) {
		mShowActionMode = show;
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
		inflater.inflate(R.menu.fragment_selection_cab, menu);
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
			default:
				return onSelectionAction(item);
			}

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
	 * Return the menu contained by our action mode, or null if there is no
	 * action mode at the moment.
	 * 
	 * @return
	 */
	protected Menu getActionModeMenu() {
		if (mActionMode != null) {
			return mActionMode.getMenu();
		} else {
			return null;
		}
	}

	/**
	 * Show a confirmation dialog on whether the user wants to delete the
	 * selection. On confirmation do the deletion, on cancel do nothing.
	 * 
	 */
	private void doDeleteConfirmation() {
		new PicdoraDialog.Builder(getActivity())
				.setTitle(R.string.selection_delete_confirmation_title)
				.setMessage(R.string.selection_delete_confirmation_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.selection_delete_confirmation_positive,
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

		/*
		 * Set the resulting list with the items removed. This also clears the
		 * selection.
		 */
		setItemsToShow(result);

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

	/**
	 * Get the current grid size, or if that is not available get the default
	 * grid size that the grid will be init'd to.
	 * 
	 * @return
	 */
	public GridSize getGridSize() {
		if (mSelector == null) {
			return DEFAULT_GRID_SIZE;
		} else {
			return mSelector.getGridSize();
		}
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
	@UiThread(propagation = Propagation.REUSE)
	protected void showProgress() {
		progress.setVisibility(View.VISIBLE);
		gridContainer.setVisibility(View.GONE);
		mMessageContainer.setVisibility(View.GONE);
	}

	/**
	 * Hide the grid and progress bars and show the given text
	 * 
	 * @param msg
	 *            The message to show
	 */
	@UiThread(propagation = Propagation.REUSE)
	protected void showMessage(String msg) {
		messageText.setText(msg);

		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.GONE);
		mMessageContainer.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the items grid and hide the progress bar and message text
	 */
	@UiThread(propagation = Propagation.REUSE)
	protected void showGrid() {
		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.VISIBLE);
		mMessageContainer.setVisibility(View.GONE);
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
		if (mActionMode == null && !selection.isEmpty() && mShowActionMode) {
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
	@UiThread(propagation = Propagation.REUSE)
	public void setItemsToShow(List<Selectable> items) {
		clearSelection();
		mSelector.setItems(items);
		showItems();
	}

	/**
	 * Set the items that should be selected. This will clear any currently
	 * selected items and replace them with the items in the given list.
	 * 
	 * @param selectable
	 */
	@UiThread(propagation = Propagation.REUSE)
	protected void setSelectedItems(List<Selectable> items) {
		mSelector.setSelectedItems(items);
	}

	/**
	 * Start a background task to load items to show. {@link #doItemLoad()}
	 * should be overridden to implement the load logic. This will remember
	 * which items are currently selected and will reselect them if the
	 * refreshed list contains them.
	 * 
	 */
	public synchronized void refreshItemsAsync() {
		mLoadInProgress = true;
		/* Copy the current selection so we can try to restore it after the load. */
		mSavedSelection = new ArrayList<Selectable>(getSelection());
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
			handleLoadResult(items);
		}
	}

	@UiThread
	protected void handleLoadResult(List<Selectable> items) {
		setItemsToShow(items);
		/* Restore selection if there was one.  */
		if(mSavedSelection != null){
			/* Remove any of the selection items that aren't in the new item list. */
			Iterator<Selectable> it = mSavedSelection.iterator();
			while (it.hasNext()) {
				if (!items.contains(it.next())) {
					it.remove();
				}
			}
			
			setSelectedItems(mSavedSelection);
			mSavedSelection = null;
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
	@UiThread(propagation = Propagation.REUSE)
	public void clearSelection() {
		mSelector.getSelectedItems().clear();
		mAdapter.notifyDataSetChanged();

		onSelectionChanged(getSelection());
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
		/*
		 * This won't be called on config changes since we are set to retain
		 * state
		 */
		mDestroyed = true;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		/*
		 * Keep track of whether the view was destroyed to know when the
		 * fragment is restarted due to config changes.
		 */
		mViewDestroyed = true;
	}

	/**
	 * Cast a list of objects to a list of Selectables. No type checking is done
	 * so the client must ensure the proper items are passed in.
	 * 
	 * @param list
	 *            A list of items that implement Selectable.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<Selectable> toSelectable(List<?> list) {
		return (List<Selectable>) (List<?>) list;
	}
}
