package com.picdora.ui.grid;

/**
 * For use with {@link #SelectionFragment}, objects implementing this interface
 * can be displayed in a grid for selection.
 * 
 */
public interface Selectable {

	public long getId();

	/** Get the imgur id of the icon to represent this selectable. */
	public String getIconId();

	/** The selectable's name. Can be null if only an image represents the item. */
	public String getName();
}
