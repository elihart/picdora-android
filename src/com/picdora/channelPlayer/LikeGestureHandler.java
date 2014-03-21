package com.picdora.channelPlayer;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.picdora.channelPlayer.ChannelViewActivity.LIKE_EVENT;
import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelImage.LIKE_STATUS;

/**
 * 
 * Examine touch events for like gestures: a swipe up for a like or a swipe down
 * for a dislike. The gesture must be sufficiently vertical, long, and fast to
 * qualify. It must also not follow a multi touch event too closely because
 * pinches can often set off swipes and the like gesture should be independent
 * of a pinch. Lastly, we check to make sure the activity isn't currently zoomed
 * in, if it is then we ignore the motion as it conflicts with a zoom motion.
 * <p>
 * If all those checks pass then we recognize the like event, get the currently
 * displayed fragment from the Activity, adjust the fragment's image Like status
 * and alert it of the change.
 * <p>
 * A swipe up or down will reset the image Like status to neutral if it is
 * currently in the opposite extreme. This means the user has to swipe up twice
 * to go from disliked, to neutral, and finally to liked, or vice versa for
 * liked to disliked.
 */

@EBean
public class LikeGestureHandler {
	@RootContext
	protected ChannelViewActivity mActivity;

	/** The length of a swipe before we will recognize it as a like gesture */
	private static final int SWIPE_MIN_DISTANCE = 120;
	/**
	 * The error threshold in a swipe being non vertical before we won't count
	 * it as a like swipe
	 */
	private static final int SWIPE_MAX_OFF_PATH = 250;
	/** Minimum velocity for a like swipe to count */
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	/** use a gesture detector to recognize swipes for us */
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

	@AfterInject
	protected void init() {
		mGestureDetector = new GestureDetectorCompat(mActivity,
				new LikeGestureListener());
	}

	/**
	 * Check if the given MotionEvent includes a like gesture, and handle it if
	 * so.
	 * 
	 * @param ev
	 */
	public void checkTouchForLikeGesture(MotionEvent ev) {
		/*
		 * If the event has more than one finger then ignore it and save the
		 * time that it happened.
		 */
		if (ev.getPointerCount() > 1) {
			mLastPinch = System.currentTimeMillis();
			return;
		} else {
			mGestureDetector.onTouchEvent(ev);
		}
	}

	/**
	 * After a like event has been recognized we need to save the new like
	 * status and show visual feedback of the event.
	 * 
	 * @param status
	 */
	private void handleLikeEvent(LIKE_EVENT event) {
		// ignore event if we are zoomed in
		if (mActivity.isZoomed()) {
			return;
		}

		// ignore event if a pinch happened recently
		if ((System.currentTimeMillis() - mLastPinch) < DELAY_AFTER_PINCH) {
			return;
		}

		/*
		 * The like event is for the currently displayed image. Get the current
		 * fragment and then the image from that. If either is null then don't
		 * continue.
		 */
		ImageSwipeFragment frag = mActivity.getCurrentFragment();

		if (frag == null) {
			return;
		}
		ChannelImage image = frag.getImage();

		if (image == null) {
			return;
		}

		/*
		 * Adjust the current status of the image based on the event. Don't go
		 * straight from Liked to Disliked, or vice versa. Reset to Neutral
		 * first. This lets the user clear the status back to neutral without
		 * being stuck with either liked or disliked.
		 */
		LIKE_STATUS status = image.getLikeStatus();

		// if we're currently neutral then change to the new status
		if (status == LIKE_STATUS.NEUTRAL) {

			if (event == LIKE_EVENT.LIKED) {
				image.setLikeStatus(LIKE_STATUS.LIKED);
			} else if (event == LIKE_EVENT.DISLIKED) {
				image.setLikeStatus(LIKE_STATUS.DISLIKED);
			}

			image.saveAsync();
		}
		// If we're going from one extreme to the other then return to neutral
		else if ((status == LIKE_STATUS.LIKED && event == LIKE_EVENT.DISLIKED)
				|| (status == LIKE_STATUS.DISLIKED && event == LIKE_EVENT.LIKED)) {

			image.setLikeStatus(LIKE_STATUS.NEUTRAL);

			image.saveAsync();
		}

		/*
		 * Otherwise the status stays the same and we don't have to save it
		 * again. Still show the visual feedback though so the user is reassured
		 * of the current status.
		 */

		// show an indication on screen of the current status
		frag.setLikeStatus(image.getLikeStatus());

	}

	/**
	 * Use a simple gesture listener to check for flings.
	 */
	class LikeGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent event) {
			return true;

		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			// if the line isn't vertical enough then abort
			if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}
			// down to up
			else if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				handleLikeEvent(LIKE_EVENT.LIKED);
				return true;
			}
			// up to down
			else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				handleLikeEvent(LIKE_EVENT.DISLIKED);
				return true;
			} else {
				return false;
			}
		}
	}
}
