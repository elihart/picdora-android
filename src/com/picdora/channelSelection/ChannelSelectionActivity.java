package com.picdora.channelSelection;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.picdora.ChannelHelper;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.channelCreation.ChannelCreationActivity_;
import com.picdora.channelSelection.ChannelGridFragment.OnChannelClickListener;
import com.picdora.models.Channel;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_channel_selection)
public class ChannelSelectionActivity extends PicdoraActivity implements
		OnItemClickListener, OnChannelClickListener {
	// TODO: Have one main menu activity and have fragments for the menu options

	@FragmentById
	ChannelGridFragment channelFragment;
	@Pref
	PicdoraPreferences_ prefs;
	protected Activity mActivity;

	private Channel mSelectedChannel;
	private ChannelSelectionGridItem mSelectedView;

	@AfterViews
	void initViews() {
		mActivity = this;

		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		channelFragment.setOnItemClickListener(this);

		channelFragment.setOnChannelClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		SlidingMenuHelper.refreshList(this);

		refreshChannels();
	}

	/**
	 * Get a fresh list of channels from the database and refresh the grid
	 */
	private void refreshChannels() {
		if (channelFragment != null) {
			// TODO: Filter out nsfw by preference
			List<Channel> channels = ChannelHelper.getAllChannels(prefs
					.showNsfw().get());
			// TODO: Allow more sorting options
			ChannelHelper.sortChannelsAlphabetically(channels);
			channelFragment.setChannels(channels);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_selection, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_new_channel:
			newChannel();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Click
	protected void createChannelButtonClicked() {
		newChannel();
	}

	private void newChannel() {
		startActivity(new Intent(this, ChannelCreationActivity_.class));
	}

	@Override
	public void onChannelClicked(Channel channel) {
		mSelectedChannel = channel;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (mSelectedView != null) {
			mSelectedView.showButtons(false);
		}

		mSelectedView = (ChannelSelectionGridItem) view;
		mSelectedView.showButtons(true);

		// set up listeners for the buttons
		mSelectedView.findViewById(R.id.playButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						ChannelHelper.playChannel(mSelectedChannel, true,
								mActivity);
					}
				});

		mSelectedView.findViewById(R.id.settingsButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						ChannelHelper.showChannelDetail(mSelectedChannel,
								mActivity);
					}
				});

	}

}
