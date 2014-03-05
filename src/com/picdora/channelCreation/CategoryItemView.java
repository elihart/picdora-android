package com.picdora.channelCreation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;

@EViewGroup(R.layout.grid_item_image_and_text)
public class CategoryItemView extends RelativeLayout{
	@ViewById
	TextView text;
	@ViewById
	ImageView image;
	
	public CategoryItemView(Context context) {
		super(context);
	}
	
	public void bind(String text, String url, boolean highlight) {
		this.text.setText(text);
		FontHelper.setTypeFace(this.text, STYLE.MEDIUM);
		
		Util.setImageHighlight(image, highlight);

		tryLoadUrl(url);
    }	
	
	private int numAttempts = 0;
	private static final int MAX_ATTEMPTS = 3;

	private void tryLoadUrl(final String url) {
		ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {

			@Override
			public void onLoadingFailed(String imageUri, View view,
					FailReason failReason) {
				handleLoadFailure(url);
			}

			@Override
			public void onLoadingComplete(String imageUri, View view,
					Bitmap loadedImage) {
				if (image != null) {
					image.setImageBitmap(loadedImage);
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

	private void handleLoadFailure(String url) {
		if (image != null) {
			numAttempts++;
			if (numAttempts < MAX_ATTEMPTS) {
				tryLoadUrl(url);
			}
		}
	}


}
