package com.picdora.likes;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import android.view.Menu;

import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_likes)
public class LikesActivity extends PicdoraActivity {

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.likes, menu);
		return true;
	}

}
