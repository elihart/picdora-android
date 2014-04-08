package com.picdora.collections;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

public class CollectionUtil {

	public static List<Collection> getAllCollections(boolean includeNsfw){
		 
		String query = "SELECT * FROM " + Collection.TABLE_NAME;

		if (!includeNsfw) {
			query += " WHERE nsfw=0";
		}

		CursorList<Collection> list = Query.many(Collection.class, query, null).get();
		List<Collection> result = list.asList();
		list.close();

		return result;
	}
}
