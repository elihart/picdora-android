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
	 * Get the number of usable images in the given categories. This excludes
	 * deleted and reported images.
	 * 
	 * @param category
	 * @param onlyCountUnseen
	 *            True if the count should only include images that haven't been
	 *            seen yet and not the total amount of images.
	 * @return
	 */
	public static int getImageCount(Category category, boolean onlyCountUnseen) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		final String query = "SELECT COUNT(*) FROM Images JOIN ImageCategories ON id=imageId WHERE categoryId="
				+ category.getId();
		
		// TODO: Add join on imge views and exlude seen.

		SQLiteStatement s = db.compileStatement(query);

		long result = 0;
		try {
			result = s.simpleQueryForLong();
		} catch (SQLiteDoneException ex) {
			// no result
		}

		return (int) result;
	}

	/**
	 * Get the lowest score out of all the images in this category. If there are
	 * no images in this category then MAX_INT is returned.
	 * 
	 * @param category
	 * @return
	 */
	public static int getLowestImageScore(Category category) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		final String query = "SELECT MIN(redditScore) FROM Images JOIN ImageCategories ON id=imageId WHERE categoryId="
				+ category.getId();

		SQLiteStatement s = db.compileStatement(query);

		long result = 0;
		try {
			result = s.simpleQueryForLong();
		} catch (SQLiteDoneException ex) {
			// no result
		}

		return (int) result;
	}

	/**
	 * Get the date in unix time of the most recently created image in the given
	 * category.
	 * 
	 * @param category
	 * @return
	 */
	public static long getNewestImageDate(Category category) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		final String query = "SELECT MAX(createdAt) FROM Images JOIN ImageCategories ON id=imageId WHERE categoryId="
				+ category.getId();

		SQLiteStatement s = db.compileStatement(query);

		long result = 0;
		try {
			result = s.simpleQueryForLong();
		} catch (SQLiteDoneException ex) {
			// no result
		}

		return result;
	}

}
