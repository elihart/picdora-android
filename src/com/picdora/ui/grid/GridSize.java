package com.picdora.ui.grid;

import com.picdora.ImageUtils.ImgurSize;

/**
 * The size of a grid row. This includes the width of the row and the
 * {@link #ImgurSize} image suggested for matching the width.
 * 
 */
public enum GridSize {
	TINY("Tiny", ImgurSize.SMALL_SQUARE), SMALL("Small", ImgurSize.BIG_SQUARE), MEDIUM("Medium", 
			ImgurSize.MEDIUM_THUMBNAIL), LARGE("Large", ImgurSize.LARGE_THUMBNAIL);

	private ImgurSize size;
	private String name;

	private GridSize(String name, ImgurSize size) {
		this.size = size;
		this.name = name;
	}

	/** The imgur size suggested for an image that will fit this grid size */
	public ImgurSize getImageSize() {
		return size;
	}

	/** The row width of this grid size in pixels */
	public int getRowWidth(){
		return size.getSize();
	}
	
	/** Get the name of this size */
	public String getName(){
		return name;
	}
	
}
