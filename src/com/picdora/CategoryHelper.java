package com.picdora;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.emilsjolander.sprinkles.Query;

import com.picdora.models.Category;

public abstract class CategoryHelper {

	public static Category getCategoryById(int categoryId) {
		return Query.one(Category.class, "SELECT * FROM Categories WHERE id=?",
				categoryId).get();
	}

	public static void sortByName(List<Category> categories) {
		Collections.sort(categories, new Comparator<Category>() {

			@Override
			public int compare(Category lhs, Category rhs) {
				return lhs.getName().compareToIgnoreCase(rhs.getName());
			}
		});
	}

	
	
	public static void sortCategoryListAlphabetically(List<Category> categories){
		Collections.sort(categories, new CategoryAlphabeticalComparator());
	}
	
    /**
	 * Basic comparator to sort categories alphabetically by name
	 * 
	 */
	private static class CategoryAlphabeticalComparator implements Comparator<Category> {
		public int compare(Category left, Category right) {
			return left.getName().toLowerCase()
					.compareTo(right.getName().toLowerCase());
		}
	}

}
