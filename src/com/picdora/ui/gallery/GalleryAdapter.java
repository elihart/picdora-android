package com.picdora.ui.gallery;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;

import com.picdora.models.Image;
import com.picdora.ui.grid.ModelGridAdapter;

/**
 * A specialized {@link #ModelGridAdapter} that displays the picture represented by an Image model with no text overlaid on top of it. 
 */
@EBean
public class GalleryAdapter extends ModelGridAdapter<Image> {
	
	@AfterInject
	protected void init(){
		// Only show the image
		mShowText = false;
	}

	@Override
	public long getItemId(int position) {
		return getItems().get(position).getId();
	}

	@Override
	protected String getText(Image item) {
		// Just show the image
		return null;
	}

	@Override
	protected String getImgurId(Image item) {
		return item.getImgurId();
	}
}
