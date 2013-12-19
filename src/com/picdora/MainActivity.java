package com.picdora;

import net.frakbot.imageviewex.ImageViewEx;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;

public class MainActivity extends FragmentActivity {
	/**
	 * The pager widget, which handles animation and allows swiping horizontally
	 * to access previous and next wizard steps.
	 */
	private ViewPager mPager;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);
		
		this.deleteDatabase("picdora");
		
//		PicdoraDatabaseHelper dbHelper = new PicdoraDatabaseHelper(this);
//
//		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
//		ContentValues values = new ContentValues();
//		values.put("ImgurId", "asdf");
//		db.insert("images", null, values);
		
//		Cursor cursor = db.query("images",
//		        null, null, null, null, null, null);
//		
//		int count = cursor.getCount();
//		cursor.moveToLast();
//		String imgurId = cursor.getString(1);
//		Util.log("Rows : " + count + " last: " + imgurId);

		// Give the screen size so images are scaled to save memory
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;

		ImageViewEx.setScreenSize(width, height);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
		private ImageManager mImageManager;

		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
			mImageManager = new ImageManager();
		}

		@Override
		public Fragment getItem(int position) {
			ImageSwipeFragment frag = new ImageSwipeFragment();
			
			Image image = mImageManager.getImage(position);
			image.markView();

			Bundle args = new Bundle();
			args.putString("url", image.getUrl());
			frag.setArguments(args);

			return frag;
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}
	}

}
