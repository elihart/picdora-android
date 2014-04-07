package com.picdora.models;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.NotNull;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

import com.picdora.ImageUtils;
import com.picdora.ImageUtils.ImgurSize;

@Table("Images")
public class Image extends Model {
	/********** DB Fields ***********************/
	/**
	 * Default image id for an image that hasn't been saved to the db yet. This
	 * is a number that won't conflict with saved image ids
	 */
	public static final int UNSAVED_IMAGE_ID = -1;

	@PrimaryKey
	@Column("id")
	private long mId = UNSAVED_IMAGE_ID;

	@Column("imgurId")
	@NotNull
	private String mImgurId;

	@Column("redditScore")
	@NotNull
	private int mRedditScore;

	@Column("categoryId")
	@NotNull
	private int mCategoryId;

	@Column("deleted")
	private boolean mDeleted;

	@Column("reported")
	private boolean mReported;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("gif")
	private boolean mGif;

	/****************************************************/

	public Image() {

	}

	/**
	 * Create a image
	 * 
	 * @param id
	 * @param imgurId
	 * @param redditScore
	 * @param categoryId
	 * @param nsfw
	 * @param porn
	 * @param gif
	 */
	public Image(int id, String imgurId, int redditScore, int categoryId,
			boolean nsfw, boolean gif) {
		this.mId = id;
		this.mImgurId = imgurId;
		this.mRedditScore = redditScore;
		this.mCategoryId = categoryId;
		this.mNsfw = nsfw;
		this.mGif = gif;
	}

	public Image(JSONObject obj) {
		mId = -1;
		try {
			mId = obj.getInt("id");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mImgurId = "badId";
		try {
			mImgurId = obj.getString("imgurId");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mRedditScore = -1;
		try {
			mRedditScore = obj.getInt("reddit_score");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mCategoryId = -1;
		try {
			mCategoryId = obj.getInt("category_id");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mNsfw = false;
		try {
			mNsfw = obj.getBoolean("nsfw");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mGif = false;
		try {
			mGif = obj.getBoolean("gif");
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	/******************* Getters ***************/
	public long getId() {
		return mId;
	}

	public String getImgurId() {
		return mImgurId;
	}

	public int getRedditScore() {
		return mRedditScore;
	}

	public int getCategoryId() {
		return mCategoryId;
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
		return mImgurId.toLowerCase(Locale.US).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Image))
			return false;

		Image img = (Image) obj;
		return img.getImgurId().equalsIgnoreCase(mImgurId);
	}

}
