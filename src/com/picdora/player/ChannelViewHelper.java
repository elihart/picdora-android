package com.picdora.player;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.picdora.Util;
import com.picdora.models.ChannelImage.LIKE_STATUS;
import com.picdora.ui.UiUtil;

/**
 * Holds helper methods for the ChannelViewActivity in order to keep that
 * activity cleaner.
 */
@EBean
public class ChannelViewHelper {
	@RootContext
	protected ChannelViewActivity mActivity;

	/**
	 * Save the height of the device's screen in pixels as an instance variable.
	 * 
	 * @param parent
	 *            The root viewgroup, holding all other views in the activity
	 */
	@SuppressLint("NewApi")
	public void initScreenHeight(final ViewGroup parent) {
		// get the height of the actual device screen
		Display display = mActivity.getWindowManager().getDefaultDisplay();

		int result;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Point size = new Point();
			display.getSize(size);
			result = size.y;
		} else {
			// deprecated
			result = display.getHeight();
		}

		mActivity.setWindowHeight(result);

		// to be more accurate we want the height of our activity minus any
		// decorations. We can measure our root view for this, but we have to
		// wait until it is drawn
		parent.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {
						// gets called after layout has been done
						int height = parent.getHeight();
						// safety check to make sure it actually has a
						// dimension. If this fails we'll just use the original
						// full size measurement
						if (height > 0) {
							mActivity.setWindowHeight(height);
						}

						// remove the listener so it doesn't keep getting called
						if (android.os.Build.VERSION.SDK_INT >= 16) {
							parent.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
						} else {
							parent.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					}

				});
	}

	@UiThread
	public void indicateLikeStatus(LIKE_STATUS likeStatus) {
		Rect bounds = getCurrentImageBounds();
		Util.log(bounds.toString());
	}

	/**
	 * Get the absolute location of the image currently displayed image.
	 * @return
	 */
	private Rect getCurrentImageBounds() {
		ImageSwipeFragment frag = mActivity.getCurrentFragment();

		if (frag != null) {
			Rect imgBounds = frag.getImageBounds();
			
			if (imgBounds != null) {
				return imgBounds;
			}
		}

		// if we are unable to get image bounds, return the bounds for the whole layout
		ViewGroup root = mActivity.getRootView();
		return UiUtil.getPositionOnScreen(root);

	}

}
