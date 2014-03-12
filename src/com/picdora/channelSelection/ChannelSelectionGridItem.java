package com.picdora.channelSelection;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.picdora.R;
import com.picdora.ui.PicdoraGridItem;

@EViewGroup
public class ChannelSelectionGridItem extends PicdoraGridItem {
	@ViewById
	protected LinearLayout buttonContainer;

	public ChannelSelectionGridItem(Context context) {
		super(context);

		// Add menu buttons on top of the grid item
		LayoutInflater.from(context).inflate(
				R.layout.channel_selection_grid_item, this);
	}

	@Override
	public void bind(String text, String url, boolean highlight) {
		super.bind(text, url, highlight);

		showButtons(false);
	}

	public void showButtons(boolean show) {
		if (show) {
			buttonContainer.setVisibility(View.VISIBLE);
		} else {
			buttonContainer.setVisibility(View.GONE);
		}
	}

}
