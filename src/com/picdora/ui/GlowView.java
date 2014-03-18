package com.picdora.ui;

import org.androidannotations.annotations.EView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

import com.nineoldandroids.view.ViewHelper;
import com.picdora.Util;

@EView
public class GlowView extends View {
	private View mThis;

	// We need 8 edge gradient shapes, so that we have an inside and outside
	// glow for each of the four edges
	private static final int EDGE_ARRAY_SIZE = 8;
	private EdgeGradient[] mEdges;

	private AnimationSet mAnimation;

	// how long to spend fading each way
	private static final int FADE_DURATION = 350;

	private static int GLOW_WIDTH_PX;
	private static final int GLOW_WIDTH_DP = 17;

	private boolean mQueueAnimation;

	// the tile mode to use with the gradients
	private static final TileMode TILE_MODE = android.graphics.Shader.TileMode.MIRROR;

	public GlowView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public GlowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GlowView(Context context) {
		super(context);
		init();
	}

	/**
	 * Create the paints we will need, and set the view to hidden to begin with.
	 */
	private void init() {
		mThis = this;
		setVisible(false);

		// convert our desired glow size to pixels based on the device screen
		// density
		GLOW_WIDTH_PX = UiUtil.dpToPixel(GLOW_WIDTH_DP);

		mEdges = new EdgeGradient[EDGE_ARRAY_SIZE];
		for (int i = 0; i < EDGE_ARRAY_SIZE; i++) {
			mEdges[i] = new EdgeGradient();
		}

		createAnimation();
	}

	/**
	 * Animate a glowing rectangle of the given color that fades in and then out
	 * 
	 * @param bounds
	 *            The bounds to create the rectangle with relative to this view
	 * @param color
	 *            The color to make the glow
	 */
	public void doGlow(Rect bounds, int color) {
		ViewHelper.setAlpha(mThis, 0.1f);
		//ViewHelper.setAlpha(mThis, 0.4f);
		setVisible(true);
		Util.log("Glow");
		// cancel any existing animation
		clearAnimation();
		setEdges(color, bounds);

		mQueueAnimation = true;

		invalidate();
	}

	/**
	 * Set the edges to match the given Rect bounds and use the given color
	 * 
	 * @param color
	 * @param t
	 *            The target rectangle whose edges we should cover
	 */
	private void setEdges(int color, Rect t) {
		Rect r;
		// the width of each edge. Use this for shorthand
		int w = GLOW_WIDTH_PX;

		// above the top
		r = new Rect(t.left, t.top - w, t.right, t.top);
		mEdges[0].setRect(r);
		mEdges[0].setGradient(createGradient(r, color, false));

		// below the top
		r = new Rect(t.left, t.top, t.right, t.top + w);
		mEdges[1].setRect(r);
		mEdges[1].setGradient(createGradient(r, color, true));

		// outside the right
		r = new Rect(t.right, t.top, t.right + w, t.bottom);
		mEdges[2].setRect(r);
		mEdges[2].setGradient(createGradient(r, color, false));

		// inside the right
		r = new Rect(t.right - w, t.top, t.right, t.bottom);
		mEdges[3].setRect(r);
		mEdges[3].setGradient(createGradient(r, color, true));

		// below the bottom
		r = new Rect(t.left, t.bottom, t.right, t.bottom + w);
		mEdges[4].setRect(r);
		mEdges[4].setGradient(createGradient(r, color, true));

		// above the bottom
		r = new Rect(t.left, t.bottom - w, t.right, t.bottom);
		mEdges[5].setRect(r);
		mEdges[5].setGradient(createGradient(r, color, false));

		// outside the left
		r = new Rect(t.left - w, t.top, t.left, t.bottom);
		mEdges[6].setRect(r);
		mEdges[6].setGradient(createGradient(r, color, true));

		// inside the left
		r = new Rect(t.left, t.top, t.left + w, t.bottom);
		mEdges[7].setRect(r);
		mEdges[7].setGradient(createGradient(r, color, false));

	}

	public Shader createGradient(Rect r, int color, boolean invert) {
		// create a copy and invert it if necessary
		r = new Rect(r);
		if(invert){
			int top = r.top;
			int left = r.left;
			r.left = r.right;
			r.right = left;
			r.top = r.bottom;
			r.bottom = top;
		}
		
		// if the rect is wider than it is tall then the gradient should run
		// vertically
		boolean vertical = Math.abs(r.left - r.right) > Math.abs(r.top
				- r.bottom);

		if (vertical) {
			return new LinearGradient(r.left, r.bottom, r.left, r.top, color,
					Color.TRANSPARENT, TILE_MODE);
		} else {
			return new LinearGradient(r.left, r.top, r.right, r.top, color,
					Color.TRANSPARENT, TILE_MODE);
		}
	}

	/**
	 * Create the animation we will use to fade the glow in and out
	 */
	private void createAnimation() {
		Animation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator());
		fadeIn.setDuration(FADE_DURATION);

		Animation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setInterpolator(new AccelerateInterpolator());
		fadeOut.setStartOffset(FADE_DURATION);
		fadeOut.setDuration(FADE_DURATION);

		mAnimation = new AnimationSet(false);
		mAnimation.addAnimation(fadeIn);
		mAnimation.addAnimation(fadeOut);

		mAnimation.setAnimationListener(new AnimationListener() {
			public void onAnimationEnd(Animation animation) {
				Util.log("On animate end");
				// hide again
				setVisible(false);
				ViewHelper.setAlpha(mThis, 0.1f);
			}

			public void onAnimationRepeat(Animation animation) {

			}

			public void onAnimationStart(Animation animation) {
				Util.log("On animate start");
				// show
				setVisible(true);
				ViewHelper.setAlpha(mThis, 1f);
			}
		});

	}

	@Override
	protected void onDraw(Canvas canvas) {
		Util.log("Draw");
		// draw each of the edges around the target
		for (EdgeGradient e : mEdges) {
			canvas.drawRect(e.r, e.p);
		}
		
		if(mQueueAnimation){
			mQueueAnimation = false;
			Util.log("animate");
			startAnimation(mAnimation);
		}
	}

	/**
	 * Set whether we should be visible or not.
	 * 
	 * @param hidden
	 *            True to show this view, false to hide it.
	 */
	private void setVisible(boolean visible) {
		if (visible) {
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.INVISIBLE);
		}
	}

	private class EdgeGradient {
		public final Rect r;
		public final Paint p;

		public EdgeGradient() {
			p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setDither(true);

			r = new Rect();
		}

		public void setRect(Rect rect) {
			r.top = rect.top;
			r.bottom = rect.bottom;
			r.left = rect.left;
			r.right = rect.right;
		}

		public void setGradient(Shader gradient) {
			p.setShader(gradient);
		}
	}

}
