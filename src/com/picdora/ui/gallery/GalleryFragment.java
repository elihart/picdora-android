package com.picdora.ui.gallery;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;

import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.picdora.ImageUtil;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Image;
import com.picdora.ui.ActionSpinner;
import com.picdora.ui.grid.GridSize;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragment;

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
@EFragment(R.layout.fragment_basic_grid)
public abstract class GalleryFragment extends SelectionFragment {

	/** The spinner in the actionbar for selecting image size */
	private ActionSpinner mActionSizeSpinner;

	@AfterViews
	protected void initGalleryFragment() {
		/* Don't show any text, just the image */
		setShowText(false);
	}

	@Override
	protected void onCreateSelectionMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.fragment_gallery_cab, menu);
		super.onCreateSelectionMenu(inflater, menu);
	}

	@Override
	protected boolean onSelectionAction(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.download:
			downloadSelection();
			return true;
		case R.id.share:
			shareSelection();
			return true;
		default:
			return super.onSelectionAction(item);
		}
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
	 * Open a share intent dialog to share the selected items.
	 * 
	 */
	protected void shareSelection() {
		ImageUtil.shareImages(getActivity(), getSelectedImages());
	}

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
				getActivity(), R.layout.action_spinner_view_dropdown,
				GridSize.values());

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
		mActionSizeSpinner.setSelection(getGridSize().ordinal());
	}

	@Override
	protected void onClick(Selectable item) {
		FullscreenFragment frag = new FullscreenFragment_();
		frag.setImage((Image) item);

		FragmentManager fm = getActivity().getSupportFragmentManager();
		frag.show(fm, "fragment_fullscreen");
	}
	
	@Override
	protected void onFragmentTouch(){
		mActionSizeSpinner.collapse();
	}

	@SuppressWarnings("unchecked")
	protected List<Image> getSelectedImages() {
		return (List<Image>) (List<?>) getSelection();
	}

}
