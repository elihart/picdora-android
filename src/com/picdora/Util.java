package com.picdora;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;

public class Util {
	
	/**
	 * Converts a java object to a Json string. Uses Google GSON
	 * @param src The object to be converted 
	 * @return A string of JSON representing the source object
	 */
	public static String toJson(Object src){
			// simple wrapper function for GSON
			Gson gson = new Gson();		
			return gson.toJson(src);
	}
	
	/**
	 * Convert a JSON string to a java class object
	 * @param <T>
	 * 
	 * @param json The json string to convert
	 * @param classType The class to convert to - e.g. Song.class
	 * @return The converted object. Null on failure
	 */
	public static <T> T fromJson(String json, Class<T> classType) {
		// simple wrapper function for GSON
		Gson gson = new Gson();	
		T result = null;
		try {
			result = gson.fromJson(json, classType);		
		} catch(Exception e) {
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

}
