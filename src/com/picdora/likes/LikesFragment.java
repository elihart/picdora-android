package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;

import com.picdora.ChannelUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.Image;

@EFragment(R.layout.fragment_image_grid)
public class LikesFragment extends ImageGridFragment {
	private List<Channel> mChannels;

	/**
	 * Use the given channels to source the liked images for display.
	 * 
	 * @param channels
	 */
	public void setChannels(List<Channel> channels) {
		mChannels = channels;
		refreshImageList();
	}

	/**
	 * Display only liked images from this channel
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
		Util.log("count " + images.size());
		setImagesToShow(images);
		if (images.isEmpty()) {
			showMessage("You haven't liked any images yet!");
		} else {
			showImageGrid();
		}
	}

	@Override
	protected void onSelectionChanged(List<Image> selectedImages) {
		Util.log("Selection: " + selectedImages.toString());
	}

	@Override
	protected void onImageClick(Image image) {
		Util.log("click");
	}

}
