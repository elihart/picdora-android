package com.picdora.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * This class helps style text in the lyricoo font
 * 
 */
public class FontHelper {
	// we need the app context to create a TypeFaceSpan because that class uses
	// a cache to prevent memory leaks that can happen when using outside fonts
	private static Context mAppContext;

	private static SparseArray<Typeface> mTypeFaceMap;

	public enum FontStyle {
		BOLD, BOLD_ITALIC, ITALIC, MEDIUM, MEDIUM_ITALIC, REGULAR
	}

	public static void init(Context appContext) {
		mAppContext = appContext;
		mTypeFaceMap = new SparseArray<Typeface>();
	}

	/**
	 * Style a string with a particular font style
	 * 
	 * @param text
	 *            The text to style
	 * @param style
	 *            The font to use
	 * @return A span containing the styled text
	 */
	public static SpannableString styleString(String text, FontStyle style) {
		SpannableString s = new SpannableString(text);
			String font = getFontFilename(style);
			s.setSpan(new TypefaceSpan(mAppContext, font), 0, s.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		return s;
	}

	/**
	 * Return the font file name
	 * 
	 * @param style
	 * @return
	 */
	public static String getFontFilename(FontStyle style) {
		switch (style) {
		case BOLD:
			return "Nobile-Bold.ttf";
		case BOLD_ITALIC:
			return "Nobile-BoldItalic.ttf";
		case ITALIC:
			return "Nobile-Italic.ttf";
		case MEDIUM:
			return "Nobile-Medium.ttf";
		case MEDIUM_ITALIC:
			return "Nobile-MediumItalic.ttf";
		case REGULAR:
			return "Nobile-Regular.ttf";
		default:
			return "Nobile-Regular.ttf";
		}
	}

	/**
	 * Return the path name of the font file with the assets folder
	 * 
	 * @param style
	 * @return
	 */
	public static String getFontPath(FontStyle style) {
		return "fonts/" + getFontFilename(style);
	}

	/**
	 * Set the font of the typeface to the given lyricoo style. On failure the
	 * error is caught and the font is left alone
	 * 
	 * @param text
	 * @param style
	 * @return Whether or not the font was set successfully
	 */
	public static boolean setTypeFace(TextView text, FontStyle style) {
		try {
			text.setTypeface(getTypeface(style));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get the typeface for the given style. Throws an error if the typeface
	 * can't be created.
	 * 
	 * @param style
	 * @return
	 * @throws Exception
	 */
	private static Typeface getTypeface(FontStyle style) throws Exception {
		// cache the type faces for big efficiency gains when showing lots of
		// text

		Typeface typeface = mTypeFaceMap.get(style.ordinal());

		if (typeface == null) {
			String fontPath = FontHelper.getFontPath(style);
			typeface = Typeface.createFromAsset(mAppContext.getAssets(),
					fontPath);
			mTypeFaceMap.put(style.ordinal(), typeface);
		}

		return typeface;
	}

	public static void setTypeFace(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View child = vg.getChildAt(i);
			if (child instanceof Button) {
				setTypeFace((TextView) child, FontStyle.MEDIUM);
			} else if (child instanceof TextView) {
				setTypeFace((TextView) child, FontStyle.REGULAR);
			}
			if (child instanceof ViewGroup) {
				setTypeFace((ViewGroup) child);
			}
		}
	}
}
