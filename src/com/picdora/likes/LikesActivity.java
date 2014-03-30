package com.picdora.likes;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;

import android.view.Menu;

import com.picdora.ChannelUtils;
import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.models.Channel;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_likes)
public class LikesActivity extends PicdoraActivity {
	@FragmentById
	protected LikesFragment likesFragment;

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		List<Channel> channels = ChannelUtils.getAllChannels(true);
		likesFragment.setChannels(channels);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.likes, menu);
		return true;
	}

}
