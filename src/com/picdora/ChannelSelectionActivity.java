package com.picdora;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ViewById;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_channel_selection)
public class ChannelSelectionActivity extends PicdoraActivity {
	@ViewById
    ListView channelList;

    @Bean
    ChannelListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channel_selection);
		
		SlidingMenuHelper.addMenuToActivity(this, true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		
	}
	
	@AfterViews
    void bindAdapter() {
        channelList.setAdapter(adapter);
    }
	
	@ItemClick
    void channelListItemClicked(Channel channel) {
        Intent intent = new Intent(this, ChannelViewActivity.class);
        intent.putExtra("channel", Util.toJson(channel));
        startActivity(intent);
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
	
	private void newChannel(){
		startActivity(new Intent(this, ChannelCreationActivity.class));
	}

}
