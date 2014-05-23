package com.picdora.models;

import java.util.Date;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;
import se.emilsjolander.sprinkles.annotations.AutoIncrementPrimaryKey;
import se.emilsjolander.sprinkles.annotations.Column;
import se.emilsjolander.sprinkles.annotations.NotNull;
import se.emilsjolander.sprinkles.annotations.Table;
import android.database.sqlite.SQLiteDatabase;

import com.picdora.Util;
import com.picdora.ui.grid.Selectable;

@Table("Channels")
public class Channel extends Model implements Selectable {
	/*
	 * TODO: What happens to liked ChannelImages when the channel is deleted? We
	 * may want to save them, or ask the user if they still want them. Maybe add
	 * a delete flag and never fully remove the channel. This could help not
	 * show duplicate images in the future, and keep the liking history for
	 * future reference.
	 */

	public enum GifSetting {
		NONE, ALLOWED, ONLY
	}

	/********** DB Fields ***********************/
	@AutoIncrementPrimaryKey
	@Column("id")
	protected long mId;

	@Column("name")
	@NotNull
	protected String mName;

	@Column("nsfw")
	protected boolean mNsfw;

	@Column("icon")
	@NotNull
	protected String mPreviewImage;

	/** The unix time that the channel was last played */
	@Column("lastUsed")
	protected long mLastUsed;

	/** The unix time that the channel was created. */
	@Column("createdAt")
	protected long mCreatedAt;

	protected List<Category> mCategories;

	@Column("gifSetting")
	@NotNull
	protected int mGifSetting;

	// TODO: Implement parcelable to pass this between activities

	/**
	 * Create a new channel with a unique name. The channel must be first saved
	 * to the database before categories can be added.
	 * 
	 * @param name
	 * @param gifSetting
	 */
	public Channel(String name, GifSetting gifSetting, List<Category> categories) {
		if (Util.isStringBlank(name)) {
			throw new IllegalArgumentException("Name can't be blank");
		}
		
		setCategories(categories);

		mName = name;
		mGifSetting = gifSetting.ordinal();
	}

	@Override
	public boolean isValid() {
		if (Util.isStringBlank(mName)) {
			return false;
		} else {
			return true;
		}
	}

	public Channel() {
		// empty constructor for Sprinkles model creation
	}

	@Override
	protected void beforeCreate() {
		mCreatedAt = Util.getUnixTime();
		mLastUsed = Util.getUnixTime();
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

	public GifSetting getGifSetting() {
		return GifSetting.values()[mGifSetting];
	}

	/**
	 * Get the categories set to this channel. May do a db access to load the
	 * categories, so there is potential for a synchronous db access.
	 * 
	 * @return
	 */
	public List<Category> getCategories() {
		if (mCategories == null) {
			getCategoriesFromDb();
		}
		return mCategories;
	}

	/**
	 * Clear all categories associated with this channel in preparation for
	 * setting new ones.
	 * 
	 */
	private void clearChannelCategories() {
		SQLiteDatabase db = Sprinkles.getDatabase();

		String query = "DELETE from ChannelCategories WHERE channelId=" + mId;

		db.execSQL(query);
	}

	/**
	 * Get the categories that characterize this channel from the db. Does a
	 * synchronous db access!
	 * 
	 */
	private List<Category> getCategoriesFromDb() {
		/* Get all the categories saved to this channel. */
		String query = "select * from Categories where id in "
				+ "(select categoryId from ChannelCategories where channelId=?)";

		CursorList<Category> result = Query.many(Category.class, query, mId)
				.get();
		List<Category> categories = result.asList();
		result.close();

		return categories;
	}

	/**
	 * Save the categories that characterize this channel to the db. Does a
	 * synchronous db access! A regular save() of a Channel does
	 * not save the categories, they must be saved manually with this.
	 * 
	 */
	public void saveCategoriesToDb() {
		if (mCategories == null) {
			throw new IllegalStateException("Categories can't be null");
		}
		else if (mCategories.isEmpty()) {
			throw new IllegalStateException("Categories can't be empty");
		}
		
		/*
		 * TODO: Could maybe optimize this to not delete everything before
		 * saving the new ones if the deletions aren't necessary
		 */

		/* Clear the existing categories before getting the new ones. */
		clearChannelCategories();

		Transaction t = new Transaction();

		/* Add the new categories to the db */
		for (Category c : mCategories) {
			new ChannelCategory(c, this).save(t);
		}
		t.setSuccessful(true);
		t.finish();
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
		// id, so compare info instead

		return ch.mId == mId
				&& ch.mName.equals(mName)
				&& ch.mNsfw == mNsfw
				&& ch.mGifSetting == mGifSetting
				&& ((ch.mCategories == null && mCategories == null) || (ch.mCategories != null
						&& mCategories != null && ch.mCategories
							.containsAll(mCategories)));

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

	/**
	 * Get the last time the channel was used, in Unix time.
	 * 
	 * @param date
	 */
	public void setLastUsed(Date date) {
		/* Turn the millisecond value to unix time in seconds. */
		mLastUsed = date.getTime() / 1000L;
	}

	public Date getLastUsed() {
		return new Date(mLastUsed);
	}

	public Date getCreatedAt() {
		return new Date(mCreatedAt);
	}

	public void setName(String name) {
		mName = name;
	}

	/**
	 * Set the categories to be used for this channel. Updates the channel icon
	 * to use an image from the categories, and set the channel nsfw setting
	 * based on the categories. This must only be used once the channel has been
	 * saved to the database. You should manually save the channel after setting
	 * the categories as well as call saveCategoriesToDb().
	 * 
	 * @param categories
	 */
	public void setCategories(List<Category> categories) {
		if (categories == null) {
			throw new IllegalArgumentException("Categories can't be null");
		}
		else if (categories.isEmpty()) {
			throw new IllegalArgumentException("Categories can't be empty");
		}

		mCategories = categories;

		/* Set the channel icon based on the categories. */
		mPreviewImage = getCategories().get(0).getIconId();

		/* Set nsfw based on categories. */
		mNsfw = false;
		for (Category c : mCategories) {
			if (c.isNsfw()) {
				mNsfw = true;
				break;
			}
		}
	}

	/** For the Selectable interface and use with the selection fragment. */
	@Override
	public String getIconId() {
		return mPreviewImage;
	}

}
