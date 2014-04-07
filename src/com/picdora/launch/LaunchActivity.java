package com.picdora.launch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.crashlytics.android.Crashlytics;
import com.picdora.PicdoraApp;
import com.picdora.R;
import com.picdora.channelSelection.ChannelSelectionActivity_;

public class LaunchActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_launch);

		// TODO: Launch splash screen

		// only use crashlytics when not debugging
		if (!PicdoraApp.DEBUG) {
			Crashlytics.start(this);
		}
		startActivity(new Intent(this, ChannelSelectionActivity_.class));
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.launch, menu);
		return true;
	}

}
