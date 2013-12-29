package com.picdora.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.picdora.ChannelSelectionActivity;
import com.picdora.ChannelSelectionActivity_;
import com.picdora.PicdoraActivity;
import com.picdora.R;


public class SlidingMenuHelper {
	// TODO: Hide contextual action buttons on drawer show
	// TODO: Drawer tutorial
	// TODO: Highlight activity name when drawer opens in that activity
	// TODO: Show messages count next to Messages label

	/**
	 * Get a list of the items to place in the sliding menu
	 * 
	 * @return An arraylist of objects to add to the sliding menu
	 */
	private static ArrayList<SlidingMenuItem> getMenuEntries() {
		ArrayList<SlidingMenuItem> items = new ArrayList<SlidingMenuItem>();

		items.add(new SlidingMenuItem(R.drawable.ic_launcher, "Channels",
				ChannelSelectionActivity_.class));


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
	public static void addMenuToActivity(final PicdoraActivity activity,
			boolean showIcon) {
		final DrawerLayout drawerLayout = (DrawerLayout) activity
				.findViewById(R.id.drawer_layout);

		// make sure this activity actually has a drawer layout
		if (drawerLayout == null) {
			return;
		}

		final ListView drawerList = (ListView) activity
				.findViewById(R.id.sliding_menu_list);

		// Set the adapter for the list view
		drawerList.setAdapter(new SlidingMenuAdapter(activity,
				SlidingMenuHelper.getMenuEntries()));

		addClickListener(activity, drawerLayout, drawerList);
		addDrawerListener(activity, drawerLayout, showIcon);
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
				// the tag and the onClose listener will check for itr
				drawerLayout.setTag(new Intent(activity, item
						.getActivityToStart()));
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
		// interaction
		// between the action bar and drawer. To do this, the actionbar
		// needs a drawer ico
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(activity,
				drawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {
			private String title = "Lyricoo";

			/**
			 * Called when a drawer has settled in a completely closed state.
			 */
			public void onDrawerClosed(View view) {
				// change title back to activity name
				activity.getSupportActionBar().setTitle(title);

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
			public void onDrawerOpened(View drawerView) {
				// When the drawer is opened change the action bar text to
				// the app name. Remember the old title so we can change it back
				// when the drawer closes
				title = activity.getSupportActionBar().getTitle().toString();
				activity.getSupportActionBar().setTitle("Lyricoo");

				// Redraw action bar to hide contextual actions while drawer is
				// open
				activity.supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerSlide(View arg0, float arg1) {

			}

			@Override
			public void onDrawerStateChanged(int arg0) {

			}

		};

		toggle.setDrawerIndicatorEnabled(showIcon);

		drawerLayout.setDrawerListener(toggle);

		activity.setDrawerToggle(toggle);
	}
}