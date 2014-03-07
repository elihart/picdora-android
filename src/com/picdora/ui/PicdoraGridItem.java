package com.picdora.ui;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.res.ColorRes;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.picdora.R;
import com.picdora.ui.FontHelper.STYLE;

@EViewGroup
public class PicdoraGridItem extends RelativeLayout {
	protected TextView mText;
	protected ImageView mImage;

	@ColorRes(R.color.channel_grid_item_tint)
	protected int defaultTint;
	@ColorRes(R.color.channel_grid_item_tint_pressed)
	protected int pressedTint;
	@ColorRes(R.color.channel_grid_item_tint_selected)
	protected int highlightedTint;

	protected static final int TEXT_PADDING_DP = 8;
	protected static final int TEXT_SIZE_DP = 25;

	protected boolean highlighted;

	public PicdoraGridItem(Context context) {
		super(context);

		mImage = new PicdoraGridImage(context);

		mImage.setLayoutParams(new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT));

		mImage.setColorFilter(defaultTint);
		mImage.setScaleType(ScaleType.CENTER_CROP);

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
		FontHelper.setTypeFace(mText, STYLE.MEDIUM);

		addView(mText);

	}

	protected int dpToPixel(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	public void bind(String text, String url, boolean highlight) {
		// reset the background to be white until an image loads
		mImage.setImageResource(R.drawable.rect_white);

		highlighted = highlight;

		if (highlighted) {
			mImage.setColorFilter(highlightedTint);
		} else {
			mImage.setColorFilter(defaultTint);
		}

		mText.setText(text);

		tryLoadUrl(url);
	}

	protected int numAttempts = 0;
	protected static final int MAX_ATTEMPTS = 3;

	protected void tryLoadUrl(final String url) {
		ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {

			@Override
			public void onLoadingFailed(String imageUri, View view,
					FailReason failReason) {
				handleLoadFailure(url);
			}

			@Override
			public void onLoadingComplete(String imageUri, View view,
					Bitmap loadedImage) {
				if (mImage != null) {
					mImage.setImageBitmap(loadedImage);
				}
			}

			@Override
			public void onLoadingCancelled(String imageUri, View view) {
				handleLoadFailure(url);
			}

			@Override
			public void onLoadingStarted(String imageUri, View view) {

			}
		});

	}

	protected void handleLoadFailure(String url) {
		if (mImage != null) {
			numAttempts++;
			if (numAttempts < MAX_ATTEMPTS) {
				tryLoadUrl(url);
			}
		}
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
		setTint();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		setTint();
	}

	protected void setTint() {
		if (isPressed()) {
			mImage.setColorFilter(pressedTint);
		} else if (highlighted) {
			mImage.setColorFilter(highlightedTint);
		} else {
			mImage.setColorFilter(defaultTint);
		}
	}

	protected class PicdoraGridImage extends ImageView {

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