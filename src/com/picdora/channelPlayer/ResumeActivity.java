package com.picdora.channelPlayer;

import org.androidannotations.annotations.EActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.picdora.R;

@EActivity
public class ResumeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* TODO: Store the last viewed channel somewhere can load it from here */
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.resume, menu);
		return true;
	}

}
