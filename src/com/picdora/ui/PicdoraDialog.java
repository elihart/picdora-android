package com.picdora.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.picdora.R;

public class PicdoraDialog extends Dialog {

	public PicdoraDialog(Context context, int theme) {
		super(context, theme);
	}

	public static class Builder {
		private Context mContext;
		private String mTitle;
		private boolean mShowTitle = true;
		private View mView;
		private String mMessage;
		private boolean mFullScreen = false;
		private ButtonInfo mNegativeButton;
		private ButtonInfo mNeutralButton;
		private ButtonInfo mPositiveButton;

		public Builder(Context context) {
			mContext = context;
		}

		public Builder setTitle(String title) {
			mTitle = title;
			return this;
		}

		public Builder setTitle(int resId) {
			return setTitle(mContext.getText(resId).toString());
		}

		public Builder showTitle(boolean show) {
			mShowTitle = show;
			return this;
		}

		public Builder setMessage(String message) {
			mMessage = message;
			return this;
		}

		public Builder setMessage(int resId) {
			return setMessage(mContext.getText(resId).toString());
		}

		public Builder setView(View view) {
			mView = view;
			return this;
		}

		public Builder setFullScreen(boolean fullscreen) {
			mFullScreen = fullscreen;
			return this;
		}

		/**
		 * Set a positive, negative, or neutral button
		 * 
		 * @param which
		 *            The button id, either DialogInterface.BUTTON_POSITIVE,
		 *            NEGATIVE, or NEUTRAL
		 * @param text
		 *            A string to use as the button text
		 * @param listener
		 *            A callback on button click
		 * @return
		 */
		public Builder setButton(int which, String text,
				DialogInterface.OnClickListener listener) {
			switch (which) {
			case DialogInterface.BUTTON_NEGATIVE:
				mNegativeButton = new ButtonInfo(which, text, listener);
				break;
			case BUTTON_NEUTRAL:
				mNeutralButton = new ButtonInfo(which, text, listener);
				break;
			case BUTTON_POSITIVE:
				mPositiveButton = new ButtonInfo(which, text, listener);
				break;
			default:
				throw new IllegalArgumentException("Invalid button");
			}

			return this;
		}

		/**
		 * Set a positive, negative, or neutral button
		 * 
		 * @param which
		 *            The button id, either DialogInterface.BUTTON_POSITIVE,
		 *            NEGATIVE, or NEUTRAL
		 * @param resId
		 *            A string id to use as the button text
		 * @param listener
		 *            A callback on button click
		 * @return
		 */
		public Builder setButton(int which, int resId,
				DialogInterface.OnClickListener listener) {
			return setButton(which, mContext.getString(resId), listener);
		}

		/**
		 * Helper function to set the positive button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setPositiveButton(int resId,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_POSITIVE, resId, listener);
		}

		/**
		 * Helper function to set the positive button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setPositiveButton(String text,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_POSITIVE, text, listener);
		}

		/**
		 * Helper function to set the neutral button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setNeutralButton(int resId,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_NEUTRAL, resId, listener);
		}

		/**
		 * Helper function to set the neutral button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setNeutralButton(String text,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_NEUTRAL, text, listener);
		}

		/**
		 * Helper function to set the negative button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setNegativeButton(int resId,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_NEGATIVE, resId, listener);
		}

		/**
		 * Helper function to set the negative button
		 * 
		 * @param resId
		 * @param listener
		 * @return
		 */
		public Builder setNegativeButton(String text,
				DialogInterface.OnClickListener listener) {
			return setButton(DialogInterface.BUTTON_NEGATIVE, text, listener);
		}

		public PicdoraDialog create() {
			PicdoraDialog dialog = new PicdoraDialog(mContext,
					R.style.picdora_dialog_style);

			PicdoraDialogView view = PicdoraDialogView_.build(mContext);
			view.bind(dialog, mTitle, mShowTitle, mView, mMessage,
					mPositiveButton, mNegativeButton, mNeutralButton,
					mFullScreen);

			dialog.setContentView(view);
			return dialog;
		}

		public void show() {
			create().show();			
		}

	}

	public static class ButtonInfo {
		public int which;
		public String text;
		public DialogInterface.OnClickListener listener;

		public ButtonInfo(int which, String text, OnClickListener listener) {
			super();
			this.which = which;
			this.text = text;
			this.listener = listener;
		}
	}

}
