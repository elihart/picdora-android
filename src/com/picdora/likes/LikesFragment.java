package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

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
import com.picdora.ui.grid.ImageGridFragment;

@EFragment(R.layout.fragment_image_grid)
public class LikesFragment extends ImageGridFragment {
	private List<Channel> mChannels;
	/** ActionMode for showing contextual options for selected images */
	private ActionMode mActionMode;

	/**
	 * Use the given channels to source the liked images for display. Use
	 * {@link #setChannel(Channel)} to shown just one channel.
	 * 
	 * @param channels
	 */
	public void setChannels(List<Channel> channels) {
		mChannels = channels;
		refreshImageList();
	}

	/**
	 * Display only liked images from this channel. Use
	 * {@link #setChannels(List)} to shown just multiple channels.
	 * 
	 * @param channel
	 */
	public void setChannel(Channel channel) {
		mChannels = new ArrayList<Channel>();
		mChannels.add(channel);
		refreshImageList();
	}

	/**
	 * Refresh the list of liked images from the set channels. This will
	 * retrieve the current list from the database and repopulate the grid.
	 * 
	 */
	@Background
	public void refreshImageList() {
		showProgress();
		List<Image> images = ChannelUtils.getLikedImages(mChannels);
		// update the display on the ui thread
		handleRefreshResult(images);
	}

	/**
	 * Need to use the ui thread to display the refreshed images
	 * 
	 * @param images
	 */
	@UiThread
	protected void handleRefreshResult(List<Image> images) {
		setImagesToShow(images);
		if (images.isEmpty()) {
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
			// mActionMode.setCustomView(arg0);
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
