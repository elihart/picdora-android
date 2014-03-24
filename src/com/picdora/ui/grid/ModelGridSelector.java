package com.picdora.ui.grid;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;

/**
 * Use this class with the {@link #ModelGridAdapter} and the {@link #GridItemView} to create a
 * grid of images representing models that the user can choose from. Clicking on
 * an item in the grid selects it, and multiple items can be selected at once.
 * For example, this is used to show a list of all categories so the user can
 * choose which categories to use in a channel.
 * <p>
 * The class is parameterized so it can work with different model types in
 * different circumstances. Right now this includes Channels and Categories, for
 * use in dialogs, and fragments.
 * 
 * @param <T>
 */
public class ModelGridSelector<T> {
	protected List<T> mAvailableItems, mSelectedItems;
	protected Context mContext;
	protected GridView mGrid;
	protected ModelGridAdapter<T> mAdapter;
	protected OnGridItemClickListener<T> mClickListener;

	/**
	 * Create a new ImageGridSelector.
	 * 
	 * @param context
	 * @param availableItems
	 *            The list of items to show in the grid
	 * @param selectedItems
	 *            Which items should start as selected. use an empty list for no
	 *            items initially selected. This list will be updated
	 *            automatically as items are selected/deselected
	 * @param adapter
	 *            The adapter to use. Subclass ImageGridAdapter to provide
	 *            support for a specific model
	 */
	public ModelGridSelector(Context context, List<T> availableItems,
			List<T> selectedItems, ModelGridAdapter<T> adapter) {
		if (availableItems == null) {
			throw new IllegalArgumentException(
					"AvailableItems must not be null");
		}
		if (selectedItems == null) {
			throw new IllegalArgumentException("selectedItems must not be null");
		}
		if (adapter == null) {
			throw new IllegalArgumentException("adapter must not be null");
		}

		mContext = context;
		mAvailableItems = availableItems;
		mSelectedItems = selectedItems;

		mGrid = (GridView) LayoutInflater.from(context).inflate(
				R.layout.image_grid, null);
		mAdapter = adapter;
		mAdapter.setSelectedItems(mSelectedItems);
		mAdapter.setAvailableItems(mAvailableItems);

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
				itemClicked((GridItemView) view, mAdapter.getItem(pos));
			}
		});
	}

	/**
	 * Set the items to show
	 * 
	 * @param items
	 */
	public void setItems(List<T> items) {
		mAvailableItems = items;
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
	 * @param view The view that was clicked on
	 * @param item The item represented by the view
	 */
	protected void itemClicked(GridItemView view, T item) {

		// highlight/unhighlight item and add/remove it to the list
		if (mSelectedItems.contains(item)) {
			mSelectedItems.remove(item);
			view.setHighlighted(false);
		} else {
			mSelectedItems.add(item);
			view.setHighlighted(true);
		}

		if (mClickListener != null) {
			mClickListener.OnGridItemClick(view, item);
		}
	}

	/**
	 * Call back for when an item in the grid is clicked
	 *
	 * @param <T> The type of item 
	 */
	public interface OnGridItemClickListener<T> {
		/**
		 * Called when an item in the grid is clicked
		 * @param view The view representing the item
		 * @param item The model item selected
		 */
		public void OnGridItemClick(GridItemView view, T item);
	}
}
