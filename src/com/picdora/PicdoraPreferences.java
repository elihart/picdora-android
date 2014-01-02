package com.picdora;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref
public interface PicdoraPreferences {

	// The field lastUpdated will have default value 0
	long lastUpdated();

}
