package com.picdora.ui.grid;

import java.util.Locale;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.DrawableRes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.makeramen.RoundedImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.picdora.R;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;

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
	protected int defaultTint;
	// image tint when pressed
	@ColorRes(R.color.channel_grid_item_tint_pressed)
	protected int pressedTint;
	// image tint when selected
	@ColorRes(R.color.channel_grid_item_tint_selected)
	protected int highlightedTint;

	// A blank white drawable to use when an image isn't loaded.
	@DrawableRes(R.drawable.rect_white)
	protected Drawable imagePlaceholder;

	protected static final int TEXT_PADDING_DP = 8;
	protected static final int TEXT_SIZE_DP = 20;
	protected static final int CORNER_RADIUS = 10;

	protected boolean highlighted;
	protected String text;
	protected String url;
	private boolean mShowText;

	public GridItemView(Context context) {
		super(context);

		mImage = new PicdoraGridImage(context);

		mImage.setLayoutParams(new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT));

		mImage.setScaleType(ScaleType.CENTER_CROP);
		mImage.setCornerRadius(CORNER_RADIUS);

		mImage.setImageDrawable(imagePlaceholder);

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
	 *            The text to display if {@link #mShowText} is enabled
	 * @param url
	 *            The url of the image to display
	 * @param highlight
	 *            Whether the item should be highlighted
	 */
	public void bind(String text, String url, boolean highlight) {
		this.text = text.toUpperCase(Locale.US);
		this.url = url;

		// reset the image to be white until an image loads
		mImage.setImageDrawable(imagePlaceholder);

		highlighted = highlight;

		setTint();

		if (mShowText) {
			mText.setText(text);
		}

		ImageLoader.getInstance().displayImage(url, mImage);
	}

	/**
	 * Toggle whether or not this item should be highlighted, indicating
	 * selection
	 * 
	 * @param highlighted
	 */
	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
		setTint();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		// detect when the state changes so we can tell if we're being pressed
		setTint();
	}

	/**
	 * Update the tint depending on our current state
	 */
	protected void setTint() {

		if (isPressed()) {
			mImage.setColorFilter(pressedTint);
		} else if (highlighted) {
			mImage.setColorFilter(highlightedTint);
		}
		// If we are not showing text then we don't need the black overlay
		else if (!mShowText) {
			mImage.setColorFilter(null);
		}
		// set a dark overlay to make the text stand out
		else {
			mImage.setColorFilter(defaultTint);
		}
	}

	/** Set whether text should be shown overlayed on the image
	 * 
	 * @param showText
	 */
	public void setShowText(boolean showText) {
		mShowText = showText;
		setTextVisibility(showText);
		setTint();
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

}