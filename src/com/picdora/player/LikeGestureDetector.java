package com.picdora.player;

import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.picdora.Util;
import com.picdora.player.ChannelViewActivity.LIKE_EVENT;

/**
 * Examine touch events for like gestures and alert a callback when a like event
 * occurs.
 */
public class LikeGestureDetector {

	// Gesture detection constants
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetectorCompat mGestureDetector;

	/**
	 * The amount of time to wait in milliseconds after a pinch until we will
	 * recognize a Like gesture. Set this high enough so that the user won't
	 * accidently do a Like swipe when they are pinching the photo.
	 */
	private static final int DELAY_AFTER_PINCH = 300;

	/**
	 * Keep track of when the last multi touch gesture was. We don't want to
	 * count the fling if it was combined with a pinch.
	 */
	private long mLastPinch = System.currentTimeMillis();

	/**
	 * Callback to alert the client when a like happens
	 * 
	 */
	private OnLikeEventListener mListener;

	/**
	 * A detector that examines touch events and alerts you when one of them is
	 * a like gesture.
	 * 
	 * @param context
	 * @param listener
	 *            The callback that will be used when a like gesture occurs.
	 *            Must not be null.
	 */
	public LikeGestureDetector(Activity context, OnLikeEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener cannot be null");
		} else if (context == null) {
			throw new IllegalArgumentException("Context cannot be null");
		}

		mListener = listener;
		mGestureDetector = new GestureDetectorCompat(context,
				new LikeGestureListener());
	}

	/**
	 * Callback for when a like event occurs
	 */
	public interface OnLikeEventListener {
		/**
		 * Called when a like event happens.
		 * 
		 * @param status
		 *            The status of the like event.
		 * @return True if the event was used, false otherwise.
		 */
		public boolean OnLikeEvent(LIKE_EVENT event);
	}

	/**
	 * Examine the given touch event for a like gesture.
	 * 
	 * @param ev
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent ev) {
		/*
		 * If the event has more than one finger then ignore it and save the
		 * time that it happened.
		 */
		if (ev.getPointerCount() > 1) {
			mLastPinch = System.currentTimeMillis();
			return false;
		}

		return mGestureDetector.onTouchEvent(ev);
	}

	/**
	 * Check for flings.
	 */
	class LikeGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent event) {
			return true;

		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			// ignore event if a pinch happened recently
			if ((System.currentTimeMillis() - mLastPinch) < DELAY_AFTER_PINCH) {
				return false;
			}
			// if the line isn't vertical enough then abort
			else if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}
			// down to up
			else if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				return mListener.OnLikeEvent(LIKE_EVENT.LIKED);
			}
			// up to down
			else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				return mListener.OnLikeEvent(LIKE_EVENT.DISLIKED);
			} else {
				return false;
			}
		}
	}
}
