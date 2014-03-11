package com.picdora.channelCreation;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Override view pager methods to be able to enable or disable swiping
 * @author eli
 *
 */
public class ChannelCreationViewPager extends ViewPager {

	private boolean enabled;

	public ChannelCreationViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.enabled = true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (this.enabled) {
			return super.onTouchEvent(event);
		}

		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.enabled) {
			return super.onInterceptTouchEvent(event);
		}

		return false;
	}

	/**
	 * Whether or not the user can swipe manually with touch
	 * @param enabled
	 */
	public void setPagingEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}