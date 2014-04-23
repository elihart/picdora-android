package com.picdora;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.APPLICATION_DEFAULT)
public interface PicdoraPreferences {

	// The field lastUpdated will have default value 0
	long lastUpdated();

	@DefaultBoolean(true)
	boolean showNsfw();
	
	/** Whether this is the first time the user has run the app. */
	@DefaultBoolean(true)
	boolean firstLaunch();

}
