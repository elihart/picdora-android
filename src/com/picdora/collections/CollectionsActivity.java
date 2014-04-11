package com.picdora.collections;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.widget.FrameLayout;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_collections)
public class CollectionsActivity extends PicdoraActivity {
	@ViewById(R.id.fragment_container)
	protected FrameLayout mFragmentContainer;

	private static final String SELECTION_FRAGMENT_TAG = "CollectionSelectionFrag";
	private CollectionSelectionFragment mSelectionFrag;

	private static final String DETAIL_FRAGMENT_TAG = "CollectionDetailFrag";
	private CollectionFragment mDetailFrag;
	
	/* TODO: Sort collections in selection. */

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
		mSelectionFrag = (CollectionSelectionFragment) fm
				.findFragmentByTag(SELECTION_FRAGMENT_TAG);


		/* Create the fragment and add it if it doesn't yet exist */
		if (mSelectionFrag == null) {
			mSelectionFrag = new CollectionSelectionFragment_();
			fm.beginTransaction()
					.add(R.id.fragment_container, mSelectionFrag,
							SELECTION_FRAGMENT_TAG).commit();
		}

		fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {

			@Override
			public void onBackStackChanged() {
				/*
				 * If the back stack is empty then we are not showing a detail
				 * fragment so return to the main activity title.
				 */
				if (fm.getBackStackEntryCount() == 0) {
					setActionBarTitle(getResources().getString(
							R.string.title_activity_collections));
					mDetailFrag = null;
				}

			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.collections, menu);
		return true;
	}

	/**
	 * Swap out the current fragment with the given one and add the transaction
	 * to the backstack.
	 * 
	 * @param frag
	 *            The fragment to show.
	 * @param tag
	 *            The tag to associate the fragment with.
	 */
	public void showFragment(Fragment frag, String tag) {
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.fragment_container, frag, tag);
		transaction.addToBackStack(DETAIL_FRAGMENT_TAG);

		// Commit the transaction
		transaction.commit();

	}

	/**
	 * Set the fragment that should be show for Collection detail view.
	 * 
	 * @param frag
	 */
	public void showCollectionDetailFragment(CollectionFragment frag) {
		mDetailFrag = frag;
		showFragment(frag, DETAIL_FRAGMENT_TAG);
	}

}
