package com.picdora.favorites;

import org.androidannotations.annotations.EActivity;

import com.picdora.R;
import com.picdora.R.layout;
import com.picdora.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

@EActivity
public class FavoritesActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorites);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.favorites, menu);
		return true;
	}

}
