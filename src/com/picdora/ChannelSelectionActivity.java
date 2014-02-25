package com.picdora;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.picdora.models.Channel;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_channel_selection)
public class ChannelSelectionActivity extends PicdoraActivity {
	@ViewById
	ListView channelList;

	@Bean
	ChannelListAdapter adapter;

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// TODO: Load list in background and show loading icon
		channelList.setAdapter(adapter);

		// ImageManager.getCategoriesFromServer();
		// ImageManager.getImagesFromServer(50, 1);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		adapter.refresh();
	}

	@ItemClick
	void channelListItemClicked(Channel channel) {
		ChannelHelper.playChannel(channel, this);
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

	private void newChannel() {
		startActivity(new Intent(this, ChannelCreationActivity_.class));
	}

}
