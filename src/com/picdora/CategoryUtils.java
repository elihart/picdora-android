package com.picdora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
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
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Get the lowest score out of all the images in this category.
	 * 
	 * @param category
	 * @return
	 */
	public static int getLowestImageScore(Category category) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Get the date in unix time of the most recently created image in the given
	 * category.
	 * 
	 * @param category
	 * @return
	 */
	public static long getNewestImageDate(Category category) {
		// TODO Auto-generated method stub
		return 0;
	}

}
