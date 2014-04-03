package com.picdora.ui.gallery;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.models.Image;
import com.picdora.ui.ActionSpinner;
import com.picdora.ui.grid.GridItemView;
import com.picdora.ui.grid.GridSize;
import com.picdora.ui.grid.ModelGridSelector;
import com.picdora.ui.grid.ModelGridSelector.OnGridItemClickListener;

/**
 * Displays Images in a grid and provides all the functionality for selections,
 * showing loading progress, and showing an error message.
 * <p>
 * This works similar to the Gallery app. A list of Images must first be
 * provided, and they are then shown in a grid. The user can click on any image
 * to interact with it; {@link #onImageClick(Image)} needs to be subclassed to
 * handle a click. This also provides support for selections like the Gallery
 * app. A selection is started by a long click. More images can then be added by
 * either long clicking or normal clicking other images. Long clicking or short
 * clicking an already selected image will deselect it. Anytime the set of
 * selected images changes {@link #onSelectionChanged(List)} will be called with
 * the set of currently selected images.
 * <p>
 * The client can manually call {@link #showProgress()} and
 * {@link #showMessage(String)} when they want to show a progress bar or a
 * message grid.
 * <p>
 * A collapsed action view Spinner is placed in the action bar to allow the user
 * to choose the size of the images in the grid.
 * 
 */
@EFragment(R.layout.fragment_image_grid)
public abstract class GalleryFragment extends Fragment implements
		OnGridItemClickListener<Image> {

	@Pref
	protected PicdoraPreferences_ mPrefs;

	@ViewById
	protected ProgressBar progress;
	@ViewById
	protected TextView messageText;
	@ViewById
	protected FrameLayout gridContainer;

	protected GalleryAdapter mAdapter;
	protected ModelGridSelector<Image> mImageSelector;

	/** The spinner in the actionbar for selecting image size */
	private ActionSpinner mActionSizeSpinner;

	/**
	 * Whether any images are currently selected. False to begin with since no
	 * images are selected at start.
	 */
	private boolean mImagesSelected = false;

	/**
	 * Keep track of whether the fragment has been destroyed for use in
	 * {@link #isDestroyed()}
	 */
	private volatile boolean mDestroyed = false;

	@AfterViews
	protected void init() {
		setHasOptionsMenu(true);

		/*
		 * Retain state between config changes so we don't have to load images
		 * all over again.
		 */
		setRetainInstance(true);

		/*
		 * If we retained state then we shouldn't recreate these.
		 */
		if (mImageSelector == null) {
			mAdapter = GalleryAdapter_.getInstance_(getActivity());

			/*
			 * Start with empty list of images and nothing selected.
			 */
			mImageSelector = new ModelGridSelector<Image>(getActivity(),
					new ArrayList<Image>(), null, mAdapter);

			mImageSelector.setRequireLongClick(true);

			boolean pauseOnScroll = false;
			boolean pauseOnFling = true;

			PauseOnScrollListener listener = new PauseOnScrollListener(
					ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);

			mImageSelector.setScrollListener(listener);
			mImageSelector.setOnClickListener(this);
		}

		/*
		 * If this view was recreated then we need to remove our image selector
		 * from the old view before adding it to the new one
		 */
		View v = mImageSelector.getView();
		ViewGroup parent = (ViewGroup) v.getParent();
		if (parent != null) {
			parent.removeView(v);
		}

		/* Add grid view to the fragment */
		gridContainer.addView(v);

		/* set the default grid size */
		GridSize size = GridSize.values()[mPrefs.gridSize().get()];
		setGridSize(size);

		/* Collapse the size spinner when a touch happens in the grid */
		mImageSelector.setGridTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mActionSizeSpinner.collapseSpinner();
				return false;
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.fragment_gallery, menu);

		/* Init the size spinner */
		initSizeSpinner(menu.findItem(R.id.size_spinner));

		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * Initialize the size spinner in the action bar
	 * 
	 * @param spinnerItem
	 *            The menu item containing the spinner action view.
	 * 
	 */
	private void initSizeSpinner(MenuItem spinnerItem) {
		mActionSizeSpinner = (ActionSpinner) MenuItemCompat
				.getActionView(spinnerItem);

		Spinner spinner = mActionSizeSpinner.getSpinner();

		final GridSizeArrayAdapter adapter = new GridSizeArrayAdapter(
				getActivity(), R.layout.action_spinner_item, GridSize.values());

		spinner.setAdapter(adapter);

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			/*
			 * Spinners have a strange behavior where they do an automatic
			 * selection when they are first displayed, without any user input.
			 * We want to ignore this first one and only respond to the user's
			 * selections
			 */
			boolean firstSelection = true;

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				/* Ignore automatic first selection */
				if (firstSelection) {
					firstSelection = false;
					return;
				}

				GridSize size = adapter.getItem(position);
				setGridSize(size);
				mActionSizeSpinner.collapseSpinner();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		/* Set to last selected value */
		spinner.setSelection(mPrefs.gridSize().get());
	}

	/**
	 * The grid size to use when displaying the images.
	 * 
	 * @param size
	 */
	public void setGridSize(GridSize size) {
		// save the preference first.
		mPrefs.gridSize().put(size.ordinal());
		mImageSelector.setGridSize(size);
	}

	@Override
	public void onGridItemClick(GridItemView view, Image item) {
		/*
		 * We need to watch out for the case where the only selected image is
		 * clicked, which unselects it. The list of selected images will be
		 * reported as empty now, but it still isn't a normal click since it's
		 * purpose was deselection. We check for this by keeping track of
		 * whether any images were selected on the last click.
		 */
		if (!mImagesSelected) {
			/*
			 * Just a normal click with no selection going on.
			 */
			onImageClick(item);
		} else {
			/*
			 * Otherwise we are in selection mode so a click does the same thing
			 * as a long click.
			 */
			onGridItemLongClick(view, item);
		}
	}

	@Override
	public void onGridItemLongClick(GridItemView view, Image item) {
		List<Image> selected = getSelectedImages();
		mImagesSelected = !selected.isEmpty();
		onSelectionChanged(selected);
	}

	/**
	 * Show the progress bar and hide the image grid.
	 */
	@UiThread
	public void showProgress() {
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
	public void showMessage(String msg) {
		messageText.setText(msg);

		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.GONE);
		messageText.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the image grid and hide the progress bar and message text
	 */
	@UiThread
	public void showImageGrid() {
		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.VISIBLE);
		messageText.setVisibility(View.GONE);
	}

	/**
	 * Called when the set of selected images changes. This includes images
	 * being added or removed from the set. If the list is empty then the last
	 * selected image was removed and no images are currently selected
	 * 
	 * @param selectedImages
	 *            List of currently selected images. Empty if no images are
	 *            selected.
	 */
	protected abstract void onSelectionChanged(List<Image> selectedImages);

	/**
	 * Called when an image is clicked that isn't part of the selection process.
	 * In other words, if the user has long clicked and is clicking other images
	 * to include in the selection this won't be call.
	 * 
	 * @param image
	 */
	protected abstract void onImageClick(Image image);

	/**
	 * Set the images to be displayed in the grid. Clear any currently selected
	 * images.
	 * 
	 * @param images
	 */
	public void setImagesToShow(List<Image> images) {
		clearSelectedImages();
		mImageSelector.setItems(images);
	}

	/**
	 * Whether our image list is currently empty.
	 * 
	 * @return True if we don't have any images set, false otherwise.
	 */
	public boolean isImagesEmpty() {
		return mAdapter.getItems().isEmpty();
	}

	/**
	 * Get the list of images currently selected
	 * 
	 * @return Currently selected images. Empty if no images are selected.
	 */
	public List<Image> getSelectedImages() {
		return mImageSelector.getSelectedItems();
	}

	/**
	 * Clear the list of selected images. Will call
	 * {@link #onSelectionChanged(List)} once the list is cleared.
	 * 
	 */
	public void clearSelectedImages() {
		mImageSelector.getSelectedItems().clear();
		mAdapter.notifyDataSetChanged();

		onSelectionChanged(mImageSelector.getSelectedItems());
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
