package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.ChannelUtils;
import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.gallery.GalleryFragment;

@EFragment(R.layout.fragment_image_grid)
public class LikesFragment extends GalleryFragment {
	private List<Channel> mChannels;
	/** ActionMode for showing contextual options for selected images */
	private ActionMode mActionMode;

	/** Whether we currently have a background task going to get new images */
	private volatile boolean mImageRefreshInProgress = false;

	/**
	 * On a config change the action mode bar will not be recreated
	 * automatically so we need to recreate it manually.
	 * 
	 */
	@AfterViews
	protected void restoreActionMode() {
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
	 * Use the given channels to source the liked images for display. Use
	 * {@link #setChannel(Channel)} to shown just one channel.
	 * 
	 * @param channels
	 *            The channels to source liked images from. Can't be null.
	 */
	public void setChannels(List<Channel> channels) {
		if (channels == null) {
			throw new IllegalArgumentException("Channels can't be null");
		}

		/*
		 * If our existing channels are the same as the new ones then don't
		 * bother changing anything. However, if we don't have any images to
		 * show and no background refresh task is ongoing then let's try to
		 * refresh again.
		 */
		if (channels.equals(mChannels)
				&& (!isImagesEmpty() || mImageRefreshInProgress)) {
			showImages();
		} else {
			/*
			 * TODO: Case where image refresh is in progress for other channels
			 * and the user changes the channels during the load. Ideally this
			 * would instantly cancel the previous load, but right now the
			 * second will start in parallel. This is bad.
			 */
			mChannels = channels;
			refreshImageList();
		}
	}

	/**
	 * Display only liked images from this channel. Use
	 * {@link #setChannels(List)} to shown just multiple channels.
	 * 
	 * @param channel
	 */
	public void setChannel(Channel channel) {
		List<Channel> channels = new ArrayList<Channel>();
		channels.add(channel);
		setChannels(channels);
	}

	/**
	 * Refresh the list of liked images from the set channels. This will
	 * retrieve the current list from the database and repopulate the grid.
	 * 
	 */
	@Background
	public void refreshImageList() {
		mImageRefreshInProgress = true;
		showProgress();
		List<Image> images = ChannelUtils.getLikedImages(mChannels);
		/*
		 * Update the display on the ui thread if the fragment wasn't destroyed
		 * while we were getting images
		 */
		if (!isDestroyed()) {
			handleRefreshResult(images);
		}
		mImageRefreshInProgress = false;
	}

	/**
	 * Need to use the ui thread to display the refreshed images
	 * 
	 * @param images
	 */
	@UiThread
	protected void handleRefreshResult(List<Image> images) {
		setImagesToShow(images);
		showImages();
	}

	/**
	 * Show the image grid if we have images, but if our image list is empty
	 * then show a message instead.
	 * 
	 */
	protected void showImages() {
		if (mImageRefreshInProgress) {
			showProgress();
		} else if (isImagesEmpty()) {
			showMessage("You haven't liked any images yet!");
		} else {
			showImageGrid();
		}
	}

	@Override
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
	}

	@Override
	protected void onImageClick(Image image) {
		Util.log("click");
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.likes_contextual, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.download:
				Util.log("download");
				return true;
			case R.id.delete:
				Util.log("delete");
				return true;
			case R.id.star:
				Util.log("star");
				return true;
			default:
				return false;
			}
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

}
