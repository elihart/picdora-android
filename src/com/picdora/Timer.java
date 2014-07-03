package com.picdora;

/**
 * Keeps track of how long methods take.
 * 
 */
public class Timer {
	private long mStartTime;
	private static final int NANOS_PER_MILLI = 1000000;
	
	/**
	 * Mark a start time for a timer for use in clocking method run time.
	 * 
	 */
	public void start(){
		mStartTime = System.nanoTime();
	}
	
	/**
	 * Print the time since the last lap/start of the timer.
	 * 
	 * @param msg
	 *            A message to include with the time.
	 */
	public void lap(String msg){
		long curr = System.nanoTime();
		long elapsed = curr - mStartTime;
		long millis = elapsed / NANOS_PER_MILLI;
		long nanos = curr % NANOS_PER_MILLI;
		
		Util.log(String.format("%s : %d ms %d ns", msg, millis, nanos));
		mStartTime = curr;
	}
	

}
