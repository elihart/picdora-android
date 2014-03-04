package com.picdora;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Model;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.annotations.Table;
import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;

public class Util {

	/**
	 * Converts a java object to a Json string. Uses Google GSON
	 * 
	 * @param src
	 *            The object to be converted
	 * @return A string of JSON representing the source object
	 */
	public static String toJson(Object src) {
		// simple wrapper function for GSON
		Gson gson = new Gson();
		return gson.toJson(src);
	}

	/**
	 * Convert a JSON string to a java class object
	 * 
	 * @param <T>
	 * 
	 * @param json
	 *            The json string to convert
	 * @param classType
	 *            The class to convert to - e.g. Song.class
	 * @return The converted object. Null on failure
	 */
	public static <T> T fromJson(String json, Class<T> classType) {
		// simple wrapper function for GSON
		Gson gson = new Gson();
		T result = null;
		try {
			result = gson.fromJson(json, classType);
		} catch (Exception e) {
			return null;
		}

		return result;
	}

	/**
	 * Display a toast with default settings
	 * 
	 * @param context
	 * @param msg
	 */
	public static void makeBasicToast(Context context, String msg) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}

	/**
	 * Print a message to log. Shortcut for calling to System.out.println()
	 * 
	 * @param msg
	 *            The message to log
	 */
	public static void log(String msg) {
		System.out.println(msg);
	}

	/**
	 * Check if a string is null or has only white space
	 * 
	 */
	public static boolean isStringBlank(String str) {
		if (str == null || str.trim().equals("")) {
			return true;
		} else {
			return false;
		}
	}

	public static <T extends Model> List<T> all(Class<T> clazz) {
		List<T> models = new ArrayList<T>();
		String query = "SELECT * FROM " + clazz.getAnnotation(Table.class).value();
		CursorList<T> list = Query.many(clazz, query, null).get();
		models.addAll(list.asList());
		list.close();

		return models;
	}
	
	public static String capitalize(String str) {		
        if (str == null || str.length() == 0) {
            return str;
        }
        
        str = str.toLowerCase();
        
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);

        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            boolean isDelimiter = Character.isWhitespace(ch);

            if (isDelimiter) {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }
}
