package com.picdora;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ViewById;
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
		
        channelList.setAdapter(adapter);
    }
	
	@ItemClick
    void channelListItemClicked(Channel channel) {
        Intent intent = new Intent(this, ChannelViewActivity_.class);
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
