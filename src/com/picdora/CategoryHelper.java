package com.picdora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import com.picdora.models.Category;

public abstract class CategoryHelper {

	public static Category getCategoryById(int categoryId) {
		return Query.one(Category.class, "SELECT * FROM Categories WHERE id=?",
				categoryId).get();
	}

	/**
	 * Sort categories alphabetically by name
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
	 * @param includeNsfw True if nsfw categories should be included
	 * @return
	 */
	public static List<Category> getAll(boolean includeNsfw) {
		List<Category> categories = new ArrayList<Category>();
		String query = "SELECT * FROM Categories";

		if (!includeNsfw) {
			query += " WHERE nsfw=0";
		}

		CursorList<Category> list = Query.many(Category.class, query, null).get();
		categories.addAll(list.asList());
		list.close();

		return categories;
	}

}
