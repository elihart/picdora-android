package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EFragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.ChannelUtils;
import com.picdora.R;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.gallery.DbGalleryFragment;

/**
 * A special case of the {@link #GalleryFragment} where Liked images from
 * Channels are shown. The client has set one Channel or a list of Channels to
 * use and Liked images will be drawn from there to display.
 * <p>
 * On selection the user will have the option to remove the images from the
 * liked list, add them to a collection, or download them.
 */
@EFragment(R.layout.fragment_basic_grid)
public class LikesFragment extends DbGalleryFragment {
	private List<Channel> mChannels;

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
		 * bother changing anything. Otherwise do a fresh load to update the
		 * images for the new channels.
		 */
		if (!channels.equals(mChannels)){
			mChannels = channels;
			loadImagesFromDb();
		} 
		/* Make sure we are showing the images */
		else {
			showImages();
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
		// TODO

	}

	@Override
	protected void onSelectionDeleted(List<Image> deletedImages) {
		/* Remove the given images from the currently selected channels */
		ChannelUtils.deleteLikes(mChannels, deletedImages);
	}

	@Override
	protected List<Image> getImagesFromDb() {
		return ChannelUtils.getLikedImages(mChannels);
	}

	@Override
	protected String getEmptyMessage() {
		String msg = "You haven't liked any images yet!";
		if (mChannels.size() == 1) {
			msg = "You haven't liked any images in the channel "
					+ mChannels.get(0).getName();
		}

		return msg;
	}
}
