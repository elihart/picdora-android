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
import android.widget.ImageButton;

import com.picdora.ChannelUtils;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.channelCreation.ChannelCreationActivity_;
import com.picdora.channelSelection.ChannelGridFragment.OnChannelClickListener;
import com.picdora.models.Channel;
import com.picdora.ui.SlidingMenuHelper;

/**
 * Provide a screen where the user can see all of their channels and access
 * them. The nsfw channels are not shown if the nsfw setting is turned off. The
 * channels are shown in a grid, and clicking on one presents a menu to either
 * play the channel or go to the channel detail page.
 * 
 * @author eli
 * 
 */
@EActivity(R.layout.activity_channel_selection)
public class ChannelSelectionActivity extends PicdoraActivity implements
		OnItemClickListener, OnChannelClickListener {
	// TODO: Have one main menu activity and have fragments for the menu options

	// Use a fragment to display the channels
	@FragmentById
	protected ChannelGridFragment channelFragment;
	@Pref
	protected PicdoraPreferences_ prefs;

	protected Activity mActivity;

	// Keep track of the last channel/grid item that was clicked
	private Channel mSelectedChannel;
	private ChannelSelectionGridItem mSelectedView;

	@AfterViews
	void initViews() {
		mActivity = this;

		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// listeners to get the clicked channel and view item. On an grid item
		// click the channel will first call the channel listener before calling
		// the item listener so we know which channel to associate with the item
		channelFragment.setOnItemClickListener(this);
		channelFragment.setOnChannelClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		SlidingMenuHelper.redrawMenu(this);

		refreshChannels();
	}

	/**
	 * Get a fresh list of channels from the database and refresh the grid
	 */
	private void refreshChannels() {
		if (channelFragment != null) {
			// TODO: Filter out nsfw by preference
			List<Channel> channels = ChannelUtils.getAllChannels(prefs
					.showNsfw().get());
			// TODO: Allow more sorting options
			ChannelUtils.sortChannelsAlphabetically(channels);
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

	/**
	 * The fragment will alert us when a channel is selected. This will be called before the onItemClick listener
	 */
	@Override
	public void onChannelClicked(Channel channel) {
		mSelectedChannel = channel;
	}

	/**
	 * We are overriding the default channel grid to add a button menu on top of
	 * each item when it is clicked. We need to get the clicked view and enable
	 * the buttons
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {


		// disable buttons on the previously selected item
		if (mSelectedView != null) {
			mSelectedView.showButtons(false);
		}

		mSelectedView = (ChannelSelectionGridItem) view;
		mSelectedView.showButtons(true);
		
		// on double click act like the play button was pressed
		mSelectedView.setOnClickListener(doubleClickListener);

		// set up listeners for the buttons
		mSelectedView.findViewById(R.id.playButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						ChannelUtils.playChannel(mSelectedChannel,	mActivity, true);
					}
				});

		mSelectedView.findViewById(R.id.settingsButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						ChannelUtils.showChannelDetail(mSelectedChannel,
								mActivity);
					}
				});
	}
	
	// on double click act like the play button was pressed
	private OnClickListener doubleClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			ImageButton playButton = (ImageButton) mSelectedView.findViewById(R.id.playButton);
			playButton.setPressed(true);
			playButton.performClick();
		}
	};

}
