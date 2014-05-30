package com.picdora;

import se.emilsjolander.sprinkles.Sprinkles;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;

public class DbUtils {

	/**
	 * Compile the given sql query and execute it as a simpleQueryForLong().
	 * 
	 * @param query
	 * @param noMatchResult What should be returned if the query doesn't match any rows.
	 * @return The query result, or noMatchResult if no rows are returned.
	 */
	public static long simpleQueryForLong(String query, long noMatchResult) {
		SQLiteDatabase db = Sprinkles.getDatabase();

		SQLiteStatement s = db.compileStatement(query);

		try {
			return s.simpleQueryForLong();
		} catch (SQLiteDoneException ex) {
			return noMatchResult;
		}
	}

}
