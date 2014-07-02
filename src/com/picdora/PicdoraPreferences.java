package com.picdora;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultLong;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.APPLICATION_DEFAULT)
public interface PicdoraPreferences {

	@DefaultBoolean(true)
	boolean showNsfw();
	
	/** Whether this is the first time the user has run the app. */
	@DefaultBoolean(true)
	boolean firstLaunch();
	// TODO: Need to turn this to false still...

	/**
	 * The last time our images were successfully updated in unix time. Defaults
	 * to 0 for yet updated.
	 */
	@DefaultLong(0)
	long lastImageUpdate();
	
	/** Whether a warning dialog has been shown about heavy data usage on mobile networks. */
	@DefaultBoolean(false)
	boolean hasShownDataWarning();

}
