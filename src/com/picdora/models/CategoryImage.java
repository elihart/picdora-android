package com.picdora.models;

import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.annotations.CascadeDelete;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.ForeignKey;
import se.emilsjolander.sprinkles.annotations.PrimaryKey;
import se.emilsjolander.sprinkles.annotations.Table;

/**
 * A many to many relationship of images tagged with categories. Each
 * combination is unique, so a image can only list a specific category once.
 */
@Table("CategoryImages")
public class CategoryImage extends Model {
	@PrimaryKey
	@ForeignKey("Categories(id)")
	@CascadeDelete
	@Column("categoryId")
	private long mCategoryId;

	@PrimaryKey
	@ForeignKey("Images(id)")
	@CascadeDelete
	@Column("imageId")
	private long mImageId;

	public CategoryImage() {
		// empty constructor for sprinkles
	}

	/**
	 * Tag an image with a category. The image cannot have duplicate categories.
	 * 
	 * @param category
	 * @param image
	 */
	public CategoryImage(Category category, Image image) {
		if (category.getId() < 1) {
			throw new IllegalArgumentException("Category does not have id");
		}

		else if (image.getId() < 1) {
			throw new IllegalArgumentException("Image does not have id");
		}

		mCategoryId = category.getId();
		mImageId = image.getId();
	}
}
