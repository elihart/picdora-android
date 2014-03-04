package com.picdora.channelSelection;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ChannelGridImage extends ImageView {
	public ChannelGridImage(Context context) {
		super(context);
	}

	public ChannelGridImage(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChannelGridImage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// Snap to width
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
	}
}
