package com.picdora;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

import com.picdora.ui.grid.GridSize;

@SharedPref(value = SharedPref.Scope.APPLICATION_DEFAULT)
public interface PicdoraPreferences {

	// The field lastUpdated will have default value 0
	long lastUpdated();

	@DefaultBoolean(true)
	boolean showNsfw();

	/**
	 * The column width of an image grid. Use the ordinals of {@link #GridSize}
	 * to store the grid size.
	 */
	@DefaultInt(2)
	int gridSize();

}
