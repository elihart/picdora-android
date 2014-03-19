package com.picdora.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

public class UiUtil {
	private static Context mContext;

	public static void init(Context context) {
		mContext = context;
	}

	public static int dpToPixel(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				mContext.getResources().getDisplayMetrics());
	}

	/**
	 * Get the absolute coordinates of the view as it's drawn on screen
	 * 
	 * @param v
	 * @return
	 */
	public static Rect getPositionOnScreen(View v) {
		int[] location = new int[2];
		v.getLocationOnScreen(location);

		int x = location[0];
		int y = location[1];

		int width = v.getWidth();
		int height = v.getHeight();

		Rect r = new Rect(x, y, x + width, y + height);

		return r;
	}

	/**
	 * Create a new Rect from a RectF, rounding the floats to the nearest int.
	 * 
	 * @param rect
	 * @return
	 */
	public static Rect rect(RectF r) {
		return new Rect(Math.round(r.left), Math.round(r.top),
				Math.round(r.right), Math.round(r.bottom));
	}

}
