package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;
import com.picdora.ui.UiUtil;

/**
 * Use this class with the {@link #ModelGridAdapter} and the
 * {@link #GridItemView} to create a grid of images representing models that the
 * user can choose from. Clicking on an item in the grid selects it, and
 * multiple items can be selected at once. For example, this is used to show a
 * list of all categories so the user can choose which categories to use in a
 * channel.
 * <p>
 * The class is parameterized so it can work with different model types in
 * different circumstances. Right now this includes Channels and Categories, for
 * use in dialogs, and fragments.
 * 
 * @param <T>
 */
public class ModelGridSelector<T> {
	protected List<T> mSelectedItems;
	protected Context mContext;
	protected GridView mGrid;
	protected ModelGridAdapter<T> mAdapter;
	protected OnGridItemClickListener<T> mClickListener;

	/**
	 * Whether the selection process must begin with a long click for the first
	 * item. Default to false.
	 */
	protected boolean mRequireLongClick = false;
	/** The current size we are using for the grid. */
	private GridSize mGridSize;
	/** Default grid size */
	private static final GridSize DEFAULT_GRID_SIZE = GridSize.MEDIUM;

	/**
	 * Create a new ImageGridSelector.
	 * 
	 * @param context
	 * @param availableItems
	 *            The list of items to show in the grid. Must not be null.
	 * @param selectedItems
	 *            Which items should start as selected. use an empty list or
	 *            null for no items initially selected. If a list is provided it
	 *            will be updated automatically as items are
	 *            selected/deselected.
	 * @param adapter
	 *            The adapter to use. Subclass ImageGridAdapter to provide
	 *            support for a specific model. Must not be null.
	 */
	public ModelGridSelector(Context context, List<T> availableItems,
			List<T> selectedItems, ModelGridAdapter<T> adapter) {
		if (availableItems == null) {
			throw new IllegalArgumentException(
					"AvailableItems must not be null");
		}
		if (adapter == null) {
			throw new IllegalArgumentException("adapter must not be null");
		}

		mContext = context;

		/*
		 * If a list of selected items was provided then use it, otherwise
		 * create an empty one
		 */
		if (selectedItems == null) {
			mSelectedItems = new ArrayList<T>();
		} else {
			mSelectedItems = selectedItems;
		}

		mGrid = (GridView) LayoutInflater.from(context).inflate(
				R.layout.image_grid, null);
		mAdapter = adapter;
		mAdapter.setSelectedItems(mSelectedItems);
		mAdapter.setAvailableItems(availableItems);

		mGrid.setAdapter(mAdapter);

		// tell the image loader to pause on fling scrolling
		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;
		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		mGrid.setOnScrollListener(listener);

		// setup click listener that selects/deselects image and reports click
		// to listener
		mGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				itemClicked((GridItemView) view, mAdapter.getItem(pos), false);
			}
		});

		// set up long click listener
		mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int pos, long id) {
				itemClicked((GridItemView) view, mAdapter.getItem(pos), true);
				return true;
			}
		});

		/* Init default size */
		setGridSize(DEFAULT_GRID_SIZE);
	}

	/**
	 * Set the items to show
	 * 
	 * @param items
	 */
	public void setItems(List<T> items) {
		mAdapter.setAvailableItems(items);
	}

	/**
	 * Get the list of currently selected items. This list will be updated as
	 * the selections change
	 * 
	 * @return
	 */
	public List<T> getSelectedItems() {
		return mSelectedItems;
	}
	

	/**
	 * Set the list of items to be selected.
	 * 
	 * @param items
	 */
	public void setSelectedItems(List<T> items){
		mSelectedItems.clear();
		mSelectedItems.addAll(items);
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Get the gridview used to show the items. Use this to display the items in
	 * a fragment or dialog
	 * 
	 * @return
	 */
	public View getView() {
		return mGrid;
	}

	/**
	 * Add a listener for when an item in the grid is clicked
	 * 
	 * @param listener
	 */
	public void setOnClickListener(OnGridItemClickListener<T> listener) {
		mClickListener = listener;
	}

	/**
	 * Handle an item click in the grid. Add/remove it from the selected items
	 * list, highlight it or unhighlight it as necessary, and call the onClick
	 * listener if there is one registered
	 * 
	 * @param view
	 *            The view that was clicked on
	 * @param item
	 *            The item represented by the view
	 * @param longClick
	 *            True if the click was long
	 */
	protected void itemClicked(GridItemView view, T item, boolean longClick) {

		/*
		 * Add the clicked item to the list of selected items. However, if a
		 * long click is required to start the selection process than make sure
		 * the conditions are met; if the selected list already has items in it
		 * then the selection process has already been started so keep going,
		 * otherwise check if a long click is required.
		 */
		if (!mSelectedItems.isEmpty() || !mRequireLongClick
				|| (mRequireLongClick && longClick)) {

			// highlight/unhighlight item and add/remove it to the selected list
			if (mSelectedItems.contains(item)) {
				mSelectedItems.remove(item);
				view.setHighlighted(false);
			} else {
				mSelectedItems.add(item);
				view.setHighlighted(true);
			}
		}

		if (mClickListener != null) {
			if (longClick) {
				mClickListener.onGridItemLongClick(view, item);
			} else {
				mClickListener.onGridItemClick(view, item);
			}
		}
	}

	/**
	 * Set whether the selection process should require a long click to be
	 * started.
	 * 
	 * @param longClickRequired
	 */
	public void setRequireLongClick(boolean longClickRequired) {
		mRequireLongClick = longClickRequired;
	}

	/**
	 * Call back for when an item in the grid is clicked
	 * 
	 * @param <T>
	 *            The type of item
	 */
	public interface OnGridItemClickListener<T> {
		/**
		 * Called when an item in the grid is clicked
		 * 
		 * @param view
		 *            The view representing the item
		 * @param item
		 *            The model item selected
		 */
		public void onGridItemClick(GridItemView view, T item);

		public void onGridItemLongClick(GridItemView view, T item);
	}

	/**
	 * Set a scroll listener for when the grid scrolls
	 * 
	 * @param listener
	 */
	public void setScrollListener(PauseOnScrollListener listener) {
		mGrid.setOnScrollListener(listener);
	}

	/**
	 * Set the size of the grid to use.
	 * 
	 * @param size
	 */
	public void setGridSize(GridSize size) {
		mGridSize = size;
		mGrid.setColumnWidth(UiUtil.dpToPixel(size.getColumnWidth()));
		mAdapter.setImageSize(size.getImageSize());
	}

	/**
	 * Get the size of a grid column.
	 * 
	 */
	public GridSize getGridSize() {
		return mGridSize;
	}

	/**
	 * Get all the items we are displaying.
	 * 
	 * @return
	 */
	public List<T> getItems() {
		return mAdapter.getItems();
	}

	/**
	 * Select all images in the grid.
	 * 
	 */
	public void selectAll() {
		/*
		 * Clear currently selected images before adding all images to avoid
		 * duplicates
		 */
		mSelectedItems.clear();
		mSelectedItems.addAll(getItems());
		mAdapter.notifyDataSetChanged();
	}

}
