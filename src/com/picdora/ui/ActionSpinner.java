package com.picdora.ui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.picdora.R;

/**
 * An actionview layout for showing a spinner. An icon is shown by default, and
 * the spinner expands when the icon is clicked.
 * 
 */
@EViewGroup(R.layout.action_spinner)
public class ActionSpinner extends RelativeLayout {
	/** The drawable to use for the icon */
	protected Drawable iconDrawable;
	/** The icon to show when the spinner is hidden */
	@ViewById
	protected ImageView icon;
	/** The dropdown spinner */
	@ViewById
	protected Spinner spinner;

	/** Whether the spinner is currently collapsed */
	private boolean mCollapsed = true;

	public ActionSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);

		/* Get the icon drawable */
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ActionSpinner, 0, 0);
		iconDrawable = a.getDrawable(R.styleable.ActionSpinner_spinnerIcon);
		a.recycle();
	}

	@AfterViews
	protected void init() {
		icon.setImageDrawable(iconDrawable);

		/* When the icon is clicked we want to hide it and show the spinner */
		icon.setClickable(true);
		icon.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				expandSpinner();
				spinner.performClick();
			}
		});
	}

	/**
	 * Get the spinner for this view.
	 * 
	 * @return
	 */
	public Spinner getSpinner() {
		return spinner;
	}

	/**
	 * Hide the spinner and show the icon.
	 * 
	 */
	public void collapseSpinner() {
		mCollapsed = true;
		icon.setVisibility(View.VISIBLE);
		spinner.setVisibility(View.GONE);
	}

	/**
	 * Hide the icon and show the spinner.
	 * 
	 */
	public void expandSpinner() {
		mCollapsed = false;
		icon.setVisibility(View.GONE);
		spinner.setVisibility(View.VISIBLE);
	}

	/**
	 * Collapse the spinner if the given motionevent happened outside of the
	 * spinner bounds.
	 * 
	 * @param ev
	 */
	public void collapseIfOutside(MotionEvent ev) {
		/* If we're already collapsed no need to do anything */
		if (mCollapsed) {
			return;
		}

		/*
		 * Get the current location on screen and compare the given point to our
		 * bounds
		 */
		int[] l = new int[2];
		getLocationOnScreen(l);
		int x = l[0];
		int y = l[1];
		int w = getWidth();
		int h = getHeight();

		/* If the point is outside then collapse */
		if (ev.getRawX() < x || ev.getRawX() > x + w || ev.getRawY() < y
				|| ev.getRawY() > y + h) {
			collapseSpinner();
		}

	}

}
