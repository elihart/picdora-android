package com.picdora.collections;

import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;
import se.emilsjolander.sprinkles.Sprinkles;
import se.emilsjolander.sprinkles.Transaction;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.picdora.ImageUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.PicdoraDialog;

@EBean(scope = Scope.Singleton)
public class CollectionUtil {
	@RootContext
	protected Context mContext;

	public static List<Collection> getAllCollections(boolean includeNsfw) {

		String query = "SELECT * FROM " + Collection.TABLE_NAME;

		if (!includeNsfw) {
			query += " WHERE nsfw=0";
		}

		CursorList<Collection> list = Query.many(Collection.class, query, null)
				.get();
		List<Collection> result = list.asList();
		list.close();

		return result;
	}

	/**
	 * Get a dialog that will prompt the user to provide a name for a new
	 * collection. On positive click the name will be validated and the
	 * collection created if valid. This will be done in the background. The
	 * result will be returned through the listener.
	 * 
	 * @param activity
	 * @param listener
	 * @return
	 */
	@UiThread(propagation = Propagation.REUSE)
	public void showCollectionCreationDialog(Activity activity,
			final OnCollectionCreatedListener listener) {
		LinearLayout container = (LinearLayout) LayoutInflater.from(activity)
				.inflate(R.layout.edit_text_for_dialog, null);
		final EditText collectionName = (EditText) container
				.findViewById(R.id.edit_text);
		collectionName.setHint(R.string.collections_create_dialog_hint);
		FontHelper.setTypeFace(collectionName, FontStyle.REGULAR);

		final PicdoraDialog dialog = new PicdoraDialog.Builder(activity)
				.setTitle(R.string.collections_create_dialog_title)
				.setView(container)
				.setPositiveButton(R.string.collections_create_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								createCollection(collectionName.getText()
										.toString(), listener);
							}
						})
				.setNegativeButton(R.string.dialog_default_negative, null)

				.create();

		// show the keyboard when the dialog pops up
		collectionName
				.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							dialog.getWindow()
									.setSoftInputMode(
											WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						}
					}
				});

		dialog.show();
	}

	/**
	 * Errors that can occur while creating a new collection.
	 * 
	 */
	public enum CreationError {
		NAME_TAKEN, NAME_BLANK, INVALID, DB_ERROR;
	}

	/**
	 * Create a new collection with the given name. This will be done in the
	 * background. Validates the collection and return success or failure
	 * through the listener if one is given.
	 * 
	 * @param name
	 * @param listener
	 */
	@Background
	public void createCollection(String name,
			OnCollectionCreatedListener listener) {
		Collection collection = new Collection(name);

		CreationError error = null;
		if (Util.isStringBlank(name)) {
			error = CreationError.NAME_BLANK;
		} else if (isNameTaken(name)) {
			error = CreationError.NAME_TAKEN;
		} else if (!collection.isValid()) {
			error = CreationError.INVALID;
		} else {
			boolean success = collection.save();
			if (!success) {
				error = CreationError.DB_ERROR;
			}
		}

		/*
		 * If no listener was passed in then we're done, otherwise alert the
		 * listener of the result.
		 */
		if (listener == null) {
			return;
		} else if (error == null) {
			listener.onSuccess(collection);
		} else {
			listener.onFailure(error);
		}
	}

	/**
	 * Callback for creating a new collection.
	 * 
	 */
	public interface OnCollectionCreatedListener {
		public void onSuccess(Collection collection);

		public void onFailure(CreationError error);
	}

	/**
	 * Check if a collection name is in use, case insensitive
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isNameTaken(String name) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "SELECT count(*) FROM " + Collection.TABLE_NAME
				+ " WHERE name = '" + name + "'  COLLATE NOCASE";

		SQLiteStatement s = db.compileStatement(query);

		try {
			return s.simpleQueryForLong() > 0;
		} catch (SQLiteDoneException e) {
			return false;
		}
	}

	/**
	 * Notify the user of an error while creating their collection and allow
	 * them to try again or cancel.
	 * 
	 * @param context
	 * @param error
	 *            The error that was encountered.
	 * @param listener
	 *            A listener that will be used if the user decides to try again.
	 */
	@UiThread(propagation = Propagation.REUSE)
	public void alertCreationError(final Activity context, CreationError error,
			final OnCollectionCreatedListener listener) {
		String msg = "Uh oh, somethign went wrong creating the collection :(";
		switch (error) {
		case NAME_TAKEN:
			msg = "There's already a collection with that name!";
			break;
		case NAME_BLANK:
			msg = "Your collection needs a name!";
			break;
		case DB_ERROR:
			break;
		case INVALID:
			break;
		default:
			break;
		}

		/*
		 * Show the error and give them the change to cancel or try again.
		 */
		new PicdoraDialog.Builder(context)
				.setTitle(R.string.collections_create_error_dialog_title)
				.setMessage(msg)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.collections_create_error_dialog_try_again,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								showCollectionCreationDialog(context, listener);
							}
						}).show();

	}

	/**
	 * Delete the given Collections in the background.
	 * 
	 * @param selection
	 */
	@Background
	public void delete(List<Collection> collections) {
		Transaction t = new Transaction();
		for (Collection c : collections) {
			c.delete(t);
		}
		t.setSuccessful(true);
		t.finish();
	}

	/**
	 * Load all the images in the given Collection..
	 * 
	 * @param collection
	 * @return
	 */
	public List<Image> loadCollectionImages(Collection collection) {
		// TODO: joins?
		String query = "SELECT * FROM Images WHERE id IN (SELECT imageId FROM "
				+ CollectionItem.TABLE_NAME + " WHERE collectionId="
				+ collection.getId() + ")";

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		List<Image> images = list.asList();
		list.close();

		return images;
	}

	@Background
	public void deleteCollectionImages(Collection collection, List<Image> images) {
		SQLiteDatabase db = Sprinkles.getDatabase();
		String query = "DELETE " + CollectionItem.TABLE_NAME + " WHERE collectionId=" + collection.mId + " AND imageId IN " + ImageUtils.getImageIds(images);

		db.compileStatement(query).execute();
		
	}
}
