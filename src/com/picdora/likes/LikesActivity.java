package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.picdora.ChannelUtils;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_likes)
public class LikesActivity extends PicdoraActivity {
	@FragmentById
	protected LikesFragment likesFragment;

	@Pref
	protected PicdoraPreferences_ mPrefs;

	/** Spinner to select channel */
	private Spinner mChannelSpinner;
	private ChannelSelectArrayAdapter mSpinnerAdapter;
	private List<Channel> mChannels;

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// List<Channel> channels = ChannelUtils.getAllChannels(true);
		// likesFragment.setChannels(channels);
		likesFragment.showProgress();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.likes, menu);

		MenuItem spinnerItem = menu.findItem(R.id.channel_spinner);
		mChannelSpinner = (Spinner) MenuItemCompat.getActionView(spinnerItem);
		initChannels();

		return true;
	}

	/**
	 * Retrieve all channels from db asynchronously. Then init the spinner with
	 * the channels.
	 * 
	 * @param spinner
	 */
	@Background
	protected void initChannels() {
		// get all channels from db.
		mChannels = ChannelUtils.getAllChannels(mPrefs.showNsfw().get());

		initSpinner();
	}

	/**
	 * Init and populate the Channel Select spinner.
	 */
	@UiThread
	protected void initSpinner() {
		/*
		 * We need an option for "All Channels". Couple ways to do this, but for
		 * now let's use a dummy Channel at index 0 with it's name being the
		 * text we want to display. We have to remember to treat position 1
		 * differently on item select though. Thoughts on how to do this less
		 * hackily?
		 */
		List<Channel> channelsToShow = new ArrayList<Channel>(mChannels);
		// dummy Channel. All we care about is the name.
		Channel allChannelsDummy = new Channel(getResources().getString(
				R.string.likes_channel_spinner_value_all),
				new ArrayList<Category>(), GifSetting.ALLOWED);
		// insert dummy at top of list.
		channelsToShow.add(0, allChannelsDummy);

		mSpinnerAdapter = new ChannelSelectArrayAdapter(this,
				R.layout.channel_spinner_item, channelsToShow);

		mChannelSpinner.setAdapter(mSpinnerAdapter);

		mChannelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				/* If position is 0 then it is our All Channels dummy */
				if (position == 0) {
					likesFragment.setChannels(mChannels);
				}
				// normal channel
				else {
					likesFragment.setChannel(mSpinnerAdapter.getItem(position));
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				/* Default to all channels selected */
				mChannelSpinner.setSelection(0);
			}
		});
		
		/* Start with all channels selected */
		mChannelSpinner.setSelection(0);

		// Pass all channels to fragment to load all by default
		//likesFragment.setChannels(mChannels);
	}

}
