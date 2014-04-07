package com.picdora.ui.gallery;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.ImageUtils;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Image;
import com.picdora.ui.ActionSpinner;
import com.picdora.ui.PicdoraDialog;
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
 * <p>
 * This fragment also handles creating an ActionMode context menu when images
 * are selected.
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

	/** ActionMode for showing contextual options for selected images */
	private ActionMode mActionMode;

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
		/* We want to add our size spinner to the action bar */
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
			/* Show progress bar until we show images */
			showProgress();

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

		/*
		 * If we have a lingering action mode or selected images then create a
		 * fresh action mode.
		 */
		if (mActionMode != null || !getSelectedImages().isEmpty()) {
			/*
			 * Easiest way to recreate is just forget about the old one and
			 * remind ourselves of the selected items.
			 */
			mActionMode = null;
			onSelectionChanged(getSelectedImages());
		}
	}

	/**
	 * Called when the contextual ActionMode for selected images is being
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
			case R.id.download:
				downloadSelection();
				return true;
			case R.id.delete:
				doDeleteConfirmation();
				return true;
			case R.id.share:
				shareSelection();
				return true;
			}

			return onSelectionAction(item);
		}

		public void onDestroyActionMode(ActionMode mode) {
			/*
			 * If we are closing and images are still selected then deselect
			 * them
			 */
			if (!getSelectedImages().isEmpty()) {
				clearSelectedImages();
			}
		}
	};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.fragment_gallery, menu);

		/* Init the size spinner */
		initSizeSpinner(menu.findItem(R.id.size_spinner));

		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * Open a share intent dialog to share the selected items.
	 * 
	 */
	protected void shareSelection() {
		ImageUtils.shareImages(getActivity(), getSelectedImages());
	}

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
	 * Remove the currently selected images from the grid and alert subclasses
	 * so they can handle the deletion as they choose.
	 * 
	 */
	private void deleteSelection() {
		/*
		 * All images and selected images. Create a copy of the selected images
		 * because we need to pass on the selection to subclasses, and the
		 * original selection will be cleared after this.
		 */
		List<Image> allImages = mImageSelector.getItems();
		List<Image> imagesToDelete = new ArrayList<Image>(getSelectedImages());

		/* Duplicate all images list and remove the selected images */
		List<Image> result = new ArrayList<Image>(allImages);
		result.removeAll(imagesToDelete);

		/* Set the resulting list with the images removed */
		mImageSelector.setItems(result);

		/* Clear the selection */
		clearSelectedImages();

		/* Pass the deleted images on to subclasses to handle cleanup */
		onSelectionDeleted(imagesToDelete);
	}

	/**
	 * Called when the user has chosen to delete the selection. The images will
	 * be removed from the grid automatically, but the subclass can handle any
	 * further cleanup it wants.
	 * 
	 * @param deletedImages
	 */
	protected abstract void onSelectionDeleted(List<Image> deletedImages);

	/**
	 * Download the currently selected images.
	 * 
	 */
	private void downloadSelection() {
		/* Use a queue to download each image one at a time */
		List<Image> imagesToDownload = getSelectedImages();
		int count = imagesToDownload.size();

		/*
		 * Show a confirmation message saying the download is starting and how
		 * many images will be downloaded. Customize language for
		 * singular/plural
		 */
		String startMessage;
		if (count > 1) {
			startMessage = getResources().getString(
					R.string.gallery_download_start_alert_multiple, count);
		} else {
			startMessage = getResources().getString(
					R.string.gallery_download_start_alert_singular);
		}
		Util.makeBasicToast(getActivity(), startMessage);

		new DownloadQueue(getActivity(), imagesToDownload).start();
	}

	/**
	 * Select all images in the grid.
	 * 
	 */
	private void selectAll() {
		mImageSelector.selectAll();
		onSelectionChanged(getSelectedImages());
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

		final GridSizeArrayAdapter adapter = new GridSizeArrayAdapter(
				getActivity(), R.layout.action_spinner_view_dropdown, GridSize.values());

		mActionSizeSpinner.setAdapter(adapter);

		mActionSizeSpinner.setSelectionListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				GridSize size = adapter.getItem(position);
				if (getGridSize() != size) {
					setGridSize(size);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		/* Set to last selected value */
		mActionSizeSpinner.setSelection(mPrefs.gridSize().get());
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

	/**
	 * The last grid size set with {@link #setGridSize(GridSize)}
	 * 
	 * @return
	 */
	private GridSize getGridSize() {
		return GridSize.values()[mPrefs.gridSize().get()];
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
	 * selected image was removed and no images are currently selected. If you
	 * subclass this method be sure to call super.
	 * 
	 * @param selectedImages
	 *            List of currently selected images. Empty if no images are
	 *            selected.
	 */
	protected void onSelectionChanged(List<Image> selectedImages) {
		/* Start an action mode to show options for the selected images */
		if (mActionMode == null && !selectedImages.isEmpty()) {
			mActionMode = ((PicdoraActivity) getActivity())
					.startSupportActionMode(mActionModeCallback);
		}

		/* End the action mode if the selected images were cleared */
		else if (selectedImages.isEmpty() && mActionMode != null) {
			mActionMode.finish();
			mActionMode = null;
		}

		/*
		 * If the action mode exists set the title to be the number of selected
		 * images
		 */
		if (mActionMode != null) {
			mActionMode.setTitle(Integer.toString(selectedImages.size()));
		}
	}

	/**
	 * Called when an image is clicked that isn't part of the selection process.
	 * In other words, if the user has long clicked and is clicking other images
	 * to include in the selection this won't be call.
	 * <p>
	 * The default action will be to show the clicked image in fullscreen. The
	 * method can be overridden for custom functionality.
	 * 
	 * @param image
	 */
	protected void onImageClick(Image image) {		
        FullscreenFragment frag = new FullscreenFragment_();
        frag.setImage(image);
       
        FragmentManager fm = getActivity().getSupportFragmentManager();
        frag.show(fm, "fragment_fullscreen");
	}

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

	/**
	 * Collapse any open action views.
	 * 
	 */
	public void collapseActionViews() {
		mActionSizeSpinner.collapse();
	}

}
