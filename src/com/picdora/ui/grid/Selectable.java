package com.picdora.ui.grid;

public interface Selectable {

	public long getId();

	/** Get the imgur id of the icon to represent this */
	public String getIconId();

	/** The selectable's name */
	public String getName();
}
