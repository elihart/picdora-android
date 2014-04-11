package com.picdora.channelSelection;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.widget.FrameLayout;

import com.picdora.PicdoraActivity;
import com.picdora.R;
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
public class ChannelSelectionActivity extends PicdoraActivity {
	// TODO: Have one main menu activity and have fragments for the menu options

	@ViewById(R.id.fragment_container)
	protected FrameLayout mFragmentContainer;

	private static final String SELECTION_FRAGMENT_TAG = "ChannelSelectionFrag";

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		/*
		 * The fragment is set to retain state so we don't have to recreate it
		 * on config changes.
		 */
		final FragmentManager fm = getSupportFragmentManager();
		ChannelSelectionFragment frag = (ChannelSelectionFragment) fm
				.findFragmentByTag(SELECTION_FRAGMENT_TAG);

		/* Create the fragment and add it if it doesn't yet exist */
		if (frag == null) {
			frag = new ChannelSelectionFragment_();
			fm.beginTransaction()
					.add(R.id.fragment_container, frag, SELECTION_FRAGMENT_TAG)
					.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_selection, menu);
		return true;
	}
}
