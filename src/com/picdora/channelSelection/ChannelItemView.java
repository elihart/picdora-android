package com.picdora.channelSelection;

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
import com.picdora.models.Channel;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;

@EViewGroup(R.layout.activity_channel_selection_item)
public class ChannelItemView extends RelativeLayout {
	@ViewById
	TextView channelName;
	@ViewById
	ImageView channelImage;

	public ChannelItemView(Context context) {
		super(context);
	}

	public void bind(Channel channel) {
		channelName.setText(channel.getName());
		FontHelper.setTypeFace(channelName, STYLE.MEDIUM);

		tryLoadUrl(channel.getPreviewUrl());

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
				if (channelImage != null) {
					channelImage.setImageBitmap(loadedImage);
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
		if (channelImage != null) {
			numAttempts++;
			if (numAttempts < MAX_ATTEMPTS) {
				tryLoadUrl(url);
			}
		}
	}

}
