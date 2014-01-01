package com.picdora;

import java.io.File;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.picdora.ImageManager.OnResultListener;
import com.picdora.ImageManager.OnServerResultListener;
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
		case R.id.action_refresh_images:
			syncImages();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	void syncImages() {
		File f = getDatabasePath("sprinkles.db");
		long dbSize = f.length();
		Util.log("db size " + dbSize);
		
		int batchSize = 1000;
		int start = 74000;
		int end = start + batchSize;
		doBatch(start, end, batchSize);
	}
	
	@Background(serial="saveImages")
	void saveImageJsonToDb(JSONArray json, int id){
		Util.log("Starting db save " + id);
		ImageManager.saveImagesToDb(json);
		Util.log("Finishing db save " + id);
	}

	void doBatch(final int start, final int end, final int batchSize) {
		ImageManager.getImagesFromServer(start, end, new OnServerResultListener() {
			
			@Override
			public void onSuccess(JSONArray json) {	
				Util.log("Images downloaded, sending to db " + end);
				saveImageJsonToDb(json, end);				
				doBatch(start + batchSize, end + batchSize, batchSize);				
			}
			
			@Override
			public void onFailure() {
				Util.log("batch failure " + end);
				
			}
		});
	}

	private void newChannel() {
		startActivity(new Intent(this, ChannelCreationActivity_.class));
	}

}
