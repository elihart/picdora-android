package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

import com.picdora.ImageUtils;
import com.picdora.ImageUtils.ImgurSize;
import com.picdora.ui.grid.Selectable;

/**
 * Represents an image that we can show a user. It is based on an imgur image,
 * so we keep track of the imgur id and details of the image. Images shouldn't
 * be created locally, they should only be inserted based on data from the
 * server. The imgurId should be unique, but we'll let the server handle
 * enforcing so that local processing can be faster.
 * 
 */
@Table("Images")
public class Image extends Model implements Selectable {
	/********** DB Fields ***********************/

	/*
	 * TODO: Add indices to speed up db accesses. We should probably index
	 * redditScore at least, but maybe should do more. Almost all of our
	 * interaction with the images is reads (bulk writes happen in the
	 * background and can be slower I think), so indexing everything might help.
	 */

	@PrimaryKey
	@Column("id")
	private long mId;

	@Column("imgurId")
	private String mImgurId;

	@Column("redditScore")
	private int mRedditScore;

	@Column("deleted")
	private boolean mDeleted;

	@Column("reported")
	private boolean mReported;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("gif")
	private boolean mGif;

	/** The date the image was last updated on the server in unix time. */
	@Column("lastUpdated")
	private long mLastUpdated;

	/** The date the image was created on the server in unix time. */
	@Column("createdAt")
	private long mCreatedAt;

	/****************************************************/

	/** Blank constructor for sprinkles. */
	public Image() {
		/* Blank constructor for sprinkles. */
	}

	/******************* Getters ***************/
	public long getId() {
		return mId;
	}

	/**
	 * Get the imgurId of this image.
	 * 
	 * @return
	 */
	public String getImgurId() {
		return mImgurId;
	}

	public int getRedditScore() {
		return mRedditScore;
	}

	public boolean isNsfw() {
		return mNsfw;
	}

	public boolean isGif() {
		return mGif;
	}

	public boolean isReported() {
		return mReported;
	}

	public boolean isDeleted() {
		return mDeleted;
	}

	/***************** Setters **********************/

	public void setGif(boolean gif) {
		mGif = true;
	}

	public void setDeleted(boolean deleted) {
		mDeleted = deleted;
	}

	public void setReported(boolean reported) {
		mReported = reported;
	}

	/********** Public Helper Methods **************/

	/**
	 * Get the url where this image is stored
	 * 
	 * @param size
	 *            The size of the image the url should point to
	 * @return
	 */
	public String getUrl(ImgurSize size) {
		return ImageUtils.getImgurLink(this, size);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mId ^ (mId >>> 32));
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
		if (!(obj instanceof Image)) {
			return false;
		}
		Image other = (Image) obj;
		if (mId != other.mId) {
			return false;
		}
		return true;
	}

	/*
	 * Implement these methods for the Selectable interface so Images can be
	 * used in a selection grid
	 */

	/**
	 * Used for Selectable interface for gallery fragment. Same thing as
	 * imgurId.
	 * 
	 */
	@Override
	public String getIconId() {
		return mImgurId;
	}

	@Override
	public String getName() {
		return null;
	}
	/* ************************************************************************************** */

}
