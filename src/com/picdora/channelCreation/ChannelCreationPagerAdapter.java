package com.picdora.channelCreation;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

/**
 * Page class for showing our info and category fragments.
 * 
 */
public class ChannelCreationPagerAdapter extends FragmentPagerAdapter {
	Fragment[] frags = { new ChannelInfoFragment_(),
			new CategorySelectFragment_() };

	SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

	public ChannelCreationPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		return frags[position];
	}

	@Override
	public int getCount() {
		return frags.length;
	}

	/*
	 * Strategy for accessing fragments in the pager. Instantiate item is called
	 * every time the fragment is loaded. From http://stackoverflow.com
	 * /questions/8785221/retrieve-a-fragment-from-a-viewpager
	 */

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Fragment fragment = (Fragment) super.instantiateItem(container,
				position);
		registeredFragments.put(position, fragment);
		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		registeredFragments.remove(position);
		super.destroyItem(container, position, object);
	}

	/**
	 * Get the fragment shown at the given position.
	 * 
	 * @param position
	 * @return
	 */
	public Fragment getRegisteredFragment(int position) {
		return registeredFragments.get(position);
	}

	/**
	 * Get the position of the category select fragment in the pager.
	 * 
	 * @return
	 */
	public int getCategoryFragmentPosition() {
		return 1;
	}

	/**
	 * Get the category select fragment in the pager.
	 * 
	 * @return The fragment, or null if it hasn't been instantiated yet.
	 */
	public CategorySelectFragment getCategoryFragment() {
		return (CategorySelectFragment) frags[getCategoryFragmentPosition()];
	}

	/**
	 * Get the position of the channel info fragment in the pager.
	 * 
	 * @return
	 */
	public int getInfoFragmentPosition() {
		return 0;
	}

	/**
	 * Get the info fragment in the pager.
	 * 
	 * @return The fragment, or null if it hasn't been instantiated yet.
	 */
	public ChannelInfoFragment getInfoFragment() {
		return (ChannelInfoFragment) frags[getInfoFragmentPosition()];
	}
}