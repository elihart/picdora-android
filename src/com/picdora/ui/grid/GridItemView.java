package com.picdora.ui.grid;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.DrawableRes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.makeramen.RoundedImageView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.TypeEvaluator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.UiUtil;

/**
 * Used with the ImageGridSelector and ImageGridAdapter to display an item. A
 * square image is shown with text overlayed on top. The image is loaded from
 * url using the Universal Image Loader.
 */
@EViewGroup
public class GridItemView extends RelativeLayout {
	protected TextView mText;
	protected PicdoraGridImage mImage;

	// the tint to put over each image
	@ColorRes(R.color.channel_grid_item_tint)
	protected int mDefaultTint;
	// image tint when pressed
	@ColorRes(R.color.channel_grid_item_tint_pressed)
	protected int mPressedTint;
	// image tint when selected
	@ColorRes(R.color.channel_grid_item_selected_border)
	protected int mHighlightedBorderColor;
	/** Placeholder color while image loads. */
	@ColorRes(R.color.black)
	protected int mPlaceholderColor;

	/** Width of the highlighted border in DP */
	private final static int HIGHLIGHT_BORDER_WIDTH_DP = 4;
	/**
	 * Width of border in pixels that will be calculated at runtime based on
	 * screen density
	 */
	private static int HIGHLIGHT_BORDER_WIDTH_PX = 0;

	protected static final int TEXT_PADDING_DP = 8;
	protected static final int TEXT_SIZE_DP = 20;
	/* TODO: Scale text size based on image size? */
	protected static final int CORNER_RADIUS = 10;

	private ValueAnimator mPressAnimation;

	protected boolean highlighted;
	/** Whether text should be shown on top of an image. Defaults to true */
	private boolean mShowText = true;

	public GridItemView(Context context) {
		super(context);

		/* Init border width if necessary */
		if (HIGHLIGHT_BORDER_WIDTH_PX == 0) {
			HIGHLIGHT_BORDER_WIDTH_PX = UiUtil
					.dpToPixel(HIGHLIGHT_BORDER_WIDTH_DP);
		}

		mImage = new PicdoraGridImage(context);

		mImage.setLayoutParams(new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT));

		mImage.setScaleType(ScaleType.CENTER_CROP);
		mImage.setCornerRadius(CORNER_RADIUS);
		mImage.setBorderWidth(HIGHLIGHT_BORDER_WIDTH_PX);

		addView(mImage);

		mText = new TextView(context);
		LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		mText.setLayoutParams(params);

		mText.setTextColor(context.getResources().getColor(
				R.color.channel_grid_item_text_color));
		mText.setGravity(Gravity.CENTER);
		int pad = dpToPixel(TEXT_PADDING_DP);
		mText.setPadding(pad, pad, pad, pad);
		mText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP);
		FontHelper.setTypeFace(mText, FontStyle.REGULAR);

		addView(mText);
	}

	protected int dpToPixel(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	/**
	 * Set this grid item to display the given image and text
	 * 
	 * @param text
	 *            The text to display. Can be null if
	 *            {@link #setShowText(boolean)} is set to false.
	 * @param url
	 *            The url of the image to display. Null or empty to not show an
	 *            image.
	 * @param highlight
	 *            Whether the item should be highlighted
	 */
	public void bind(String text, String url, boolean highlight) {
		// use a placeholder until an image loads
		mImage.setImageDrawable(new ColorDrawable(mPlaceholderColor));

		highlighted = highlight;

		decorate();

		if (mShowText) {
			mText.setText(text.toUpperCase(java.util.Locale.US));
		}

		if (!Util.isStringBlank(url)) {
			ImageLoader.getInstance().displayImage(url, mImage);
		}
	}

	/**
	 * Toggle whether or not this item should be highlighted, indicating
	 * selection
	 * 
	 * @param highlighted
	 */
	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
		decorate();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		// detect when the state changes so we can tell if we're being pressed
		decorate();
	}

	/**
	 * Adjust the image colors based on current state.
	 */
	protected void decorate() {
		/*
		 * Fade the press color in on press, or cancel the animation on no press
		 * and set default tint
		 */
		if (isPressed()) {
			startPressAnimation();
		} else {
			cancelPressAnimation();

			// Show the default tint.
			if (mShowText) {
				// Set a dark overlay to make the text stand out.
				mImage.setColorFilter(mDefaultTint);
			} else {
				// If we are not showing text then we don't need the black
				// overlay
				mImage.setColorFilter(null);
			}
		}

		/* Add a border if we are highlighted */
		if (highlighted) {
			mImage.setBorderColor(mHighlightedBorderColor);
		} else {
			mImage.setBorderColor(Color.TRANSPARENT);
		}

	}

	private void cancelPressAnimation() {
		if (mPressAnimation != null) {
			mPressAnimation.cancel();
		}
	}

	private void startPressAnimation() {
		/* Lazy init animation */
		if (mPressAnimation == null) {
			int endColor = mPressedTint;
			// transparent version of end color
			int startColor = UiUtil.adjustAlpha(mPressedTint, 0);

			mPressAnimation = ValueAnimator.ofObject(new ArgbDipEvaluator(),
					startColor, endColor);
		}

		/* Don't start it again if it's already running */
		if (mPressAnimation.isRunning()) {
			return;
		}

		mPressAnimation.removeAllListeners();
		mPressAnimation.removeAllUpdateListeners();

		mPressAnimation.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				if (mImage != null) {
					mImage.setColorFilter((Integer) animator.getAnimatedValue());
				}
			}

		});

		/* Set the endColor at the end of the animation */
		mPressAnimation.addListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator arg0) {
				// mImage.setColorFilter(mPressedTint);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {

			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				// mImage.setColorFilter(mPressedTint);
			}

			@Override
			public void onAnimationCancel(Animator arg0) {

			}
		});

		/*
		 * Set the animation to last as long as it takes to register a long
		 * click
		 */
		mPressAnimation.setDuration(ViewConfiguration.getLongPressTimeout());
		mPressAnimation.start();

	}

	/**
	 * Set whether text should be shown overlayed on the image
	 * 
	 * @param showText
	 */
	public void setShowText(boolean showText) {
		mShowText = showText;
		setTextVisibility(showText);
		decorate();
	}

	private void setTextVisibility(boolean visible) {
		if (visible) {
			mText.setVisibility(View.VISIBLE);
		} else {
			mText.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Extend Rounded ImageView to have it's height snap to it's width to force
	 * a square
	 * 
	 */
	protected class PicdoraGridImage extends RoundedImageView {

		public PicdoraGridImage(Context context) {
			super(context);
		}

		public PicdoraGridImage(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public PicdoraGridImage(Context context, AttributeSet attrs,
				int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			// Snap to width
			setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
		}
	}

	/**
	 * Interpolates an ARGB value where the end value is briefly used at the
	 * start, but fades quickly to the start value before continuing linearly to
	 * the end value as normal.
	 */
	private class ArgbDipEvaluator implements TypeEvaluator {
		/**
		 * The percent of the animation to spend at the beginning doing the fade
		 * from end value to start value
		 */
		private static final float INTRO_PERCENT = .5f;

		/**
		 * This function returns the calculated in-between value for a color
		 * given integers that represent the start and end values in the four
		 * bytes of the 32-bit int.
		 * 
		 * @param fraction
		 *            The fraction from the starting to the ending values
		 * @param startValue
		 *            A 32-bit int value representing colors in the separate
		 *            bytes of the parameter
		 * @param endValue
		 *            A 32-bit int value representing colors in the separate
		 *            bytes of the parameter
		 * @return The color to use
		 */
		public Object evaluate(float fraction, Object startValue,
				Object endValue) {
			int startInt = (Integer) startValue;
			int startA = (startInt >> 24) & 0xff;
			int startR = (startInt >> 16) & 0xff;
			int startG = (startInt >> 8) & 0xff;
			int startB = startInt & 0xff;

			int endInt = (Integer) endValue;
			int endA = (endInt >> 24) & 0xff;
			int endR = (endInt >> 16) & 0xff;
			int endG = (endInt >> 8) & 0xff;
			int endB = endInt & 0xff;

			if (fraction < INTRO_PERCENT) {
				fraction = 1 - (fraction / INTRO_PERCENT);
			} else {
				fraction = (fraction - INTRO_PERCENT) / (1 - INTRO_PERCENT);
			}

			return (int) ((startA + (int) (fraction * (endA - startA))) << 24)
					| (int) ((startR + (int) (fraction * (endR - startR))) << 16)
					| (int) ((startG + (int) (fraction * (endG - startG))) << 8)
					| (int) ((startB + (int) (fraction * (endB - startB))));
		}
	}

}