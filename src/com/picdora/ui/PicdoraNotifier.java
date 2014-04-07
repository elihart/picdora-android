package com.picdora.ui;

import java.util.LinkedList;
import java.util.Queue;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.ui.FontHelper.FontStyle;

/**
 * Display and manager notifications on screen. Notifications are delivered to
 * us, queued, and displayed in the order we receive them.
 */
@EViewGroup(R.layout.picdora_notifier)
public class PicdoraNotifier extends RelativeLayout {
	/** View to show notification text */
	@ViewById
	protected TextView text;

	/** Queue of notifications to show */
	private Queue<PicdoraNotifier.Notification> mNotificationQueue;
	/** The currently displayed notification */
	private Notification mCurrNotification;

	/** Animation to use when showing notifications */
	private AnimationSet mAnimation;
	/** how long to spend fading in in milleseconds*/
	private static final int FADE_IN_DURATION = 700;
	/** how long to spend fading out in milleseconds*/
	private static final int FADE_OUT_DURATION = 700;
	/** How long to show the notification between fading in and fading out in milleseconds*/
	private static final int NOTIFICATION_DISPLAY_LENGTH = 1500;

	public PicdoraNotifier(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public PicdoraNotifier(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PicdoraNotifier(Context context) {
		super(context);
	}	
	/**
	 * Initialize notification queue and setup views
	 */
	@AfterViews
    protected void init() {
		mNotificationQueue = new LinkedList<PicdoraNotifier.Notification>();
		createAnimation();
		FontHelper.setTypeFace(text, FontStyle.REGULAR);
    }

	/**
	 * Create the animation we will use to fade the notification in and out
	 */
	private void createAnimation() {
		Animation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator());
		fadeIn.setDuration(FADE_IN_DURATION);

		Animation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setInterpolator(new AccelerateInterpolator());
		fadeOut.setStartOffset(FADE_IN_DURATION + NOTIFICATION_DISPLAY_LENGTH);
		fadeOut.setDuration(FADE_OUT_DURATION);

		mAnimation = new AnimationSet(false);
		mAnimation.addAnimation(fadeIn);
		mAnimation.addAnimation(fadeOut);

		mAnimation.setAnimationListener(new AnimationListener() {
			public void onAnimationEnd(Animation animation) {
				onNotificationAnimationFinished();
			}

			public void onAnimationRepeat(Animation animation) {

			}

			public void onAnimationStart(Animation animation) {
				onNotificationAnimationStarted();
			}
		});
	}

	/**
	 * The currently displayed notification is fading out.
	 * 
	 */
	protected void onNotificationAnimationFinished() {
		text.setVisibility(View.INVISIBLE);
		mCurrNotification = null;
		showNextNotification();
	}

	/**
	 * The currently displayed notification is fading in.
	 * 
	 */
	protected void onNotificationAnimationStarted() {
		text.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the next notification in the queue if there is one and no
	 * notification is currently being displayed
	 * 
	 */
	private void showNextNotification() {
		if (mCurrNotification == null && !mNotificationQueue.isEmpty()) {
			mCurrNotification = mNotificationQueue.poll();
			text.setText(mCurrNotification.text);
			text.startAnimation(mAnimation);
		}
	}

	public void notify(String message) {
		if (message == null) {
			throw new IllegalArgumentException("message can't be null");
		} else {
			mNotificationQueue.add(new Notification(message));
			showNextNotification();
		}
	}

	/**
	 * Hold the details of a notification to show.
	 */
	private class Notification {
		public String text;

		public Notification(String text) {
			this.text = text;
		}

	}

}
