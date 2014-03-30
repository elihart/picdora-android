package com.picdora.ui.grid;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;
import com.picdora.models.Image;
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
 * The client must manually call {@link #showProgress()} and
 * {@link #showMessage(String)} when they want to show a progress bar or a
 * message grid.
 * 
 */
@EFragment(R.layout.fragment_image_grid)
public abstract class ImageGridFragment extends Fragment implements
		OnGridItemClickListener<Image> {

	@ViewById
	protected ProgressBar progress;
	@ViewById
	protected TextView messageText;
	@ViewById
	protected FrameLayout gridContainer;

	@Bean
	protected ImageGridAdapter mAdapter;
	protected ModelGridSelector<Image> mImageSelector;

	/**
	 * Whether any images are currently selected. False to begin with since no
	 * images are selected at start.
	 */
	private boolean mImagesSelected = false;

	@AfterViews
	protected void init() {
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

		gridContainer.addView(mImageSelector.getView());

		mImageSelector.setOnClickListener(this);
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
	public void showMessage(String msg) {
		messageText.setText(msg);

		progress.setVisibility(View.GONE);
		gridContainer.setVisibility(View.GONE);
		messageText.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the image grid and hide the progress bar and message text
	 */
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
	 * Set the images to be displayed in the grid.
	 * 
	 * @param images
	 */
	public void setImagesToShow(List<Image> images) {
		mImageSelector.setItems(images);
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

}
