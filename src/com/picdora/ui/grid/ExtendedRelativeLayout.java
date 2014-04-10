package com.picdora.ui.grid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A relative layout that provides a listener for when it is handed a touch
 * event.
 * 
 */
public class ExtendedRelativeLayout extends RelativeLayout {
	private OnDispatchTouchListener mTouchListener;

	public ExtendedRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		mTouchListener.onDispatchTouch(ev);
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * Set a callback for when we dispatch touch events to our children
	 * 
	 * @param listener
	 */
	public void setOnDispatchListener(OnDispatchTouchListener listener) {
		mTouchListener = listener;
	}

	/**
	 * Callback for when dispatchTouchEvent(MotionEvent) is called on us.
	 * 
	 */
	public interface OnDispatchTouchListener {
		/**
		 * The touch event dispatched.
		 * 
		 * @param ev
		 */
		public void onDispatchTouch(MotionEvent ev);
	}

}
