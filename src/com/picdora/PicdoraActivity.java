package com.picdora;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.Configuration;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class PicdoraActivity extends ActionBarActivity {
	// we need to be able to access the drawer toggle at certain points in the
		// activity's life
		protected ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picdora);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.picdora, menu);
		return true;
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mDrawerToggle != null){
        	mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    public void setDrawerToggle(ActionBarDrawerToggle toggle){
		mDrawerToggle = toggle;
	}

}
