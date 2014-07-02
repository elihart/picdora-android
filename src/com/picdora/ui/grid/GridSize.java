package com.picdora.ui.grid;

import com.picdora.ImageUtil.ImgurSize;
import com.picdora.ui.UiUtil;

/**
 * Sizes to use for an image grid. This includes the width of the column and the
 * {@link #ImgurSize} image suggested for matching that width..
 * 
 */
public enum GridSize {
	TINY("Tiny", ImgurSize.SMALL_SQUARE, 50), SMALL("Small",
			ImgurSize.BIG_SQUARE, 95), MEDIUM("Medium",
			ImgurSize.MEDIUM_THUMBNAIL, 140), LARGE("Large",
			ImgurSize.LARGE_THUMBNAIL, 300);

	/*
	 * TODO: Scale recommended imgur size based column dp, since imgur size is
	 * pixels/static and dp is dynamic to screen. Also, maybe put more thought
	 * into what column widths should be.
	 */

	private ImgurSize imageSize;
	private String name;
	/** Column size in dp. */
	private int columnSize;

	private GridSize(String name, ImgurSize size, int columnSize) {
		this.imageSize = size;
		this.name = name;
		this.columnSize = columnSize;
	}

	/** The imgur size suggested for an image that will fit this grid size */
	public ImgurSize getImageSize() {
		return imageSize;
	}

	/** The column width of the grid in dp. */
	public int getColumnWidth() {
		return columnSize;
	}

	/** Get the name of this size */
	public String getName() {
		return name;
	}

}
