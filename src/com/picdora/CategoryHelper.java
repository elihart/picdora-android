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
	
	

}
