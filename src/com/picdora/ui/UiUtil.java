package com.picdora.ui;

import android.content.Context;
import android.util.TypedValue;

public class UiUtil {
	private static Context mContext;
	
	public static void init(Context context){
		mContext = context;
	}
	
	public static int dpToPixel(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				mContext.getResources().getDisplayMetrics());
	}

}
