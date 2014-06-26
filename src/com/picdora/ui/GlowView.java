package com.picdora.ui;

import org.androidannotations.annotations.EView;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
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

/**
 * Creates a rectangular glow that fades in and then out. You pass the Rect that
 * the glow should surround and an inner and outer glow is set on each edge.
 * <p>
 * The glow works by using 8 linear gradients, two for each edge. On each each
 * one gradient points away as an outer glow and another points in as an inner
 * glow. Combined together these give the appearance of a uniform glow around
 * the edge.
 * <p>
 * A RadialGradient is added to each corner to fill it and smooth out the corner
 * glow.
 */
@EView
public class GlowView extends View {
	// We need 8 edge gradient shapes, so that we have an inside and outside
	// glow for each of the four edges, plus 4 more for the corners
	private static final int GRADIENT_ARRAY_SIZE = 12;
	private GlowGradient[] mGradients;

	private AnimationSet mAnimation;

	private static final int DEFAULT_COLOR = Color.RED;

	// how long to spend fading each way
	private static final int FADE_IN_DURATION = 200;
	private static final int FADE_OUT_DURATION = 400;
	// How long to pause at maximum glow between fading in and fading out
	private static final int FADE_PAUSE_DURATION = 200;

	private static int GLOW_WIDTH_PX;
	private static final int GLOW_WIDTH_DP = 20;
	// The maximum transparency to use when showing the glow
	private static final float ALPHA = .7f;

	private Rect mGlowBounds;

	private int mGlowStartColor;
	private int mGlowEndColor;
	// whether the glow animation is currently running
	private boolean mIsGlowing;

	// the tile mode to use with the gradients
	private static final TileMode TILE_MODE = android.graphics.Shader.TileMode.CLAMP;

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

		mGlowStartColor = DEFAULT_COLOR;

		// Create 8 gradients, an inner and outer glow for each of the four
		// edges
		mGradients = new GlowGradient[GRADIENT_ARRAY_SIZE];
		for (int i = 0; i < GRADIENT_ARRAY_SIZE; i++) {
			mGradients[i] = new GlowGradient();
		}

		createAnimation();
	}

	/**
	 * Set the rectangle that the glow should highlight. Will update glow if the
	 * animation is currently going.
	 * 
	 * @param bounds
	 */
	public GlowView setGlowBounds(RectF bounds) {
		mGlowBounds = UiUtil.rect(bounds);

		if (mIsGlowing) {
			calculateGradients();
		}

		return this;
	}

	/**
	 * Whether or not the glow animation is currently going.
	 * 
	 * @return
	 */
	public boolean isGlowing() {
		return mIsGlowing;
	}

	/**
	 * Set the color to use for the glow. Will update glow if the animation is
	 * currently going.
	 * 
	 * @param color
	 */
	public GlowView setGlowColor(int color) {
		mGlowStartColor = color;
		mGlowEndColor = UiUtil.adjustAlpha(color, 0);

		if (mIsGlowing) {
			calculateGradients();
		}

		return this;
	}

	/**
	 * Start the glowing animation with whatever bounds and color have been set.
	 * Cancels any current animation to start this one.
	 * 
	 */
	public void doGlow() {
		calculateGradients();
		setGlowVisibility(true);
		startAnimation(mAnimation);
	}

	/**
	 * Set the edges to match our target bounds and color
	 * 
	 */
	private void calculateGradients() {
		Rect rect;
		// the width of each edge. Use this for shorthand
		int w = GLOW_WIDTH_PX;
		// short hand for each coordinate
		int l = mGlowBounds.left;
		int r = mGlowBounds.right;
		int t = mGlowBounds.top;
		int b = mGlowBounds.bottom;

		// above the top
		rect = new Rect(l, t - w, r, t);
		mGradients[0].setRect(rect);
		mGradients[0].setGradient(createGradient(rect, false));

		// below the top
		rect = new Rect(l, t, r, t + w);
		mGradients[1].setRect(rect);
		mGradients[1].setGradient(createGradient(rect, true));

		// outside the right
		rect = new Rect(r, t, r + w, b);
		mGradients[2].setRect(rect);
		mGradients[2].setGradient(createGradient(rect, false));

		// inside the right
		rect = new Rect(r - w, t, r, b);
		mGradients[3].setRect(rect);
		mGradients[3].setGradient(createGradient(rect, true));

		// below the bottom
		rect = new Rect(l, b, r, b + w);
		mGradients[4].setRect(rect);
		mGradients[4].setGradient(createGradient(rect, true));

		// above the bottom
		rect = new Rect(l, b - w, r, b);
		mGradients[5].setRect(rect);
		mGradients[5].setGradient(createGradient(rect, false));

		// outside the left
		rect = new Rect(l - w, t, l, b);
		mGradients[6].setRect(rect);
		mGradients[6].setGradient(createGradient(rect, true));

		// inside the left
		rect = new Rect(l, t, l + w, b);
		mGradients[7].setRect(rect);
		mGradients[7].setGradient(createGradient(rect, false));

		/*
		 * Make the corners with radial glow
		 */

		// top left
		rect = new Rect(l - w, t - w, l, t);
		mGradients[8].setRect(rect);
		mGradients[8].setGradient(new RadialGradient(l, t, w, mGlowStartColor,
				mGlowEndColor, TILE_MODE));

		// top right
		rect = new Rect(r, t - w, r + w, t);
		mGradients[9].setRect(rect);
		mGradients[9].setGradient(new RadialGradient(r, t, w, mGlowStartColor,
				mGlowEndColor, TILE_MODE));

		// bottom left
		rect = new Rect(l - w, b, l, b + w);
		mGradients[10].setRect(rect);
		mGradients[10].setGradient(new RadialGradient(l, b, w, mGlowStartColor,
				mGlowEndColor, TILE_MODE));

		// bottom right
		rect = new Rect(r, b, r + w, b + w);
		mGradients[11].setRect(rect);
		mGradients[11].setGradient(new RadialGradient(r, b, w, mGlowStartColor,
				mGlowEndColor, TILE_MODE));

		// Invalidate the view since our edges have been changed
		invalidate();
	}

	/**
	 * Create a linear gradient based on the given rect. The direction will be
	 * perpendicular to the length of the rectangle. By default the direction
	 * will be from left to right or top to bottom, but you can set invert to
	 * true to reverse this. The gradient will start with
	 * {@link #mGlowStartColor} and go to transparent.
	 * 
	 * @param r
	 *            The coords to set the gradient with
	 * @param invert
	 *            Set to true to have the gradient go right to left or top to
	 *            bottom.
	 * @return
	 */
	private Shader createGradient(Rect r, boolean invert) {
		// invert colors if necessary
		int start, end;

		if (invert) {
			start = mGlowEndColor;
			end = mGlowStartColor;
		} else {
			start = mGlowStartColor;
			end = mGlowEndColor;
		}

		// if the rect is wider than it is tall then the gradient should run
		// vertically
		boolean vertical = Math.abs(r.left - r.right) > Math.abs(r.top
				- r.bottom);

		if (vertical) {
			return new LinearGradient(r.left, (vertical ? r.bottom : r.left),
					r.left, r.top, start, end, TILE_MODE);
		} else {
			return new LinearGradient(r.left, r.top, r.right, r.top, start,
					end, TILE_MODE);
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
				setGlowVisibility(false);
				mIsGlowing = false;
			}

			public void onAnimationRepeat(Animation animation) {

			}

			public void onAnimationStart(Animation animation) {
				mIsGlowing = true;
			}
		});
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw each edge only if it has been set to show
		for (GlowGradient e : mGradients) {
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
	private class GlowGradient {
		public final Rect r;
		public final Paint p;
		// whether this edge should be used. Default to true.
		public boolean show;

		public GlowGradient() {
			p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setDither(true);
			p.setFilterBitmap(true);
			p.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

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
