package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

/**
 * A many to many relationship of categories that are in a channel.
 * 
 * Each combination is unique, so a channel can only list a specific category
 * once.
 */
@Table("ChannelCategories")
public class ChannelCategory extends Model {
	@PrimaryKey
	@Column("categoryId")
	private long mCategoryId;

	@PrimaryKey
	@Column("channelId")
	private long mChannelId;

	public ChannelCategory() {
		// empty constructor for sprinkles
	}

	/**
	 * Save a category to a channel. The channel cannot have duplicate
	 * categories.
	 * 
	 * @param category
	 * @param channel
	 */
	public ChannelCategory(Category category, Channel channel) {
		if (category.getId() < 1) {
			throw new IllegalArgumentException("Category does not have id");
		}

		else if (channel.getId() < 1) {
			throw new IllegalArgumentException("Channel does not have id");
		}

		mCategoryId = category.getId();
		mChannelId = channel.getId();
	}
}
