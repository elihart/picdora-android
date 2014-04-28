package com.picdora.models;

import org.json.JSONException;
import org.json.JSONObject;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.NotNull;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

import com.picdora.Util;
import com.picdora.ui.grid.Selectable;

@Table("Categories")
public class Category extends Model implements Selectable {
	/********** DB Fields ***********************/
	@PrimaryKey
	@Column("id")
	private long mId;

	@Column("name")
	@NotNull
	private String mName;

	@Column("nsfw")
	private boolean mNsfw;

	@Column("icon")
	@NotNull
	private String mPreviewImage;

	/**
	 * The last time the category was updated on the server, in unix time.
	 * 
	 */
	@Column("updated")
	private long mLastUpdated;

	public Category(int id, String name, boolean nsfw, String icon) {
		super();
		mId = id;
		mName = name;
		mNsfw = nsfw;
		mPreviewImage = icon;
	}

	public Category() {

	}

	@Override
	protected void beforeSave() {
		mLastUpdated = Util.getUnixTime();
	}

	public Category(JSONObject jsonObject) {
		try {
			mId = jsonObject.getInt("id");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mName = "BadName";
		try {
			mName = jsonObject.getString("name");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			mNsfw = jsonObject.getBoolean("nsfw");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			mPreviewImage = jsonObject.getString("icon");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			mLastUpdated = jsonObject.getLong("updated_at");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public long getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public boolean isNsfw() {
		return mNsfw;
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
		return cat.mId == mId;
	}

	@Override
	public int hashCode() {
		return (int) mId;
	}

	/**
	 * Get the imgur id of the icon to use for this category.
	 * 
	 * @return
	 */
	public String getIconId() {
		return mPreviewImage;
	}

}
