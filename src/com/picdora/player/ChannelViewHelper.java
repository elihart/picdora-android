package com.picdora.player;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.view.MotionEvent;

import com.picdora.models.ChannelImage;
import com.picdora.models.ChannelImage.LIKE_STATUS;
import com.picdora.player.ChannelViewActivity.LIKE_EVENT;
import com.picdora.player.LikeGestureDetector.OnLikeEventListener;

/**
 * Holds helper methods for the ChannelViewActivity in order to keep that
 * activity cleaner.
 */
@EBean
public class ChannelViewHelper implements OnLikeEventListener {
	@RootContext
	protected ChannelViewActivity mActivity;

	private LikeGestureDetector mLikeDetector;

	@AfterInject
	protected void init() {
		mLikeDetector = new LikeGestureDetector(mActivity, this);
	}

	/**
	 * Handle a touch event.
	 * 
	 * @param ev
	 * @return True if we consumed the event and false otherwise.
	 */
	public boolean handleTouch(MotionEvent ev) {
		return mLikeDetector.onTouchEvent(ev);
	}

	/**
	 * Listen for like events from {@link #mLikeDetector}
	 */
	@Override
	public boolean OnLikeEvent(LIKE_EVENT event) {
		// don't register a like if we're zoomed in
		if (mActivity.isZoomed()) {
			return false;
		} else {
			handleLikeEvent(event);
			return true;
		}
	}

	/**
	 * After a like event has been recognized we need to save the new like
	 * status and show visual feedback of the event.
	 * 
	 * @param status
	 */
	private void handleLikeEvent(LIKE_EVENT event) {
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
		 * Adjust the current status of the image based on the event. Don't just
		 * straight from Liked to Disliked, or vice versa. Reset to Neutral
		 * first. This lets the user clear the status back to neutral without
		 * having to be stuck with either liked or disliked.
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
}
