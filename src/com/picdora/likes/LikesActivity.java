package com.picdora.likes;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RelativeLayout;

import com.picdora.ChannelUtil;
import com.picdora.PicdoraActivity;
import com.picdora.PicdoraPreferences_;
import com.picdora.R;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.ActionSpinner;
import com.picdora.ui.SlidingMenuHelper;
import com.picdora.ui.UiUtil;

/**
 * Displays "Liked" images in a gallery style page. A spinner in the action bar
 * allows the user to filter images by what channel they were liked in, or show
 * images from all channels (the default).
 */
@EActivity(R.layout.activity_likes)
public class LikesActivity extends PicdoraActivity {
	/** Tag to associate likes fragment with in fragment manager */
	private static final String LIKES_FRAGMENT_TAG = "likesFragment";
	protected LikesFragment mLikesFragment;

	@Pref
	protected PicdoraPreferences_ mPrefs;

	@ViewById(R.id.fragment_container)
	protected RelativeLayout mRootView;

	private ActionSpinner mActionChannelSpinner;

	private List<Channel> mChannels;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * The fragment is set to retain state so we don't have to recreate it
		 * on config changes.
		 */
		FragmentManager fm = getSupportFragmentManager();
		mLikesFragment = (LikesFragment) fm
				.findFragmentByTag(LIKES_FRAGMENT_TAG);
		/* Create the fragment and add it if it doesn't yet exist */
		if (mLikesFragment == null) {
			mLikesFragment = new LikesFragment_();
			fm.beginTransaction()
					.add(R.id.fragment_container, mLikesFragment,
							LIKES_FRAGMENT_TAG).commit();
		}

		/* Restore instance state if available */
		SavedState state = (SavedState) getRetainedState();
		if (state != null) {
			mChannels = state.channels;
		}
	}

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		/* Collapse actionviews if there is a touch outside of the actionbar */
		if (UiUtil.isEventInsideView(ev, mRootView)) {
			collapseActionViews();
		}

		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.likes, menu);

		/* Get the channel spinner */
		mActionChannelSpinner = (ActionSpinner) MenuItemCompat
				.getActionView(menu.findItem(R.id.channel_spinner));

		/*
		 * We couldn't initialize the spinner until we had a handle on it, now
		 * we can go ahead and get the channels to populate it with. If we
		 * already have the channels from a restored state then we can skip to
		 * initing the spinner
		 */
		if (mChannels == null || mChannels.isEmpty()) {
			initChannels();
		} else {
			initChannelSpinner();
		}

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
		mChannels = ChannelUtil.getAllChannels(mPrefs.showNsfw().get());

		/*
		 * If the activity was destroyed while we were getting those then don't
		 * continue, otherwise init the spinner with the channels we got
		 */
		if (!isDestroyedCompat()) {
			initChannelSpinner();
		}
	}

	/**
	 * Init and populate the Channel Select spinner.
	 */
	@UiThread
	protected void initChannelSpinner() {
		/*
		 * We need an option for "All Channels". Couple ways to do this, but for
		 * now let's use a dummy Channel at index 0 with it's name being the
		 * text we want to display. We have to remember to treat position 0
		 * differently on item select though. Thoughts on how to do this less
		 * hackily?
		 */
		List<Channel> channelsToShow = new ArrayList<Channel>(mChannels);
		// dummy Channel. All we care about is the name.
		List<Category> dummyCategories = new ArrayList<Category>();
		dummyCategories.add(new Category());
		Channel allChannelsDummy = new Channel(getResources().getString(
				R.string.likes_channel_spinner_value_all), GifSetting.ALLOWED, dummyCategories);
		// insert dummy at top of list.
		channelsToShow.add(0, allChannelsDummy);

		final ChannelSelectArrayAdapter adapter = new ChannelSelectArrayAdapter(
				this, R.layout.action_spinner_view_dropdown, channelsToShow);

		mActionChannelSpinner.setAdapter(adapter);

		mActionChannelSpinner
				.setSelectionListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						/* If position is 0 then it is our All Channels dummy */
						if (position == 0) {
							mLikesFragment.setChannels(mChannels);
						}
						// normal channel
						else {
							mLikesFragment.setChannel(adapter.getItem(position));
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		/*
		 * If we have a saved spinner position then restore that, otherwise
		 * default to "all". Ideally we would only have to set the selection on
		 * the spinner and it's callback would set the channels, but we can't
		 * rely on that for the case when the spinner isn't shown, otherwise if
		 * the spinner is hidden in the beginning then the fragment won't be
		 * told what to load.
		 */
		SavedState state = (SavedState) getRetainedState();
		if (state != null && state.spinnerPos != 0) {
			mActionChannelSpinner.setSelection(state.spinnerPos);
			mLikesFragment.setChannel(adapter.getItem(state.spinnerPos));
		} else {
			// Pass all channels to fragment to load all by default
			mActionChannelSpinner.setSelection(0);
			mLikesFragment.setChannels(mChannels);
		}
	}

	@Override
	protected Object onRetainState() {
		/*
		 * Get the currently selected position, or default to 0 if nothing is
		 * selected
		 */
		int pos = mActionChannelSpinner.getSelection();
		if (pos == AdapterView.INVALID_POSITION) {
			pos = 0;
		}

		return new SavedState(mChannels, pos);
	}

	/**
	 * Helper class to save our activity state with {@link #onRetainState()}.
	 */
	private class SavedState {
		public List<Channel> channels;
		public int spinnerPos;

		public SavedState(List<Channel> channels, int spinnerPos) {
			super();
			this.channels = channels;
			this.spinnerPos = spinnerPos;
		}

	}

	/**
	 * Collapse all expanded action views in the activity and all fragments.
	 * 
	 */
	public void collapseActionViews() {
		mActionChannelSpinner.collapse();
	}

}
