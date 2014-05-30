package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.CascadeDelete;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.ForeignKey;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

@Table(CollectionItem.TABLE_NAME)
public class CollectionItem extends Model {
	public static final String TABLE_NAME = "CollectionItems";

	/********** DB Fields ***********************/
	@PrimaryKey
	@Column("imageId")
	protected long mImageId;

	@PrimaryKey
	@ForeignKey(Collection.TABLE_NAME + "(id)")
	@CascadeDelete
	@Column("collectionId")
	protected long mCollectionId;

	@Column("lastUsed")
	protected long mLastUsed;

	@Column("createdAt")
	protected long mCreatedAt;

	public CollectionItem() {
		// empty constructor for sprinkles
	}

	/**
	 * Add an image to a collection. The collection must be saved in the db, and
	 * the image should not already be added to it.
	 * 
	 * @param collection
	 * @param image
	 */
	public CollectionItem(Collection collection, Image image) {
		if (collection.getId() < 1) {
			throw new IllegalArgumentException("Collection doesn't have id");
		}
		if (image.getId() < 1) {
			throw new IllegalArgumentException("Image doesn't have id");
		}

		mImageId = image.getId();
		mCollectionId = collection.getId();
	}

	@Override
	protected void beforeCreate() {
		mCreatedAt = System.currentTimeMillis();
		mLastUsed = System.currentTimeMillis();
	}

	@Override
	public boolean isValid() {
		/*
		 * Both the image and collection need to be saved to the database and
		 * have an id, otherwise we won't be able to accurately reference them.
		 */
		if (mImageId < 1 || mCollectionId < 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (mCollectionId ^ (mCollectionId >>> 32));
		result = prime * result + (int) (mImageId ^ (mImageId >>> 32));
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
		if (!(obj instanceof CollectionItem)) {
			return false;
		}
		CollectionItem other = (CollectionItem) obj;
		if (mCollectionId != other.mCollectionId) {
			return false;
		}
		if (mImageId != other.mImageId) {
			return false;
		}
		return true;
	}

}
