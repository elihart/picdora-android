package com.picdora.collections;

import java.util.ArrayList;
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
	 * Show a dialog with a list of all the collections in the database and
	 * allow the user to pick one. They also have an option to create a new
	 * collection which will launch the corresponding dialog.
	 * 
	 * @param activity
	 * @param title
	 *            The title to use for the dialog. Use null for default.
	 * @param listener
	 */
	@UiThread(propagation = Propagation.REUSE)
	public void showCollectionSelectionDialog(final Activity activity,
			final String title, final OnCollectionSelectedListener listener) {

		/*
		 * Use the provided title if it isn't null, otherwise use our default.
		 */
		String titleToUse = title;
		if (title == null) {
			titleToUse = activity.getResources().getString(
					R.string.collections_selection_dialog_title);
		}

		final CollectionListView list = CollectionListView_.build(activity);

		final PicdoraDialog dialog = new PicdoraDialog.Builder(activity)
				.setTitle(titleToUse)
				.setView(list)
				.setPositiveButton(
						R.string.collections_selection_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								/*
								 * The positive button allows the user to create
								 * a new dialog. On click launch the collection
								 * creation dialog.
								 */
								showCollectionCreationDialog(activity,
										new OnCollectionCreatedListener() {

											@Override
											public void onSuccess(
											/*
											 * Created! Let's go back to showing
											 * our initial selection dialog.
											 */
											Collection collection) {
												showCollectionSelectionDialog(
														activity, title,
														listener);
											}

											@Override
											public void onFailure(
											/*
											 * Show the failure dialog and allow
											 * them to come back to this
											 * creation listener.
											 */
											CreationError error) {
												alertCreationError(activity,
														error, this);

											}
										});
							}
						})
				.setNegativeButton(R.string.dialog_default_negative, null)

				.create();

		/*
		 * When a collection is selected dismiss the dialog and pass on the
		 * result to the listener.
		 */
		list.setOnCollectionSelectedListener(new OnCollectionSelectedListener() {

			@Override
			public void onCollectionSelected(Collection collection) {
				dialog.dismiss();
				listener.onCollectionSelected(collection);
			}
		});

		dialog.show();
	}

	/**
	 * Callback for the collection selection dialog.
	 * 
	 */
	public interface OnCollectionSelectedListener {
		/**
		 * Called when the user selected a Collection from the selection dialog.
		 * 
		 * @param collection
		 */
		public void onCollectionSelected(Collection collection);
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
		/**
		 * On collection created successfully.
		 * 
		 * @param collection
		 */
		public void onSuccess(Collection collection);

		/**
		 * On collection failed to create.
		 * 
		 * @param error
		 */
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
		String query = "DELETE from " + CollectionItem.TABLE_NAME
				+ " WHERE collectionId=" + collection.mId + " AND imageId IN "
				+ ImageUtils.getImageIds(images);

		db.execSQL(query);
	}

	/**
	 * Add the images to the collection in the background. If an image is
	 * already in the collection then overwrite it instead of making a
	 * duplicate.
	 * 
	 * @param collection
	 * @param imagesToAdd
	 */
	@Background
	public void addImages(Collection collection, List<Image> imagesToAdd) {
		/*
		 * TODO: Add uniqueness constraint based on imageid and collection id to
		 * prevent duplicate images in a collection. Sprinkles should support
		 * this soon so add it when it does. Right now we do a manual check for
		 * each image which is slow.
		 */

		Transaction t = new Transaction();
		for (Image i : imagesToAdd) {
			/* Only add the image if the collection doesn't already contain it. */
			if (!contains(collection, i)) {
				new CollectionItem(collection, i).save(t);
			}
		}
		t.setSuccessful(true);
		t.finish();
	}

	/**
	 * Check if the collection contains the given image.
	 * 
	 * @param collection
	 * @param image
	 * @return
	 */
	private boolean contains(Collection collection, Image image) {
		SQLiteDatabase db = Sprinkles.getDatabase();

		String query = "SELECT count(*) FROM " + CollectionItem.TABLE_NAME
				+ " WHERE imageId=" + image.getId() + " AND collectionId="
				+ collection.getId();

		SQLiteStatement s = db.compileStatement(query);

		return s.simpleQueryForLong() > 0;
	}

	/**
	 * Add the image to the collection in the background.
	 * 
	 * @param collection
	 * @param image
	 */
	public void addImage(Collection collection, Image image) {
		/*
		 * Create a list with the single image and use the multiple image
		 * method.
		 */
		List<Image> list = new ArrayList<Image>();
		list.add(image);
		addImages(collection, list);
	}
}
