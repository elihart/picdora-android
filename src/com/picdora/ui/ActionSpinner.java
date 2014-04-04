package com.picdora.ui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

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

	/** The default maximum width in dp */
	private static final int MAX_WIDTH_DP = 200;

	/** The maximum width to bind to in pixels */
	private int mBoundedWidth;

	/** The registered listener for spinner selections */
	private OnItemSelectedListener mSelectionListener;

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
		/* Get max width in pixels */
		mBoundedWidth = UiUtil.dpToPixel(MAX_WIDTH_DP);

		icon.setImageDrawable(iconDrawable);

		/* When the icon is clicked we want to hide it and show the spinner */
		icon.setClickable(true);
		icon.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				expand();
			}
		});

		/*
		 * Handle spinner selection. We will ignore the very first selection
		 * because it is automatically generated, and only pass on the
		 * selections after that as those will be done by the user. This is just
		 * an odd idiosyncracy of how Spinners work.
		 */
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			boolean firstSelection = true;

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if (firstSelection) {
					/* Ignore the first selection */
					firstSelection = false;
					return;
				}

				if (mSelectionListener != null) {
					mSelectionListener.onItemSelected(parent, view, position,
							id);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				if (mSelectionListener != null) {
					mSelectionListener.onNothingSelected(parent);
				}
			}
		});
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		/* Limit to max width */
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (mBoundedWidth < measuredWidth) {
			int measureMode = MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(mBoundedWidth,
					measureMode);
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * Hide the spinner and show the icon.
	 * 
	 */
	public void collapse() {
		icon.setVisibility(View.VISIBLE);
		spinner.setVisibility(View.GONE);
	}

	/**
	 * Hide the icon and show the spinner.
	 * 
	 */
	public void expand() {
		icon.setVisibility(View.GONE);
		spinner.setVisibility(View.VISIBLE);

		/*
		 * Do a click so the dropdown happens automatically, but we need to make
		 * sure the view gets drawn first after being shown so we wait for a
		 * slight delay
		 */
		delayedSpinnerDropdown();
	}

	@UiThread(delay = 100)
	protected void delayedSpinnerDropdown() {
		spinner.performClick();
	}

	/**
	 * Set the adapter to use for the spinner.
	 * 
	 * @param adapter
	 */
	public void setAdapter(SpinnerAdapter adapter) {
		spinner.setAdapter(adapter);
	}

	/**
	 * Set the spinner's current position.
	 * 
	 * @param position
	 */
	public void setSelection(int position) {
		spinner.setSelection(position);
	}

	/**
	 * Set a listener for when a spinner item is selected.
	 * 
	 * @param listener
	 */
	public void setSelectionListener(OnItemSelectedListener listener) {
		mSelectionListener = listener;
	}

	/**
	 * Get the currently selected item position in the spinner.
	 * 
	 * @return
	 */
	public int getSelection() {
		return spinner.getSelectedItemPosition();
	}
}
