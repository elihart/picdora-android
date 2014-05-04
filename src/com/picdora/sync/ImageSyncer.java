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
import android.database.sqlite.SQLiteDatabase;

import com.picdora.CategoryUtils;
import com.picdora.ImageUtils;
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
	private static final int NUM_IMAGES_FOR_LOW_CATEGORY = 600;

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

	@Override
	public void sync() {
		/* Check for updates for the images in our local database. */
		long updateStartTime = Util.getUnixTime();
		boolean updateSuccess = updateImages();

		/*
		 * On update success record our update time and then check for new
		 * images.
		 */
		if (updateSuccess) {
			setLastUpdated(updateStartTime);
			getNewImages();
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
			/* Log exceptions and returnfalse. */
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
	 * Retrieve new images from the database that were created since we last
	 * synced. Images are retrieved in batches to prevent too much data from
	 * being loaded at once. If the server has more images than we want in our
	 * batch size it will give us the id of the next image to use in our next
	 * batch
	 * 
	 * @return True on success, false on failure.
	 */
	public boolean getNewImages() {
		/* Get all categories and check which ones are low on images. */
		List<Category> allCategories = CategoryUtils.getAll(true);
		/*
		 * List of categories whose unseen image count is below the threshold
		 * and need more images.
		 */
		List<Category> lowCategories = new ArrayList<Category>();
		/* Get image counts and identify the low categories. */
		for (Category c : allCategories) {
			int numUnseenImages = CategoryUtils.getImageCount(c, true);
			if (numUnseenImages < LOW_IMAGE_THRESHOLD) {
				lowCategories.add(c);
			}
		}

		/*
		 * Try to get more images for each of the categories. We can get images
		 * we don't already have by passing our lowest image score and the
		 * creation date of our newest image.
		 */
		for (Category category : lowCategories) {

			int score = CategoryUtils.getLowestImageScore(category);
			long lastCreatedAt = CategoryUtils.getNewestImageDate(category);

			int attempts = 0;
			while (attempts < MAX_RETRIES) {

				Response response = mApiService.newImages(category.getId(),
						score, lastCreatedAt, NUM_IMAGES_FOR_LOW_CATEGORY);

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
					String body = responseToString(response);
					JSONArray json = new JSONArray(body);
					putImagesInDb(json, false);
					break;
				} catch (IOException e) {
					return false;
				} catch (JSONException e) {
					return false;
				}
			}
		}

		return true;
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
		SQLiteDatabase db = Sprinkles.getDatabase();
		db.beginTransaction();

		try {
			int numImages = array.length();
			for (int i = numImages - 1; i >= 0; i--) {
				JSONObject imageJson = array.getJSONObject(i);

				long id = imageJson.getLong("id");
				ContentValues values = new ContentValues();
				values.put("id", id);
				values.put("lastUpdated", imageJson.getLong("updated_at"));
				values.put("createdAt", imageJson.getLong("created_at"));
				values.put("imgurId", imageJson.getString("imgurId"));
				values.put("redditScore", imageJson.getLong("reddit_score"));
				values.put("nsfw", imageJson.getBoolean("nsfw"));
				values.put("gif", imageJson.getBoolean("gif"));
				values.put("deleted", imageJson.getBoolean("deleted"));
				values.put("reported", imageJson.getBoolean("reported"));

				/* Insert or update depending on the param. */
				if (update) {
					db.update("Images", values, "id=" + id, null);
				} else {
					/*
					 * If the image already exists we don't want to replace it,
					 * as that will delete it, causing cascading deletes of
					 * dependent likes/collections. Instead we'll ignore it and
					 * and continue on. This case shouldn't happen if our image
					 * retrieval logic is sound and bug-free however.
					 */
					db.insertWithOnConflict("Images", null, values,
							SQLiteDatabase.CONFLICT_IGNORE);
				}

				/*
				 * Set the categories for this image. First delete any
				 * categories it may have had before and then recreate them all.
				 */
				db.delete("CategoryImages", "imageId=" + id, null);

				JSONArray categories = imageJson.getJSONArray("categories");
				int numCategories = categories.length();
				for (int j = 0; j < numCategories; j++) {
					ContentValues categoryValues = new ContentValues();
					categoryValues.put("categoryId", categories.getString(j));
					categoryValues.put("imageId", id);
					/*
					 * No existing row should match since we just deleted them,
					 * but we'll do a replace on conflict just in case.
					 */
					db.insertWithOnConflict("CategoryImages", null,
							categoryValues, SQLiteDatabase.CONFLICT_REPLACE);
				}
			}
			// Util.log("Batch successful!");
			db.setTransactionSuccessful();
		} catch (JSONException e) {
			Util.logException(e);
		} finally {
			db.endTransaction();
		}
	}

}
