package com.picdora.models;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.Model;
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
	@PrimaryKey
	@Column("id")
	private int mId;

	@Column("name")
	private String mName;

	@Column("nsfw")
	private String mNsfw;

	@Column("categories")
	private String mCategoriesAsJson;
	private List<Category> mCategories;
	
	@Column("gifSetting")
	private int mGifSetting;
	
	public Channel(int id, String name, List<Category> categories, GifSetting gifSetting) {
		super();
		mId = id;
		mName = name;
		mCategories = categories;
		mGifSetting = gifSetting.ordinal();
		
		// TODO: Set nsfw based on categories
	}

	public Channel() {
		// empty constructor for Sprinkles model creation
	}
	

	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public String getNsfw() {
		return mNsfw;
	}
	
	public GifSetting getGifSetting(){
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

}
