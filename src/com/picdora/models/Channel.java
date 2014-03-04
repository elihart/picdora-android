package com.picdora.models;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.AutoIncrementPrimaryKey;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
	private String mNsfw;

	// TODO: Add this field to the db and update on channel creation, and
	// favoriting
	private String mPreviewImage = "Z9kkH2r";

	@Column("categories")
	private String mCategoriesAsJson;
	private List<Category> mCategories;

	@Column("gifSetting")
	private int mGifSetting;

	public Channel(String name, List<Category> categories, GifSetting gifSetting) {
		mName = name;
		mCategories = categories;
		mGifSetting = gifSetting.ordinal();

		// TODO: Validate non null/empty values. Unique name.

		// TODO: Set nsfw based on categories
	}

	public Channel() {
		// empty constructor for Sprinkles model creation
	}

	public long getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public String getNsfw() {
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
		if (mCategories == null) {
			mCategories = new ArrayList<Category>();
		}

		mCategoriesAsJson = new Gson().toJson(mCategories);
	}

	/**
	 * Convert the database string of categories into a list of Category objects
	 */
	private void getCategoriesFromList() {
		Type type = new TypeToken<List<Category>>() {
		}.getType();
		mCategories = new Gson().fromJson(mCategoriesAsJson, type);
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

		if (ch.getId() == 0 && mId == 0) {
			return ch.getName().equals(mName);
		} else {
			return ch.getId() == mId;
		}
	}

	public String getPreviewUrl() {
		return "http://i.imgur.com/" + mPreviewImage + "b.jpg";
	}

}
