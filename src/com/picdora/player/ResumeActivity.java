package com.picdora.player;

import org.androidannotations.annotations.EActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.picdora.ChannelHelper;
import com.picdora.R;

@EActivity
public class ResumeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// TODO: Implement resume
		ChannelHelper.resumeCachedPlayed(this);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.resume, menu);
		return true;
	}

}
