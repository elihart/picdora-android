package com.picdora.ui;

import org.androidannotations.annotations.EView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

@EView
public class GlowView extends View {
	private Paint mGradientPaint1;
	private Paint mGradientPaint2;

	private int mEdge1;
	private int mEdge2;
	private Rect rect;
	private Paint paint;
	private int color;
	
	private AnimationSet mAnimation;
	
	// how long to spend fading in in millis
	private static final int FADE_IN_DURATION = 300;
	// how long to pause once faded in before starting to fade out
	private static final int FADE_PAUSE = 100;
	// how long to spend fading out
	private static final int FADE_OUT_DURATION = 300;

	private static int GLOW_WIDTH_PX;
	private static final int GLOW_WIDTH_DP = 8;

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
	 * Animate a glowing rectangle of the given color that fades in and then out
	 * @param bounds The bounds to create the rectangle with relative to this view
	 * @param color The color to make the glow
	 */
	public void animateGlowRect(Rect bounds, int color) {
		// cancel any existing animation
		clearAnimation();
		
		this.rect = new Rect(bounds);
		this.color = color;
		createGradient(rect);
		invalidate();
		
		startAnimation(mAnimation);	
	}

	/**
	 * Create the paints we will need, and set the view to hidden to begin with.
	 */
	private void init() {
		mGradientPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGradientPaint1.setDither(true);

		mGradientPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGradientPaint2.setDither(true);

		GLOW_WIDTH_PX = UiUtil.dpToPixel(GLOW_WIDTH_DP);

		rect = new Rect(0, 0, getWidth(), getHeight());
		color = Color.RED;

		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(10);
		
		setVisible(false);
		
		createAnimation();
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
	    fadeOut.setStartOffset(FADE_IN_DURATION + FADE_PAUSE);
	    fadeOut.setDuration(FADE_OUT_DURATION);

	    mAnimation = new AnimationSet(false); 
	    mAnimation.addAnimation(fadeIn);
	    mAnimation.addAnimation(fadeOut);

	    mAnimation.setAnimationListener(new AnimationListener() {
	        public void onAnimationEnd(Animation animation) {
	        	// hide again
	        	setVisible(false);
	        }
	        
	        public void onAnimationRepeat(Animation animation) {
	           
	        }
	        
	        public void onAnimationStart(Animation animation) {
	        	// show
	        	setVisible(true);
	        }
	    });
		
	}

	private void createGradient(Rect r) {

		int[] location = new int[2];
		getLocationOnScreen(location);

		int x = location[0];
		int y = location[1];
		RectF currPos = new RectF(x, y, x + getWidth(), y + getHeight());

		int dTop = (int) (r.top - currPos.top);
		int dBottom = (int) (currPos.bottom - r.bottom);
		int dLeft = (int) (r.left - currPos.left);
		int dRight = (int) (currPos.right - r.right);

		if (dTop + dBottom > dLeft + dRight) {
			createVerticalGradient(r.top, r.bottom);
		} else {
			createHorizontalGradient((int) r.left, (int) r.right);

		}
	}

	private void createHorizontalGradient(int left, int right) {
		// mEdge1 = left;
		// mEdge2 = right;
		//
		// Shader s1 = getGradient(right, 0, right + GLOW_WIDTH_PX, getHeight(),
		// mColorLiked);
		// Shader s2 = getGradient(left, y0, x1, y1, mColorLiked);
		//
		// mGradientPaint1.setShader(s1);
		// mGradientPaint2.setShader(s2);
	}

	private void createVerticalGradient(float top, float bottom) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onDraw(Canvas canvas) {

		canvas.drawRect(rect, paint);
		// canvas.drawRect(0, 0, getWidth(), getHeight(), mGradientPaint1);
	}

	/**
	 * Create a gradient to use at the given points
	 * 
	 * @param x0
	 *            The x-coordinate for the start of the gradient line
	 * @param y0
	 *            The y-coordinate for the start of the gradient line
	 * @param x1
	 *            The x-coordinate for the end of the gradient line
	 * @param y1
	 *            The y-coordinate for the end of the gradient line
	 * @param startColor
	 * @return
	 */
	private LinearGradient getGradient(float x0, float y0, float x1, float y1) {
		return new LinearGradient(x0, y0, x1, y1, color,
				Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP);

	}

	/**
	 * Set whether we should be visible or not.
	 * @param hidden True to show this view, false to hide it.
	 */
	private void setVisible(boolean visible){
		if(visible){
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.INVISIBLE);
		}
	}
	

}
