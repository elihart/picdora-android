package com.picdora.collections;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.widget.FrameLayout;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.likes.LikesFragment;
import com.picdora.likes.LikesFragment_;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_collections)
public class CollectionsActivity extends PicdoraActivity {
	@ViewById(R.id.fragment_container)
	protected FrameLayout mFragmentContainer;
	
	private static final String LIKES_FRAGMENT_TAG = "CollectionSelectionFrag";
	private CollectionSelectionFragment mSelectionFrag;
	private CollectionFragment mDetailFrag;
	
	

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		
		/*
		 * The fragment is set to retain state so we don't have to recreate it
		 * on config changes.
		 */
		FragmentManager fm = getSupportFragmentManager();
		mSelectionFrag = (CollectionSelectionFragment) fm
				.findFragmentByTag(LIKES_FRAGMENT_TAG);
		
		/* Create the fragment and add it if it doesn't yet exist */
		if (mSelectionFrag == null) {
			mSelectionFrag = new CollectionSelectionFragment_();
			fm.beginTransaction()
					.add(R.id.fragment_container, mSelectionFrag,
							LIKES_FRAGMENT_TAG).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.collections, menu);
		return true;
	}

	/**
	 * Swap out the current fragment with the given one.
	 * 
	 * @param newFrag
	 */
	public void showFragment(Fragment newFrag) {
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.fragment_container, newFrag);
		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();

	}

}
