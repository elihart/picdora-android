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

/**
 * Creates a rectangular glow that fades in and then out. You pass the Rect that
 * the glow should surround and an inner and outer glow is set on each edge.
 * <p>
 * The glow works by using 8 linear gradients, two for each edge. On each each
 * one gradient points away as an outer glow and another points in as an inner
 * glow. Combined together these give the appearance of a uniform glow around
 * the edge.
 */
@EView
public class GlowView extends View {
	// We need 8 edge gradient shapes, so that we have an inside and outside
	// glow for each of the four edges
	private static final int EDGE_ARRAY_SIZE = 8;
	private EdgeGradient[] mEdges;

	private AnimationSet mAnimation;

	private static final int DEFAULT_COLOR = Color.RED;

	// how long to spend fading each way
	private static final int FADE_IN_DURATION = 200;
	private static final int FADE_OUT_DURATION = 400;
	// How long to pause at maximum glow between fading in and fading out
	private static final int FADE_PAUSE_DURATION = 200;

	private static int GLOW_WIDTH_PX;
	private static final int GLOW_WIDTH_DP = 30;
	// The maximum transparency to use when showing the glow
	private static final float ALPHA = .7f;

	private Rect mGlowBounds;

	private int mGlowColor;

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
		setGlowVisibility(false);

		// convert our desired glow size to pixels based on the device screen
		// density
		GLOW_WIDTH_PX = UiUtil.dpToPixel(GLOW_WIDTH_DP);

		mGlowColor = DEFAULT_COLOR;

		// Create 8 gradients, an inner and outer glow for each of the four
		// edges
		mEdges = new EdgeGradient[EDGE_ARRAY_SIZE];
		for (int i = 0; i < EDGE_ARRAY_SIZE; i++) {
			mEdges[i] = new EdgeGradient();
		}

		createAnimation();
	}

	/**
	 * Set the rectangle that the glow should highlight. Will redraw glow but
	 * not change visibility or animation.
	 * 
	 * @param bounds
	 */
	public void setGlowBounds(Rect bounds) {
		mGlowBounds = bounds;
		remakeEdges();
	}

	/**
	 * Set the color to use for the glow. Will redraw glow but not change
	 * visibility or animation.
	 * 
	 * @param color
	 */
	public void setGlowColor(int color) {
		mGlowColor = color;
		remakeEdges();
	}

	/**
	 * Set the color and bounds to use for the glow. Will redraw glow but not
	 * change visibility or animation. If you need to change both this is more
	 * efficient that separately setting the color and bounds as this will only
	 * redraw once instead of twice.
	 * 
	 * @param color
	 */
	public void setGlowBoundsAndColor(Rect bounds, int color) {
		mGlowBounds = bounds;
		mGlowColor = color;
		remakeEdges();
	}

	/**
	 * Start the glowing animation with whatever bounds and color have been set.
	 * Cancels any current animation to start this one.
	 * 
	 */
	public void doGlow() {
		Util.log("Glow");
		// cancel any existing animation
		clearAnimation();
		setGlowVisibility(true);
		startAnimation(mAnimation);
	}

	/**
	 * Set the edges to match our target bounds and color
	 * 
	 */
	private void remakeEdges() {
		Rect r;
		// the width of each edge. Use this for shorthand
		int w = GLOW_WIDTH_PX;

		// above the top
		r = new Rect(mGlowBounds.left, mGlowBounds.top - w, mGlowBounds.right,
				mGlowBounds.top);
		mEdges[0].setRect(r);
		mEdges[0].setGradient(createGradient(r, mGlowColor, false));

		// below the top
		r = new Rect(mGlowBounds.left, mGlowBounds.top, mGlowBounds.right,
				mGlowBounds.top + w);
		mEdges[1].setRect(r);
		mEdges[1].setGradient(createGradient(r, mGlowColor, true));

		// outside the right
		r = new Rect(mGlowBounds.right, mGlowBounds.top, mGlowBounds.right + w,
				mGlowBounds.bottom);
		mEdges[2].setRect(r);
		mEdges[2].setGradient(createGradient(r, mGlowColor, false));

		// inside the right
		r = new Rect(mGlowBounds.right - w, mGlowBounds.top, mGlowBounds.right,
				mGlowBounds.bottom);
		mEdges[3].setRect(r);
		mEdges[3].setGradient(createGradient(r, mGlowColor, true));

		// below the bottom
		r = new Rect(mGlowBounds.left, mGlowBounds.bottom, mGlowBounds.right,
				mGlowBounds.bottom + w);
		mEdges[4].setRect(r);
		mEdges[4].setGradient(createGradient(r, mGlowColor, true));

		// above the bottom
		r = new Rect(mGlowBounds.left, mGlowBounds.bottom - w,
				mGlowBounds.right, mGlowBounds.bottom);
		mEdges[5].setRect(r);
		mEdges[5].setGradient(createGradient(r, mGlowColor, false));

		// outside the left
		r = new Rect(mGlowBounds.left - w, mGlowBounds.top, mGlowBounds.left,
				mGlowBounds.bottom);
		mEdges[6].setRect(r);
		mEdges[6].setGradient(createGradient(r, mGlowColor, true));

		// inside the left
		r = new Rect(mGlowBounds.left, mGlowBounds.top, mGlowBounds.left + w,
				mGlowBounds.bottom);
		mEdges[7].setRect(r);
		mEdges[7].setGradient(createGradient(r, mGlowColor, false));

		// Invalidate the view since our edges have been changed
		invalidate();
	}

	/**
	 * Create a linear gradient based on the given rect. The direction will be
	 * perpendicular to the length of the rectangle. By default the direction
	 * will be from left to right or top to bottom, but you can set invert to
	 * true to reverse this. The gradient will start with the given color and go
	 * to transparent.
	 * 
	 * @param r
	 *            The coords to set the gradient with
	 * @param color
	 *            The starting color.
	 * @param invert
	 *            Set to true to have the gradient go right to left or top to
	 *            bottom.
	 * @return
	 */
	private Shader createGradient(Rect r, int color, boolean invert) {
		// create a copy and invert it if necessary
		r = new Rect(r);
		if (invert) {
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
		fadeIn.setDuration(FADE_IN_DURATION);

		Animation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setInterpolator(new AccelerateInterpolator());
		fadeOut.setStartOffset(FADE_IN_DURATION + FADE_PAUSE_DURATION);
		fadeOut.setDuration(FADE_OUT_DURATION);

		mAnimation = new AnimationSet(false);
		mAnimation.addAnimation(fadeIn);
		mAnimation.addAnimation(fadeOut);

		/*
		 * Turn the glow visibility on when the animation starts and hide it
		 * again at the end
		 */
		mAnimation.setAnimationListener(new AnimationListener() {
			public void onAnimationEnd(Animation animation) {
				Util.log("animation end");
				setGlowVisibility(false);
			}

			public void onAnimationRepeat(Animation animation) {

			}

			public void onAnimationStart(Animation animation) {
				Util.log("animation start");
			}
		});
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw each edge only if it has been set to show
		for (EdgeGradient e : mEdges) {
			if (e.show) {
				canvas.drawRect(e.r, e.p);
			}
		}
	}

	/**
	 * Set whether the glow should be visible. If set to false the alpha level
	 * will be set to 0, so it is still drawn but transparent. On true the alpha
	 * level will be set to {@value #ALPHA}.
	 * 
	 * @param visible
	 *            True to show this view, false to hide it.
	 */
	private void setGlowVisibility(boolean visible) {
		if (visible) {
			ViewHelper.setAlpha(this, ALPHA);
		} else {
			ViewHelper.setAlpha(this, 0f);
		}
	}

	/**
	 * Helper class to hold the glow for an edge. Holds the Rect that the glow
	 * will be held over and the Paint to draw it's gradient.
	 */
	private class EdgeGradient {
		public final Rect r;
		public final Paint p;
		// whether this edge should be used. Default to true.
		public boolean show;

		public EdgeGradient() {
			p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setDither(true);
			show = true;
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
