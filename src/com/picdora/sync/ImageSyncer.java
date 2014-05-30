package com.picdora.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.client.Response;
import se.emilsjolander.sprinkles.Sprinkles;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.picdora.CategoryUtils;
import com.picdora.ImageUtils;
import com.picdora.PicdoraApp;
import com.picdora.Timer;
import com.picdora.Util;
import com.picdora.models.Category;

/**
 * Keeps our local image db up to date with the server
 * 
 */
@EBean
public class ImageSyncer extends Syncer {

	/**
	 * The number of images to get from the server in each batch. Null if we
	 * want to go with the server default.
	 * 
	 */
	private static final Integer BATCH_SIZE = 1000;
	/**
	 * The number of unseen images a category should have before we try to get
	 * more for it.
	 */
	private static final int LOW_IMAGE_THRESHOLD = 200;
	/** The number of fresh images to get for a category that is low on images. */
	private static final int NUM_IMAGES_FOR_LOW_CATEGORY = 1000;
	/**
	 * Number of images to seed each category with for a default database
	 * creation.
	 */
	private static final int BASE_IMAGE_COUNT_PER_CATEGORY = 600;

	/** The maximum number of times to retry a request until we give up. */
	private static final int MAX_RETRIES = 3;

	/**
	 * Our position in the update process. Our next batch should start with this
	 * id.
	 */
	private int mUpdateBatchId;
	/** The last time our images were updated. */
	private long mLastUpdated;
	/** The creation date of our newest image. */
	private long mLastCreated;
	/** If the device has a slqite version that supports bulk inserts. */
	private boolean mSupportsBulkInserts = false;

	@Override
	public void sync() {
		/*
		 * For inserting images it is faster to do bulk inserts but this wasn't
		 * introduced until sqlite 3.7.11. Older android devices do not have
		 * this version and we have to revert to individual insertions.
		 */
		Cursor cursor = SQLiteDatabase.openOrCreateDatabase(":memory:", null)
				.rawQuery("select sqlite_version() AS sqlite_version", null);
		String sqliteVersion = "";
		while (cursor.moveToNext()) {
			sqliteVersion += cursor.getString(0);
		}
		/*
		 * Need at least 3.7.11. TODO: Check for higher versions as well.
		 */
		mSupportsBulkInserts = sqliteVersion.equalsIgnoreCase("3.7.11");

		Timer syncTimer = new Timer();
		syncTimer.start();

		/*
		 * If we don't yet have any images in the database we can seed it by
		 * retrieving images for every category from the server.
		 */
		if (PicdoraApp.SEED_IMAGE_DATABASE) {
			List<Category> allCategories = CategoryUtils.getAll(true);
			for (Category c : allCategories) {
				getTopImages(c, BASE_IMAGE_COUNT_PER_CATEGORY);
			}
			/*
			 * We don't need to do updates since these are all new images. We're
			 * done.
			 */
			return;
		}

		/*
		 * TODO: Test updating and new images.
		 */

		/* Check for updates for the images in our local database. */
		long updateStartTime = Util.getUnixTime();
		boolean updateSuccess = updateImages();

		/*
		 * On update success record our update time and then check for new
		 * images. If update failed then don't go any further.
		 */
		if (!updateSuccess) {
			return;
		}

		setLastUpdated(updateStartTime);

		/*
		 * Next we want to check if any of our categories are low on images and
		 * get more if necessary. To be more efficient we'll only check
		 * categories that are currently being used by a channel.
		 */
		List<Category> categoriesInUse = CategoryUtils.getCategoriesInUse();

		/*
		 * Check the unseen image count for each category. If the count is low
		 * we'll try to get more pictures for it.
		 */

		/*
		 * TODO: Some categories have very low image counts and the system will
		 * try to get new images every time. We should try to make them more
		 * efficient.
		 */
		for (Category c : categoriesInUse) {
			int numUnseenImages = CategoryUtils.getImageCount(c, true);
			if (numUnseenImages < LOW_IMAGE_THRESHOLD) {
				int totalImages = CategoryUtils.getImageCount(c, false);
				getTopImages(c, totalImages + NUM_IMAGES_FOR_LOW_CATEGORY);
			}
		}
	}

	/**
	 * Update the images in the database.
	 * 
	 * @return True on success, false on failure.
	 */
	private boolean updateImages() {
		/* Start the update at the first id. */
		mUpdateBatchId = 0;
		/* Get update and creation dates from db. */
		mLastUpdated = getLastUpdated();
		mLastCreated = ImageUtils.getNewestImageDate();

		/*
		 * Retrieve batches until either we get all the images or we hit too
		 * many consecutive failures.
		 */
		int attempts = 0;
		while (attempts < MAX_RETRIES) {
			/*
			 * Get the next batch of images. This returns images only based on
			 * the updated and creation dates, so it may return images that we
			 * don't have in the database. We will ignore those.
			 */
			Response response = mApiService.updateImages(mUpdateBatchId,
					mLastUpdated, mLastCreated, BATCH_SIZE);

			/*
			 * Check for a successful response and keep track of consecutive
			 * failures. Reset the count on success.
			 */
			if (response == null || response.getBody() == null) {
				attempts++;
				/* Try again... */
				continue;
			} else {
				attempts = 0;
			}

			try {
				/* Process the response json. */
				String body = responseToString(response);
				JSONArray json = new JSONArray(body);
				/*
				 * Update the images in the db, ignoring images that we don't
				 * have.
				 */
				putImagesInDb(json, true);

				/*
				 * If the number of images returned is as big as our batch size
				 * then there could be more on the server that we missed. Get
				 * the id of the last image to use in the next batch request.
				 */
				int numImages = json.length();
				if (numImages >= BATCH_SIZE) {
					JSONObject lastImage = json.getJSONObject(numImages - 1);
					mUpdateBatchId = lastImage.getInt("id");
				}
				/* All done! */
				else {
					return true;
				}
			}
			/* Log exceptions and return false. */
			catch (IOException e) {
				Util.logException(e);
				return false;
			} catch (JSONException e) {
				Util.logException(e);
				return false;
			}
		}

		/* Give up after too many consecutive connection errors. */
		return false;

	}

	/**
	 * Get the last time our images were updated successfully in unix time.
	 * 
	 * @return
	 */
	private long getLastUpdated() {
		long lastUpdated = mPrefs.lastImageUpdate().get();

		/*
		 * If the update time is 0 then we have never recorded an update. In
		 * this case we should base our update time on the most recent updated
		 * field in the Images table as that will have been the most recent
		 * update for the seed images.
		 */
		if (lastUpdated > 0) {
			return lastUpdated;
		} else {
			return ImageUtils.getLastUpdated();
		}
	}

	/**
	 * Record the given time as the most recent time that we have performed an
	 * image update in unix time.
	 * 
	 * @param updateStartTime
	 */
	private void setLastUpdated(long updateStartTime) {
		mPrefs.lastImageUpdate().put(updateStartTime);
	}

	/**
	 * Get the top images in a category. Ignores deleted and reported images.
	 * 
	 * @param category
	 *            The categoryto get pictures for
	 * @param numImagesToGet
	 *            The number of images to get. The response may be less if there
	 *            aren't a sufficient number available
	 * @return True on success, false on failure.
	 */
	public boolean getTopImages(Category category, int numImagesToGet) {
		/*
		 * If the query to server fails retry it until it works or the max
		 * number of attempts is reached.
		 */
		int attempts = 0;
		while (attempts < MAX_RETRIES) {
			/*
			 * TODO: If numImagesToGet is large this can use lots of memory,
			 * especially in the parsing of the json. We can either break up the
			 * queries to the server (although those aren't that much of a
			 * problem) or break up the json parsing into smaller chunks..
			 */

			Response response = mApiService.topImages(category.getId(),
					numImagesToGet);

			/*
			 * Check for a successful response and keep track of consecutive
			 * failures. Reset the count on success.
			 */
			if (response == null || response.getBody() == null) {
				attempts++;
				/* Try again... */
				continue;
			} else {
				attempts = 0;
			}

			try {
				/*
				 * Parse the response into json and insert the data into the db.
				 */
				String body = responseToString(response);
				JSONArray json = new JSONArray(body);
				int numImages = json.length();
				long startTime = System.currentTimeMillis();
				putImagesInDb(json, false);
				long elapsed = System.currentTimeMillis() - startTime;
				Util.log("Inserted " + numImages + " images into "
						+ category.getName() + " in " + elapsed + " ms : "
						+ Math.round(numImages / (elapsed / 1000.0))
						+ " inserts/second. ");

				return true;
			} catch (IOException e) {
				Util.logException(e);
				return false;
			} catch (JSONException e) {
				Util.logException(e);
				return false;
			}
		}
		// Gave up after too many failed attempts
		return false;
	}

	/**
	 * Parse the json array into image data and save the images to the database,
	 * overwriting any existing tuples.
	 * 
	 * @param array
	 * @param update
	 *            True if the images should update existing images and false to
	 *            insert/overwrite into the database. If set to update then an
	 *            image won't be created if the id doesn't already exist.
	 * 
	 */
	protected void putImagesInDb(JSONArray array, boolean update) {
		// number of images in the array
		int numImages = array.length();
		/*
		 * Image category tags are stored in their own table with the imageId
		 * and categoryId uniquely making a tuple. An image can have multiple
		 * categories so we may need to insert multiple tuples for each image.
		 * Additionally, if we are updating an image it may have existing
		 * categories that may or may not exist in the updated image. To handle
		 * this efficiently we will mass deleted all category tags for the
		 * inserted images and then reinsert them in a bulk insert. The category
		 * values list will hold the sql values for an image/category tuple to
		 * be inserted at the end. Initialize the list size assuming each image
		 * has on average 2 categories.
		 */
		List<String> categoryValues = new ArrayList<String>(numImages * 2);
		/*
		 * Keep track of all the ids that we insert so we can clear out that
		 * category tags before inserting the updated/new ones.
		 */
		List<Integer> ids = new ArrayList<Integer>(numImages);

		SQLiteDatabase db = Sprinkles.getDatabase();

		db.beginTransaction();
		try {
			try {
				/* Get each image from json and insert it into the db. */
				for (int i = numImages - 1; i >= 0; i--) {
					JSONObject imageJson = array.getJSONObject(i);

					int id = imageJson.getInt("id");

					ContentValues values = new ContentValues();

					values.put("lastUpdated", imageJson.getLong("updated_at"));
					values.put("createdAt", imageJson.getLong("created_at"));
					values.put("imgurId", imageJson.getString("imgurId"));
					values.put("redditScore", imageJson.getLong("reddit_score"));
					values.put("nsfw", imageJson.getBoolean("nsfw"));
					values.put("gif", imageJson.getBoolean("gif"));
					values.put("deleted", imageJson.getBoolean("deleted"));
					values.put("reported", imageJson.getBoolean("reported"));

					/*
					 * Do an update or insert depending on the param setting. If
					 * the update doesn't affect any rows then we don't have
					 * that image and we shouldn't insert category tags for it.
					 */
					if (update) {
						int numRowsAffected = db.update("Images", values, "id="
								+ id, null);
						if (numRowsAffected == 0) {
							continue;
						}
					}
					/*
					 * Insert with ignoring conflicts. We don't want to replace
					 * because that will trigger a cascade delete on
					 * dependencies likes Likes and Collections.
					 */
					else {
						values.put("id", id);
						db.insertWithOnConflict("Images", null, values,
								SQLiteDatabase.CONFLICT_IGNORE);
					}

					/*
					 * Parse and store the category data for a bulk insert
					 * later. We could do them one at a time but it is much
					 * faster to do it all together. Store the id so we know
					 * which image category tags to delete later.
					 */
					ids.add(id);
					JSONArray categories = imageJson.getJSONArray("categories");
					int numCategories = categories.length();
					for (int j = 0; j < numCategories; j++) {
						StringBuilder catValue = new StringBuilder();
						catValue.append("(");
						catValue.append(id);
						catValue.append(", ");
						catValue.append(categories.getInt(j));
						catValue.append(")");
						categoryValues.add(catValue.toString());
					}
				}
			} catch (JSONException e) {
				Util.logException(e);
				return;
			}

			insertCategoryInfo(categoryValues, ids, db);

			db.setTransactionSuccessful();
		} catch (SQLException e) {
			Util.logException(e);
			return;
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Insert the category data into the database.
	 * 
	 * @param categoryValues
	 *            Strings of the form "(imageId, categoryId)" for inserting into
	 *            sql query.
	 * @param imageIds
	 *            List of all images that we are adding categories for.
	 * @param db
	 */
	private void insertCategoryInfo(List<String> categoryValues,
			List<Integer> imageIds, SQLiteDatabase db) {
		/*
		 * First delete any category tags previously assigned to these images.
		 */
		String whereClause = "imageId IN (" + TextUtils.join(",", imageIds)
				+ ")";
		db.delete("ImageCategories", whereClause, null);

		/*
		 * Bulk insert of the category tags. Sqlite has a limit of how many
		 * separate values can be added in one insert so we'll break it into
		 * batches.
		 */
		String categorySql = "INSERT OR IGNORE INTO ImageCategories (imageId, categoryId) "
				+ "VALUES ";
		int numCatValues = categoryValues.size();
		StringBuilder catSqlBuilder = null;

		/*
		 * For devices with an older sqlite version that doesn't support bulk
		 * insert do it one by one. It's about a 25-50% performance decrease.
		 */
		if (!mSupportsBulkInserts) {
			for (int i = 0; i < numCatValues; i++) {
				String cat = categoryValues.get(i);
				db.execSQL(categorySql + cat);
			}
		} else {
			for (int i = 0; i < numCatValues; i++) {
				/*
				 * Append each one of our category tag values onto the insert
				 * query.
				 */
				String cat = categoryValues.get(i);
				if (catSqlBuilder == null) {
					catSqlBuilder = new StringBuilder(categorySql);
				} else {
					catSqlBuilder.append(", ");
				}

				catSqlBuilder.append(cat);

				/*
				 * When the batch gets big enough execute the query and start a
				 * new one.
				 */

				if (i % 450 == 0) {
					db.execSQL(catSqlBuilder.toString());
					catSqlBuilder = null;
				}
			}

			/*
			 * Execute the remnants that weren't big enough to fill a whole
			 * batch.
			 */
			if (catSqlBuilder != null) {
				db.execSQL(catSqlBuilder.toString());
			}
		}
	}
}
