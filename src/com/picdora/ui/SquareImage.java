package com.picdora.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImage extends ImageView {
	public SquareImage(Context context) {
		super(context);
	}

	public SquareImage(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareImage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// Snap to width
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
	}
}
