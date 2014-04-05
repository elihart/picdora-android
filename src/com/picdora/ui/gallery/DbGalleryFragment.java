package com.picdora.ui.gallery;

import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;

import com.picdora.R;
import com.picdora.models.Image;

/**
 * Provides logic for loading images for the gallery out of the database. This
 * class ensures that the db access is done on background threads, and manages
 * concurrent loads to ensure that only the latest load is displayed. For
 * example, if the dataset changes and you call a load while another load is in
 * progress the second load will be canceled asap and only the second one will
 * display it's results.
 * <p>
 * In addition, the progress screen is shown during loads and the images are
 * shown on load completion. If the load results in an empty image set then an
 * empty message is displayed.
 * <p>
 * To use this class, call {@link #loadImagesFromDb()} whenever you want to
 * retrieve a fresh set of images. This will trigger a callback to
 * {@link #getImagesFromDb()} which you should override to implement your db
 * access. Lastly, override {@link #getEmptyMessage()} to provide a message to
 * show if the resulting image set is empty.
 */
@EFragment(R.layout.fragment_image_grid)
public abstract class DbGalleryFragment extends GalleryFragment {
	/** Whether an image load task is currently running */
	private volatile boolean mLoadInProgress = false;
	/**
	 * The most recent id we gave to an image load so when the task ends it can
	 * tell if it was most recent or not
	 */
	private volatile int mCurrentLoadBatchId = 0;

	/**
	 * Start a background task to load images from the db.
	 * {@link #getImagesFromDb()} should be overridden to provide the db access
	 * logic.
	 * 
	 */
	protected synchronized void loadImagesFromDb() {
		mLoadInProgress = true;
		showProgress();
		/* Increment the latest batch id */
		asyncLoad(++mCurrentLoadBatchId);
	}

	/**
	 * Call {@link #getImagesFromDb()} in the background. Display results on
	 * completion only if a more recent load hasn't started after us.
	 * 
	 * @param batchId
	 */
	@Background
	protected void asyncLoad(int batchId) {
		List<Image> images = getImagesFromDb();

		/*
		 * Update the display on the ui thread if the fragment wasn't destroyed
		 * while we were getting images and if our batch is the most recent
		 */
		if (isDestroyed()) {
			mLoadInProgress = false;
		} else if (batchId == mCurrentLoadBatchId) {
			mLoadInProgress = false;
			displayLoadResult(images);			
		}
	}

	/**
	 * Override this to access the images from the database that you want to
	 * display.
	 * 
	 * @return
	 */
	protected abstract List<Image> getImagesFromDb();

	/**
	 * Whether a database access is currently in progress to load images.
	 * 
	 * @return
	 */
	protected boolean isLoadInProgress() {
		return mLoadInProgress;
	}

	/**
	 * Need to use the ui thread to display the refreshed images
	 * 
	 * @param images
	 */
	@UiThread
	protected void displayLoadResult(List<Image> images) {
		setImagesToShow(images);
		showImages();
	}

	/**
	 * Show the image grid if we have images, but if our image list is empty
	 * then show a message instead.
	 * 
	 */
	protected void showImages() {
		if (isLoadInProgress()) {
			showProgress();
		} else if (isImagesEmpty()) {
			showMessage(getEmptyMessage());
		} else {
			showImageGrid();
		}
	}

	/**
	 * Get a message to show when there are no images to show.
	 * 
	 * @return
	 */
	protected abstract String getEmptyMessage();

}
