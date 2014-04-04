package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.ChannelUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.gallery.GalleryFragment;

/**
 * A special case of the {@link #GalleryFragment} where Liked images from
 * Channels are shown. The client has set one Channel or a list of Channels to
 * use and Liked images will be drawn from there to display.
 * <p>
 * On selection the user will have the option to remove the images from the
 * liked list, add them to a collection, or download them.
 */
@EFragment(R.layout.fragment_image_grid)
public class LikesFragment extends GalleryFragment {
	private List<Channel> mChannels;

	/** Whether we currently have a background task going to get new images */
	private volatile boolean mImageRefreshInProgress = false;

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
			 * second will start in parallel which could be bad.
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
			/*
			 * Show a message about not having any likes images. If there are
			 * multiple channels selected then show a generic message, but if a
			 * certain channel is selected then be specific
			 */
			String msg = "You haven't liked any images yet!";
			if (mChannels.size() == 1) {
				msg = "You haven't liked any images in the channel "
						+ mChannels.get(0).getName();
			}

			showMessage(msg);
		} else {
			showImageGrid();
		}
	}

	@Override
	protected void onCreateSelectionMenu(MenuInflater inflater, Menu menu) {
		super.onCreateSelectionMenu(inflater, menu);
		inflater.inflate(R.menu.fragment_likes_cab, menu);
	}

	@Override
	protected boolean onSelectionAction(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.star:
			addToCollection();
			return true;
		}

		return false;
	}

	/**
	 * Show a dialog allowing the user to add the currently selected images to a
	 * collection.
	 * 
	 */
	private void addToCollection() {
		Util.log("collect");
		// TODO Auto-generated method stub

	}

	@Override
	protected void onImageClick(Image image) {
		Util.log("click");
	}
}
