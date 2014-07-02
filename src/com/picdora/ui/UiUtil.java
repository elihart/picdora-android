package com.picdora.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
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

	/**
	 * Scale the alpha level of the given color. Preserves all rgb values and
	 * only changes alpha.
	 * 
	 * @param color
	 *            The color to adjust
	 * @param factor
	 *            Alpha value from 0 to 1. The current alpha level will be
	 *            scaled by this amount
	 * @return
	 */
	public static int adjustAlpha(int color, float factor) {
		int alpha = Math.round(Color.alpha(color) * factor);
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		return Color.argb(alpha, red, green, blue);
	}

	/**
	 * Determines if the given event happened inside the view
	 * 
	 * @param ev
	 *            - The motion event to check
	 * @param view
	 *            - view object to compare
	 * @return true if the event points are within view bounds, false otherwise
	 */
	public static boolean isEventInsideView(MotionEvent ev, View view) {
		float x = ev.getRawX();
		float y = ev.getRawY();

		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		// point is inside view bounds
		if ((x > viewX && x < (viewX + view.getWidth()))
				&& (y > viewY && y < (viewY + view.getHeight()))) {
			return true;
		} else {
			return false;
		}
	}

}
