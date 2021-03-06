package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.AutoIncrementPrimaryKey;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.NotNull;
import se.emilsjolander.sprinkles.annotations.Table;
import se.emilsjolander.sprinkles.annotations.Unique;

import com.picdora.Util;
import com.picdora.ui.grid.Selectable;

@Table(Collection.TABLE_NAME)
public class Collection extends Model implements Selectable {
	public static final String TABLE_NAME = "Collections";

	/********** DB Fields ***********************/
	@AutoIncrementPrimaryKey
	@Column("id")
	protected long mId;

	@Column("name")
	@Unique
	@NotNull
	protected String mName;

	@Column("nsfw")
	protected boolean mNsfw;

	@Column("icon")
	@NotNull
	protected String mPreviewImgurId = "";
	// TODO: Find default image for collection and upload to imgur.

	@Column("lastUsed")
	protected long mLastUsed;

	@Column("createdAt")
	protected long mCreatedAt;

	public Collection() {
		// empty constructor for sprinkles
	}

	public Collection(String name) {
		mName = name;
	}

	@Override
	protected void beforeCreate() {
		mCreatedAt = System.currentTimeMillis();
		mLastUsed = System.currentTimeMillis();
	}

	@Override
	public boolean isValid() {
		if (Util.isStringBlank(mName)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public int hashCode() {
		/* Use a hash of the collection name, or 0 if a name hasn't been set */
		if (Util.isStringBlank(mName)) {
			return 0;
		} else {
			return mName.hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Collection))
			return false;

		Collection c = (Collection) obj;

		/*
		 * If the id is not zero then it has been saved to the database and is
		 * guaranteed to have a uniqueid so we can just compare ids.
		 */
		if (c.mId == mId && mId != 0) {
			return true;
		}
		/*
		 * Otherwise if both names aren't null and are equal then the
		 * collections are the same.
		 */
		else if (mName != null && c.mName != null && mName.equals(c.mName)) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * The collection id.
	 * 
	 */
	public long getId() {
		return mId;
	}

	/**
	 * The icon representing the collection.
	 * 
	 */
	public String getIconId() {
		return mPreviewImgurId;
	}

	/**
	 * The name of the collection.
	 * 
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Set the given image as the preview icon for this collection.
	 * 
	 * @param image
	 */
	public void setIcon(Image image) {
		mPreviewImgurId = image.getImgurId();
	}

}
