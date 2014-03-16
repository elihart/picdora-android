package com.picdora.models;

import java.util.Date;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.AutoIncrementPrimaryKey;
import se.emilsjolander.sprinkles.annotations.CascadeDelete;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.ForeignKey;
import se.emilsjolander.sprinkles.annotations.NotNull;
import se.emilsjolander.sprinkles.annotations.Table;

/**
 * Holds the data for each image that is shown in a channel. Since multiple
 * images can point to the same imgur picture this tracks the imgur id instead
 * of a specific image id. Each imgur id should have only a single entry for
 * each Channel, and repeat views should increment the view count and change the
 * last viewed time. We also track whether or not the image was liked or not.
 */
@Table("Views")
public class ChannelImage extends Model {
	/********** DB Fields ***********************/
	@AutoIncrementPrimaryKey
	@Column("id")
	private long mId;

	@ForeignKey("Channels(id)")
	@CascadeDelete
	@NotNull
	@Column("channelId")
	private long mChannelId;

	@Column("image")
	@NotNull
	private String mImgurId;
	private Image mImage;

	@Column("count")
	private int mViewCount;

	@Column("lastSeen")
	private long mLastSeen;

	@Column("liked")
	@NotNull
	private int mLikeStatus;

	/****************************************************/

	/*
	 * The image can be liked or disliked by the user. It's default state is
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

	public ChannelImage() {
	}

	/**
	 * Create an image with 1 view, neutral status, and last seen at the current time
	 * 
	 * @param channel
	 * @param image
	 */
	public ChannelImage(Channel channel, Image image) {
		mChannelId = channel.getId();
		mImgurId = image.getImgurId();
		mImage = image;

		mViewCount = 1;
		mLastSeen = new Date().getTime();
		mLikeStatus = LIKE_STATUS.NEUTRAL.id;
	}

	public String getImgurId() {
		return mImgurId;
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

}
