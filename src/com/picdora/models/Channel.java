package com.picdora.models;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.AutoIncrementPrimaryKey;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.Table;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.picdora.Util;

@Table("Channels")
public class Channel extends Model {
	public enum GifSetting {
		NONE, ALLOWED, ONLY
	}

	/********** DB Fields ***********************/
	@AutoIncrementPrimaryKey
	@Column("id")
	private long mId;

	@Column("name")
	private String mName;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("icon")
	private String mPreviewImage;

	// TODO: Update this on play
	@Column("lastUsed")
	private long mLastUsed;

	@Column("createAt")
	private long mCreatedAt;

	@Column("categories")
	private String mCategoriesAsJson;
	private List<Category> mCategories;

	@Column("gifSetting")
	private int mGifSetting;

	// TODO: Implement parcelable to pass this between activities

	public Channel(String name, List<Category> categories, GifSetting gifSetting) {
		mName = name;
		mCategories = categories;
		mGifSetting = gifSetting.ordinal();
	}

	// TODO: Add validations and check them before creating

	public Channel() {
		// empty constructor for Sprinkles model creation
	}

	@Override
	protected void beforeCreate() {
		mCreatedAt = System.currentTimeMillis();
		mLastUsed = System.currentTimeMillis();

		mPreviewImage = getCategories().get(0).getIconId();

		for (Category c : getCategories()) {
			if (c.isNsfw()) {
				mNsfw = true;
				break;
			}
		}
	}

	public long getId() {
		return mId;
	}

	public String getName() {
		if (mName == null) {
			return "";
		} else {
			return mName;
		}
	}

	public boolean isNsfw() {
		return mNsfw;
	}

	public GifSetting getGifSetting() {
		return GifSetting.values()[mGifSetting];
	}

	public List<Category> getCategories() {
		if (mCategories == null) {
			getCategoriesFromList();
		}
		return mCategories;
	}

	@Override
	protected void beforeSave() {
		// create a json string to represent the categories in the database
		saveCategoriesAsJson(mCategories);
	}

	private void saveCategoriesAsJson(List<Category> categories) {
		if (categories == null) {
			categories = new ArrayList<Category>();
		}

		mCategoriesAsJson = new Gson().toJson(mCategories);
	}

	/**
	 * Convert the database string of categories into a list of Category objects
	 */
	private void getCategoriesFromList() {
		if (Util.isStringBlank(mCategoriesAsJson)) {
			mCategories = new ArrayList<Category>();
		} else {
			Type type = new TypeToken<List<Category>>() {
			}.getType();
			mCategories = new Gson().fromJson(mCategoriesAsJson, type);
		}
	}

	public String getCategoriesAsJson() {
		// update the json with the current categories in case they were changed
		if (mCategories != null) {
			saveCategoriesAsJson(mCategories);
		}

		if (mCategoriesAsJson == null) {
			return "";
		} else {
			return mCategoriesAsJson;
		}
	}

	@Override
	public int hashCode() {
		return (int) mId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Channel))
			return false;

		Channel ch = (Channel) obj;

		// If either channel wasn't taken out of the database it won't have an
		// id, so compare names instead

		return ch.getId() == getId() && ch.getName().equals(getName())
				&& ch.isNsfw() == isNsfw()
				&& ch.getGifSetting() == getGifSetting()
				&& ch.getCategoriesAsJson().equals(getCategoriesAsJson());

	}

	/**
	 * 160x160 icon
	 * 
	 * @return
	 */
	public String getLargeThumbUrl() {
		return "http://i.imgur.com/" + mPreviewImage + "b.jpg";
	}

	@Override
	public String toString() {
		String result = mName + " gif: " + mGifSetting + " Categories: ";
		for (Category c : getCategories()) {
			result += c.getName() + " ";
		}
		return result;
	}

	public void setGifSetting(GifSetting gifSetting) {
		mGifSetting = gifSetting.ordinal();

	}

	public void setLastUsed(Date date) {
		mLastUsed = date.getTime();		
	}
	
	public Date getLastUsed(){
		return new Date(mLastUsed);
	}
	
	public Date getCreatedAt(){
		return new Date(mCreatedAt);
	}

	public void setName(String name) {
		mName = name;		
	}

	public void setCategories(List<Category> categories) {
		mCategories = categories;		
	}

}
