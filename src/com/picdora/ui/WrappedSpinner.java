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

	public WrappedSpinner(Context context) {
		super(context);
	}

	public WrappedSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WrappedSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
			final int measuredWidth = getMeasuredWidth();
			
			int content = measureContentWidth();
			if(content <= 0){
				content = measuredWidth;
			}

			int width = Math.min(measuredWidth, content);

			setMeasuredDimension(width, getMeasuredHeight());
		}

	}

	private int measureContentWidth() {
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
