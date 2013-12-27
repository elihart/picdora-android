package com.picdora.ui;

/** 
 * A basic class for holding the drawable resource and the label text
 * for an entry in the sliding menu
 *
 */
public class SlidingMenuItem {
	private int mIconResource;
	private String mLabel;
	// The class of the activity that should be started when this item is clicked
	private Class mActivityToStart;
	
	public SlidingMenuItem(int iconResource, String label, Class activityToStart) {
		super();
		this.mIconResource = iconResource;
		this.mLabel = label;
		mActivityToStart = activityToStart;
	}
	
	public int getIcon(){
		return mIconResource;
	}
	
	public String getLabel(){
		return mLabel;
	}

	/**
	 * Get the activity that should be started when this item is clicked
	 * @return The Class of the activity to start
	 */
	public Class getActivityToStart() {
		return mActivityToStart;
	}
	

}
