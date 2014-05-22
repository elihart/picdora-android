package com.picdora;

/**
 * Keeps track of how long methods take.
 * 
 */
public class Timer {
	private long mStartTime;
	
	/**
	 * Mark a start time for a timer for use in clocking method run time.
	 * 
	 */
	public void start(){
		mStartTime = System.currentTimeMillis();
	}
	
	/**
	 * Print the time since the last lap/start of the timer.
	 * 
	 * @param msg
	 *            A message to include with the time.
	 */
	public void lap(String msg){
		long curr = System.currentTimeMillis();
		Util.log(msg + " : " + (curr - mStartTime) + " ms");
		mStartTime = curr;
	}
	

}
