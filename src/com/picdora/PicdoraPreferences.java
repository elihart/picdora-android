package com.picdora;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultLong;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.APPLICATION_DEFAULT)
public interface PicdoraPreferences {

	@DefaultBoolean(true)
	boolean showNsfw();

	/**
	 * The last time our images were successfully updated in unix time. Defaults
	 * to 0 for yet updated.
	 */
	@DefaultLong(0)
	long lastImageUpdate();

}
