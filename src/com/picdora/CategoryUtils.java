package com.picdora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.picdora.models.Category;

public abstract class CategoryUtils {

	public static Category getCategoryById(int categoryId) {
		return Query.one(Category.class, "SELECT * FROM Categories WHERE id=?",
				categoryId).get();
	}

	/**
	 * Sort categories alphabetically by name
	 * 
	 * @param categories
	 */
	public static void sortByName(List<Category> categories) {
		Collections.sort(categories, new Comparator<Category>() {

			@Override
			public int compare(Category lhs, Category rhs) {
				return lhs.getName().compareToIgnoreCase(rhs.getName());
			}
		});
	}

	/**
	 * Get all categories synchronously from the database
	 * 
	 * @param includeNsfw
	 *            True if nsfw categories should be included
	 * @return
	 */
	public static List<Category> getAll(boolean includeNsfw) {
		List<Category> categories = new ArrayList<Category>();
		String query = "SELECT * FROM Categories";

		if (!includeNsfw) {
			query += " WHERE nsfw=0";
		}

		CursorList<Category> list = Query.many(Category.class, query, null)
				.get();
		categories.addAll(list.asList());
		list.close();

		return categories;
	}

	/**
	 * get a comma separated list of category ids for use in a sql query
	 * 
	 * @param categories
	 *            The categories whose id's we want.
	 * 
	 * @return
	 */
	public static String getCategoryIdsString(List<Category> categories) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Category cat : categories) {
			ids.add((int) cat.getId());
		}

		return ("(" + TextUtils.join(",", ids) + ")");
	}

	/**
	 * Get the number of unique usable images in the given category. This excludes
	 * deleted and reported images. Images that have already been seen can be
	 * excluded as well.
	 * 
	 * @param category
	 * @param excludeSeen
	 *            Whether images that have already been seen should be excluded
	 *            from the count.
	 * @return
	 */
	public static int getImageCount(Category category, boolean excludeSeen) {
		String query = "SELECT COUNT(distinct Images.id) FROM ImagesWithCategories WHERE deleted=0 AND reported=0 AND categoryId="
				+ category.getId();
		
		if(excludeSeen){
			query += " AND id NOT IN (SELECT distinct imageId FROM Views)";
		}

		/* Return 0 if no images match the query. */
		return (int) DbUtils.simpleQueryForLong(query, 0);
	}

	/**
	 * Get the lowest score out of all the images in this category. If there are
	 * no images in this category then -1 is returned.
	 * 
	 * @param category
	 * @return
	 */
	public static int getLowestImageScore(Category category) {
		final String query = "SELECT MIN(redditScore) FROM ImagesWithCategories WHERE categoryId="
				+ category.getId();

		return (int) DbUtils.simpleQueryForLong(query, -1);
	}

	/**
	 * Get the date in unix time of the most recently created image in the given
	 * category, or 0 if no images match.
	 * 
	 * @param category
	 * @return
	 */
	public static long getNewestImageDate(Category category) {
		final String query = "SELECT MAX(createdAt) FROM ImagesWithCategories WHERE categoryId="
				+ category.getId();
		
		return DbUtils.simpleQueryForLong(query, 0);
	}

	/** Get all the categories that are used in existing channels.
	 * 
	 * @return
	 */
	public static List<Category> getCategoriesInUse() {
		List<Category> categories = new ArrayList<Category>();
		String query = "SELECT * FROM Categories WHERE id IN (SELECT categoryId FROM ChannelCategories)";


		CursorList<Category> list = Query.many(Category.class, query, null)
				.get();
		categories.addAll(list.asList());
		list.close();

		return categories;
	}

}
