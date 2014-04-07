package com.picdora.channelPlayer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * PhotoView library has a problem where it will sometimes get array out of 
 * bounds exceptions. We have to catch them and ignore them.
 *
 */

public class PicdoraViewPager extends ViewPager {

    public PicdoraViewPager(Context context) {
        super(context);
    }

    public PicdoraViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

}
