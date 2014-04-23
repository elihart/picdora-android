package com.picdora.models;

import java.util.Date;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.CascadeDelete;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.ForeignKey;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

/**
 * Holds the data for each image that is shown in a channel. Each image should
 * have only a single entry for each Channel, and repeat views should increment
 * the view count and change the last viewed time. We also track whether or not
 * the image was liked or not.
 */
@Table("Views")
public class ChannelImage extends Model {
	/********** DB Fields ***********************/

	@PrimaryKey
	@ForeignKey("Channels(id)")
	@CascadeDelete
	@Column("channelId")
	private long mChannelId;

	@PrimaryKey
	@Column("imageId")
	private long mImageId;
	private Image mImage;

	@Column("count")
	private int mViewCount;

	@Column("lastSeen")
	private long mLastSeen;

	@Column("liked")
	private int mLikeStatus;

	/***************************************************
	 * /* The image can be liked or disliked by the user. It's default state is
	 * neutral until the user rates it. Use constant ints to hold the value of
	 * the ENUM fields so we can easily store them in the db, but convert the
	 * ints to the ENUM value when giving out the status externally.
	 */

	private static final int STATUS_NEUTRAL = 0;
	private static final int STATUS_LIKED = 1;
	private static final int STATUS_DISLIKED = 2;

	public enum LIKE_STATUS {
		NEUTRAL(STATUS_NEUTRAL), LIKED(STATUS_LIKED), DISLIKED(STATUS_DISLIKED);

		private int id;

		private LIKE_STATUS(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	/**
	 * Empty constructor for sprinkles. Don't use!
	 */
	public ChannelImage() {
		/* Empty constructor for sprinkles. */
	}

	/**
	 * Create an image with 1 view, neutral status, and last seen at the current
	 * time
	 * 
	 * @param channel
	 * @param image
	 */
	public ChannelImage(Channel channel, Image image) {
		if (channel == null) {
			throw new IllegalArgumentException("Channel can't be null");
		}
		if (channel.getId() < 1) {
			throw new IllegalArgumentException("Channel doesn't have an id");
		}
		if (image == null) {
			throw new IllegalArgumentException("Image can't be null");
		}
		if (image.getId() < 1) {
			throw new IllegalArgumentException("Image doesn't have an id.");
		}

		mChannelId = channel.getId();
		mImageId = image.getId();
		mImage = image;

		/* Init to not yet seen and neutral like status. */
		mViewCount = 0;
		mLastSeen = 0;
		mLikeStatus = LIKE_STATUS.NEUTRAL.getId();
	}

	public long getImageId() {
		return mImageId;
	}

	public int getViewCount() {
		return mViewCount;
	}

	/**
	 * Increment the view count and update last seen to now.
	 */
	public void markView() {
		mViewCount++;
		mLastSeen = new Date().getTime();
	}

	public long getLastSeen() {
		return mLastSeen;
	}

	public LIKE_STATUS getLikeStatus() {
		switch (mLikeStatus) {
		case STATUS_DISLIKED:
			return LIKE_STATUS.DISLIKED;
		case STATUS_LIKED:
			return LIKE_STATUS.LIKED;
		case STATUS_NEUTRAL:
			return LIKE_STATUS.NEUTRAL;
		default:
			return LIKE_STATUS.NEUTRAL;
		}
	}

	public void setLikeStatus(LIKE_STATUS status) {
		mLikeStatus = status.getId();
	}

	public Image getImage() {
		// TODO: Get image from db if necessary
		return mImage;
	}

	public int getChannelId() {
		return (int) mChannelId;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mChannelId ^ (mChannelId >>> 32));
		result = prime * result + (int) (mImageId ^ (mImageId >>> 32));
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ChannelImage)) {
			return false;
		}
		ChannelImage other = (ChannelImage) obj;
		if (mChannelId != other.mChannelId) {
			return false;
		}
		if (mImageId != other.mImageId) {
			return false;
		}
		return true;
	}

}
