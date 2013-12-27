package com.picdora;

import com.picdora.models.Category;

import se.emilsjolander.sprinkles.Query;

public class CategoryHelper {
	
	public static Category getCategoryById(int categoryId){
		return Query.one(Category.class, "SELECT * FROM Categories WHERE id=?", categoryId).get();		
	}

}
