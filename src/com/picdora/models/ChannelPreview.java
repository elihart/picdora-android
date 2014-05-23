package com.picdora.models;

import java.util.List;

/**
 * A functioning channel that will not save anything to the database.
 */
public class ChannelPreview extends Channel {
	// Use a negative id to not interfere with the autoincrementing primary key
	// for normal channels
	private static final int PREVIEW_ID = -2;

	// ChannelImages won't save since the channel isn't saved, so the user will
	// keep seeing the same initial images repeated. Should be enough for a
	// preview though

	// TODO: Better way to handle channel previews

	/**
	 * Create a channel with a preassigned id. In most cases this should not be
	 * used, and you should let an id be generated for you.
	 * 
	 * @param id
	 * @param name
	 * @param categories
	 * @param gifSetting
	 */
	public ChannelPreview(List<Category> categories, GifSetting gifSetting) {
		super("preview", gifSetting, categories);
		mId = PREVIEW_ID;
	}

	@Override
	public boolean isValid() {
		// never save this model
		return false;
	}

	public static boolean isPreview(Channel channel) {
		return (channel instanceof ChannelPreview);
	}
}
