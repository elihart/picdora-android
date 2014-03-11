package com.picdora.channelSelection;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.picdora.ChannelHelper;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.channelCreation.ChannelCreationActivity_;
import com.picdora.channelSelection.ChannelGridFragment.OnChannelClickListener;
import com.picdora.models.Channel;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_channel_selection)
public class ChannelSelectionActivity extends PicdoraActivity {
	// TODO: Have one main menu activity and have fragments for the menu options

	@FragmentById
	ChannelGridFragment channelFragment;
	@Pref
	PicdoraPreferences_ prefs;

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		channelFragment.setOnChannelClickListener(new OnChannelClickListener() {

			@Override
			public void onChannelClick(Channel channel) {
				channelSelected(channel);
			}
		});
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

	public void channelSelected(Channel channel) {
		ChannelHelper.playChannel(channel, true, this);
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

}
