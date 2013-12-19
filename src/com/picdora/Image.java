package com.picdora;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.*;

@Table("Images")
public class Image extends Model {
	/********** DB Fields ***********************/
	@PrimaryKey
	@Column("id")
	private int mId;

	@Column("imgurId")
	private String mImgurId;

	@Column("redditScore")
	private int mRedditScore;

	@Column("categoryId")
	private int mCategoryId;

	@Column("albumId")
	private int mAlbumId;

	@Column("reported")
	private boolean mReported = false;

	@Column("deleted")
	private boolean mDeleted = false;;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("porn")
	private boolean mPorn;

	@Column("gif")
	private boolean mGif;

	@Column("landscape")
	private boolean mLandscape;

	@Column("liked")
	private boolean mLiked = false;;

	@Column("favorite")
	private boolean mFavorite = false;

	@Column("viewCount")
	private int mViewCount = 0;

	@Column("lastViewed")
	private long mLastViewed = 0;

	/****************************************************/
	
	public Image(){
		
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
			boolean nsfw, boolean porn, boolean gif) {
		this.mId = id;
		this.mImgurId = imgurId;
		this.mRedditScore = redditScore;
		this.mCategoryId = categoryId;
		this.mNsfw = nsfw;
		this.mPorn = porn;
		this.mGif = gif;
	}

	public Image(JSONObject obj) {
		mId = -1;
		try {
			mId = obj.getInt("id");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mImgurId = "badId";
		try {
			mImgurId = obj.getString("imgurId");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mRedditScore = -1;
		try {
			mRedditScore = obj.getInt("reddit_score");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mCategoryId = -1;
		try {
			mCategoryId = obj.getInt("category_id");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mNsfw = false;
		try {
			mNsfw = obj.getBoolean("nsfw");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mPorn = false;
		try {
			mPorn = obj.getBoolean("porn");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mGif = false;
		try {
			mGif = obj.getBoolean("gif");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/******************* Getters ***************/
	public int getId() {
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

	public int getAlbumId() {
		return mAlbumId;
	}

	public boolean isReported() {
		return mReported;
	}

	public boolean isDeleted() {
		return mDeleted;
	}

	public boolean isNsfw() {
		return mNsfw;
	}

	public boolean isPorn() {
		return mPorn;
	}

	public boolean isGif() {
		return mGif;
	}

	public boolean isLandscape() {
		return mLandscape;
	}

	public boolean isLiked() {
		return mLiked;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	public int getViewCount() {
		return mViewCount;
	}

	public long getLastViewed() {
		return mLastViewed;
	}

	/***************** Setters **********************/

	public void setLiked(boolean liked) {
		mLiked = liked;
		save();
	}

	public void setLandscape(boolean landscape) {
		mLandscape = landscape;
		save();
	}

	public void setFavorite(boolean favorite) {
		mFavorite = favorite;
		save();
	}

	public void setDeleted(boolean deleted) {
		mDeleted = deleted;
		save();
	}

	public void setReported(boolean reported) {
		mReported = reported;
		save();
	}

	public void markView() {
		mViewCount++;
		mLastViewed = new Date().getTime();
		save();
	}

	/********** Public Helper Methods **************/

	/**
	 * Get the url where this image is stored
	 * 
	 * @return
	 */
	public String getUrl() {
			return "http://imgur.com/" + mImgurId + ".jpg";		
	}
}
