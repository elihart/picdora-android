package com.picdora.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * A spinner implementation that adjusts the spinner width to wrap the currently
 * selected item.
 * 
 */
public class WrappedSpinner extends Spinner {
	private static final int MIN_WIDTH_DP = 60;
	private static int mMinWidth = -1;

	public WrappedSpinner(Context context) {
		super(context);
		init();
	}

	public WrappedSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public WrappedSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		if (mMinWidth == -1) {
			mMinWidth = UiUtil.dpToPixel(MIN_WIDTH_DP);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		/*
		 * Try to fit the width to wrap the currently selected item, but don't
		 * go any bigger than the currently measured width.
		 */
		final int measuredWidth = getMeasuredWidth();

		int selectionWidth = measureSelectionWidth();

		if (selectionWidth <= 0) {
			selectionWidth = measuredWidth;
		} else if (selectionWidth < mMinWidth) {
			selectionWidth = mMinWidth;
		}

		int width = Math.min(measuredWidth, selectionWidth);

		setMeasuredDimension(width, getMeasuredHeight());
	}

	/**
	 * Get the width of the currently selected item.
	 * 
	 * @return
	 */
	private int measureSelectionWidth() {
		/*
		 * The Spinner source code measures all of the items widths and chooses
		 * the largest. We just want to use the selected width. This code is
		 * adapted from the spinner source to only measure the selected item.
		 */
		SpinnerAdapter adapter = getAdapter();

		int selection = getSelectedItemPosition();
		if (adapter == null || selection == Spinner.INVALID_POSITION) {
			return 0;
		}

		View view = adapter.getView(selection, null, this);
		if (view.getLayoutParams() == null) {
			view.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

		int width = view.getMeasuredWidth();

		Drawable background = getBackground();
		if (background != null) {
			Rect mTempRect = new Rect();
			background.getPadding(mTempRect);
			width += mTempRect.left + mTempRect.right;
		}
		return width;
	}
}
