package com.picdora.channelSelection;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.picdora.ChannelUtil;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.channelCreation.ChannelCreationActivity_;
import com.picdora.models.Channel;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragmentWithNew;

@SuppressWarnings("unchecked")
@EFragment(R.layout.fragment_selection_grid_with_new)
public class ChannelSelectionFragment extends SelectionFragmentWithNew {
	@Pref
	protected PicdoraPreferences_ mPrefs;
	@Bean
	protected ChannelUtil mChannelUtils;

	@Override
	protected void onCreateSelectionMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.fragment_channel_selection, menu);
		super.onCreateSelectionMenu(inflater, menu);
	}

	@Override
	protected void onSelectionChanged(List<Selectable> selection) {
		super.onSelectionChanged(selection);

		/*
		 * We need to modify the action mode menu depending on how many items
		 * are selected. If only one is selected then we can show the play and
		 * settings options, otherwise we need to hide them.
		 */

		if (selection.isEmpty()) {
			return;
		}

		Menu menu = getActionModeMenu();
		if (menu == null) {
			return;
		}

		MenuItem playItem = menu.findItem(R.id.action_play);
		MenuItem settingsItem = menu.findItem(R.id.action_settings);

		boolean isSingleChannelSelected = selection.size() == 1;

		if (playItem != null) {
			playItem.setVisible(isSingleChannelSelected);
		}

		if (settingsItem != null) {
			settingsItem.setVisible(isSingleChannelSelected);
		}
	}

	@Override
	protected boolean onSelectionAction(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_play:
			play();
			return true;
		case R.id.action_settings:
			showDetail();
			return true;
		default:
			return super.onSelectionAction(item);
		}
	}

	/**
	 * Play the currently selected channel.
	 * 
	 */
	private void play() {
		Channel c = getSelectedChannel();
		ChannelUtil.playChannel(c, getActivity(), true);
	}

	/**
	 * Get the selected channel. This should be called when only one channel is
	 * selected, otherwise it will only return the first in the list of many.
	 * 
	 * @return
	 */
	private Channel getSelectedChannel() {
		return (Channel) getSelection().get(0);
	}

	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		List<Channel> channels = new ArrayList<Channel>(
				(List<Channel>) (List<?>) selection);
		mChannelUtils.deleteChannels(channels);
	}

	@Override
	protected void onClick(Selectable item) {
		Channel channel = (Channel) item;
		ChannelUtil.playChannel(channel, getActivity(), true);
	}

	@Override
	protected List<Selectable> doItemLoad() {
		List<Channel> channels = ChannelUtil.getAllChannels(mPrefs.showNsfw()
				.get());
		// TODO: Allow more sorting options
		ChannelUtil.sortChannelsAlphabetically(channels);
		return (List<Selectable>) (List<?>) channels;
	}

	@Override
	protected String getEmptyMessage() {
		return getResources().getString(R.string.channel_selection_empty);

	}

	@Override
	protected String getCreateButtonText() {
		return getResources().getString(R.string.channel_create_button);
	}

	@Override
	protected void createNew() {
		startActivity(new Intent(getActivity(), ChannelCreationActivity_.class));
	}

	private void showDetail() {
		Channel channel = getSelectedChannel();
		ChannelUtil.showChannelDetail(channel, getActivity());
	}
}
