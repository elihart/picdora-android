package com.picdora.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.channelSelection.ChannelSelectionActivity_;
import com.picdora.favorites.FavoritesActivity_;
import com.picdora.likes.LikesActivity_;
import com.picdora.player.ChannelViewActivity;
import com.picdora.player.ResumeActivity_;
import com.picdora.settings.SettingsActivity;


public class SlidingMenuHelper {
	// TODO: Hide contextual action buttons on drawer show
	// TODO: Drawer tutorial
	// TODO: Highlight activity name when drawer opens in that activity

	/**
	 * Get a list of the items to place in the sliding menu
	 * 
	 * @return An arraylist of objects to add to the sliding menu
	 */
	private static ArrayList<SlidingMenuItem> getMenuEntries() {
		ArrayList<SlidingMenuItem> items = new ArrayList<SlidingMenuItem>();

		// TODO: Resume doesn't get updated on activity resume
		if(ChannelViewActivity.hasCachedPlayer()){
		items.add(new SlidingMenuItem(R.drawable.ic_action_play_over_video, ChannelViewActivity.getCachedPlayerChannelName(),
				ResumeActivity_.class));
		}
		items.add(new SlidingMenuItem(R.drawable.ic_action_channels, "Channels",
				ChannelSelectionActivity_.class));
		items.add(new SlidingMenuItem(R.drawable.ic_action_like, "Likes",
				LikesActivity_.class));
		items.add(new SlidingMenuItem(R.drawable.ic_action_favorite, "Favorites",
				FavoritesActivity_.class));
		items.add(new SlidingMenuItem(R.drawable.ic_action_settings, "Settings",
				SettingsActivity.class));


		return items;
	}

	/**
	 * Adds a fully customized sliding menu to the activity. The activity xml
	 * must be setup for a drawerlayout, otherwise nothing is done.
	 * 
	 * @param activity
	 *            The activity to add the menu to
	 * @param showIcon
	 *            Whether or not to show the drawer icon in the action bar
	 */
	public static DrawerLayout addMenuToActivity(
			final PicdoraActivity activity, boolean showIcon) {
		final DrawerLayout drawerLayout = (DrawerLayout) activity
				.findViewById(R.id.drawer_layout);

		// make sure this activity actually has a drawer layout
		if (drawerLayout == null) {
			return null;
		}

		final ListView drawerList = (ListView) activity
				.findViewById(R.id.sliding_menu_list);

		// Set the adapter for the list view
		drawerList.setAdapter(new SlidingMenuAdapter(activity,
				SlidingMenuHelper.getMenuEntries()));

		addClickListener(activity, drawerLayout, drawerList);
		addDrawerListener(activity, drawerLayout, showIcon);

		return drawerLayout;
	}

	private static void addClickListener(final PicdoraActivity activity,
			final DrawerLayout drawerLayout, final ListView drawerList) {
		// Add click listener to change activities when an item is clicked
		drawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				SlidingMenuItem item = (SlidingMenuItem) parent
						.getItemAtPosition(position);

				// close drawer before starting the activity so that the drawer
				// isn't open if they come back. The close animation isn't
				// instant, so we need to wait for onClose to get called in the
				// listener before we can switch activities. Set the intent in
				// the tag and the onClose listener will check for it
				Intent i = new Intent(activity, item.getActivityToStart());
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				drawerLayout.setTag(i);
				drawerLayout.closeDrawers();
			}
		});
	}

	/**
	 * Add drawer listener to listen for open and close events
	 * 
	 * @param activity
	 * @param drawerLayout
	 */
	private static void addDrawerListener(final PicdoraActivity activity,
			final DrawerLayout drawerLayout, boolean showIcon) {
		// interaction between the action bar and drawer. To do this, the
		// actionbar needs a drawer icon
		final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				activity, drawerLayout, R.drawable.ic_navigation_drawer,
				R.string.drawer_open, R.string.drawer_close) {
			// the title to show on the action bar when the drawer opens
			private String openTitle = "Lyricoo";

			/**
			 * Called when a drawer has settled in a completely closed state.
			 */
			@Override
			public void onDrawerClosed(View view) {
				// change openTitle back to activity name
				activity.setActionBarTitle(openTitle);

				// if the drawer was closed because an option was selected,
				// start that activity
				Intent intent = (Intent) drawerLayout.getTag();
				if (intent != null) {
					drawerLayout.setTag(null);
					activity.startActivity(intent);
				} else {
					// Redraw action bar icons now that drawer is closed
					activity.supportInvalidateOptionsMenu();
				}
			}

			/** Called when a drawer has settled in a completely open state. */
			@Override
			public void onDrawerOpened(View drawerView) {
				// When the drawer is opened change the action bar text to
				// the app name. Remember the old openTitle so we can change it back
				// when the drawer closes
				openTitle = activity.getSupportActionBar().getTitle().toString();
				activity.setActionBarTitle("Picdora");

				// Redraw action bar to hide contextual actions while drawer is
				// open
				activity.supportInvalidateOptionsMenu();
			}
		};

		toggle.setDrawerIndicatorEnabled(showIcon);

		drawerLayout.setDrawerListener(toggle);
		activity.setDrawerToggle(toggle);
	}
}
