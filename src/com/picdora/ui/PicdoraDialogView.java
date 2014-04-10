package com.picdora.ui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.PicdoraDialog.ButtonInfo;

@EViewGroup(R.layout.picdora_dialog)
public class PicdoraDialogView extends RelativeLayout {
	@ViewById
	protected RelativeLayout rootView;
	@ViewById
	protected LinearLayout titleContainer;
	@ViewById
	protected TextView title;
	@ViewById
	protected FrameLayout contentContainer;
	@ViewById
	protected TextView message;
	@ViewById
	protected LinearLayout buttonContainer;
	@ViewById
	protected Button negativeButton;
	@ViewById
	protected Button positiveButton;
	// TODO: Add neutral button if we need it

	protected PicdoraDialog mDialog;

	public PicdoraDialogView(Context context) {
		super(context);

	}

	@AfterViews
	protected void init() {
		FontHelper.setTypeFace(rootView);
		FontHelper.setTypeFace(title, FontStyle.MEDIUM);
	}

	public static class Builder {

		public Builder() {

		}
	}

	public void bind(PicdoraDialog dialog, String title, boolean showTitle,
			View view, String message, ButtonInfo positiveButtonInfo,
			ButtonInfo negativeButtonInfo, ButtonInfo neutralButtonInfo,
			boolean fullScreen) {

		mDialog = dialog;

		if (title != null) {
			this.title.setText(title);
		} else {
			this.title.setText(R.string.dialog_default_title);
		}

		if (showTitle) {
			this.titleContainer.setVisibility(View.VISIBLE);
		} else {
			this.titleContainer.setVisibility(View.GONE);
		}

		if (view != null) {
			this.message.setVisibility(View.GONE);
			contentContainer.addView(view);
		} else {
			this.message.setVisibility(View.VISIBLE);
			if (message == null) {
				this.message.setText(R.string.dialog_default_message);
			} else {
				this.message.setText(message);
			}
		}

		setupButton(positiveButton, positiveButtonInfo);

		setupButton(negativeButton, negativeButtonInfo);

		// TODO: Create a neutral button if we need one
		// setupButton(neutralButton, negativeButtonInfo);

		if (fullScreen) {
			// remove the margin of the dialog and make it full screen
			LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);

			params.setMargins(0, 0, 0, 0);
			rootView.setLayoutParams(params);

			// move the buttons to the very bottom
			params = (LayoutParams) buttonContainer.getLayoutParams();
			// clear out the rule to be below the contentContainer to avoid
			// circular dependency
			params.addRule(RelativeLayout.BELOW, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			// stretch the content to the buttons
			params = (LayoutParams) contentContainer.getLayoutParams();
			params.addRule(RelativeLayout.ABOVE, buttonContainer.getId());
		}
	}

	/**
	 * Setup a button with the given info
	 * 
	 * @param button
	 * @param info
	 */
	private void setupButton(Button button, ButtonInfo info) {
		// hide the button if there is no info for it
		if (info == null) {
			button.setVisibility(View.GONE);
		}
		// otherwise show it, set the text, and set the listener
		else {
			button.setVisibility(View.VISIBLE);
			button.setOnClickListener(makeButtonListener(info));
			button.setText(info.text);
		}
	}

	private OnClickListener makeButtonListener(final ButtonInfo button) {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDialog.dismiss();
				if (button.listener != null) {
					button.listener.onClick(mDialog, button.which);
				}
			}
		};
	}
}
