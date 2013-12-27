package com.picdora;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class ChannelCreationActivity extends PicdoraActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channel_creation);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_creation, menu);
		return true;
	}

}
