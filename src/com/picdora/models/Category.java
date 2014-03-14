package com.picdora.models;

import org.json.JSONException;
import org.json.JSONObject;

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
	
	@Column("icon")
	private String mPreviewImage;

	public Category(int id, String name, boolean porn, boolean nsfw, String icon) {
		super();
		mId = id;
		mName = name;
		mNsfw = nsfw;
		mPorn = porn;
		mPreviewImage = icon;
	}
	
	public Category(){
		
	}

	public Category(JSONObject jsonObject) {
		mId = -1;
		try {
			mId = jsonObject.getInt("id");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mName = "BadName";
		try {
			mName = jsonObject.getString("name");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			mNsfw = jsonObject.getBoolean("nsfw");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mPorn = jsonObject.getBoolean("porn");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mPreviewImage = jsonObject.getString("icon");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public boolean isNsfw() {
		return mNsfw;
	}

	public boolean getPorn() {
		return mPorn;
	}
	
	

	// base equals and hashcode on id
	@Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Category))
            return false;

        Category cat = (Category) obj;
        return cat.getId() == mId;
    }
    
    public int hashCode() {
        return mId;
    }

    public String getPreviewUrl() {
		return "http://i.imgur.com/" + mPreviewImage + "b.jpg";
	}

	public String getIconId() {
		return mPreviewImage;
	}

}
