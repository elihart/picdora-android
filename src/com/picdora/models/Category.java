package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

@Table("Categories")
public class Category extends Model {
	/********** DB Fields ***********************/
	@PrimaryKey
	@Column("id")
	private int mId;

	@Column("name")
	private String mName;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("porn")
	private boolean mPorn;

	public Category(int id, String name, boolean porn, boolean nsfw) {
		super();
		mId = id;
		mName = name;
		mNsfw = nsfw;
		mPorn = porn;
	}
	
	public Category(){
		
	}

	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public boolean getNsfw() {
		return mNsfw;
	}

	public boolean getPorn() {
		return mPorn;
	}

}
