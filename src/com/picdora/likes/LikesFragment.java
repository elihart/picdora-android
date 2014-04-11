package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.ChannelUtil;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.collections.Collection;
import com.picdora.collections.CollectionUtil;
import com.picdora.collections.CollectionUtil.OnCollectionSelectedListener;
import com.picdora.models.Channel;
import com.picdora.models.Image;
import com.picdora.ui.gallery.GalleryFragment;
import com.picdora.ui.grid.Selectable;

/**
 * A special case of the {@link #GalleryFragment} where Liked images from
 * Channels are shown. The client has set one Channel or a list of Channels to
 * use and Liked images will be drawn from there to display.
 * <p>
 * On selection the user will have the option to remove the images from the
 * liked list, add them to a collection, or download them.
 */
@EFragment(R.layout.fragment_basic_grid)
public class LikesFragment extends GalleryFragment {
	private List<Channel> mChannels;

	@Bean
	protected CollectionUtil mCollectionUtils;

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
		if (!channels.equals(mChannels)) {
			mChannels = channels;
			refreshItemsAsync();
		}
		/* Make sure we are showing the images */
		else {
			showItems();
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
		default:
			return super.onSelectionAction(item);
		}

	}

	/**
	 * Show a dialog allowing the user to add the currently selected images to a
	 * collection.
	 * 
	 */
	private void addToCollection() {
		final List<Image> imagesToAdd = getSelectedImages();
		
		String title = getResources().getString(
				R.string.collections_add_to_collection);
		
		mCollectionUtils.showCollectionSelectionDialog(getActivity(), title,
				new OnCollectionSelectedListener() {

					@Override
					public void onCollectionSelected(Collection collection) {
						mCollectionUtils.addImages(collection, imagesToAdd);
						
						Util.makeBasicToast(getActivity(), "Selection added to the collection " + collection.getName());
					}
				});
	}

	@Override
	protected String getEmptyMessage() {
		return "You haven't liked any images yet!";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		/* Remove the given images from the currently selected channels */
		ChannelUtil.deleteLikes(mChannels, (List<Image>) (List<?>) selection);

	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Selectable> doItemLoad() {
		/*
		 * If no channels have been loaded yet then we can't load images. Return
		 * empty for now.
		 */
		if (mChannels == null || mChannels.isEmpty()) {
			return new ArrayList<Selectable>();
		} else {

			/* Load images from the db that belong to the selected channels. */
			return (List<Selectable>) (List<?>) ChannelUtil
					.getLikedImages(mChannels);
		}
	}
}
