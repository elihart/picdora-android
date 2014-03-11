package com.picdora.sync;

import java.io.IOException;
import java.util.Date;

import org.androidannotations.annotations.EBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.client.Response;
import se.emilsjolander.sprinkles.Sprinkles;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.picdora.Util;

/**
 * Keeps our local image db up to date with the server
 * 
 */
@EBean
public class ImageSyncer extends Syncer {
	// TODO: Send updates to the server about changes to the images, such as
	// gif/deleted/reported changes

	// The last time we updated images. Give this to the server so it knows what
	// images we need
	private long mLastUpdated;
	// Image updates come in batches. We tell the server what image Id to start
	// with and it gives us images including and after that id. If there
	// are more images than the batch size the server gives us the id of the
	// next
	// image so we know where to start on the next batch.
	private int mIdIndex;

	// The number of images to get from the server in each batch. Null if we
	// want to go with the server default
	private static final Integer BATCH_SIZE = 1000;
	// The number of consecutive failures we need to hit before we give up on
	// updating.
	private static final int FAILURE_LIMIT = 3;
	// Keep track of consecutive failures so we know when to give up. Reset to 0
	// on a success
	private int mNumFailures;
	// keep track of when we start the update so we can save the last updated
	// time. If we use the end time there is a race condition of missing images
	// that are updated after we pass their id
	private long mStartTime;

	@Override
	public void sync() {
		getNewImages();

		// TODO: Update existing images when things like gif/deleted/reported
		// status changes
	}

	/**
	 * Retrieve new images from the database that were created since we last
	 * synced. Images are retrieved in batches to prevent too much data from
	 * being loaded at once. If the server has more images than we want in our
	 * batch size it will give us the id of the next image to use in our next
	 * batch
	 */
	public void getNewImages() {
		mStartTime = new Date().getTime();
		// get all images on the server that have changed since the last time we
		// updated. Start with id 0 and increment in batches till we have them
		// all
		mLastUpdated = mPrefs.lastUpdated().get();
		Util.log("Last updated " + new Date(mLastUpdated));
		// need to convert from millis to seconds for Unix time
		mLastUpdated /= 1000;
		// the id to start each batch with. Start at 0 and will be incremented
		// on each successful batch. When the server doesn't have anymore images
		// it won't give us a next id and this will be set to -1 to indicate we
		// are finished
		mIdIndex = 0;

		// We retry on error, but if we run into too many consecutive errors we
		// give up
		mNumFailures = 0;

		
		while (mIdIndex != -1 && mNumFailures < FAILURE_LIMIT) {
			getNewImageBatch();
		}

		if (mIdIndex == -1) {
			// success!
			mPrefs.lastUpdated().put(mStartTime);
		} else {
			// failure
		}
	}

	private void getNewImageBatch() {
		Response response = mApiService.newImages(mIdIndex,
				mLastUpdated, BATCH_SIZE);
		if (response == null || response.getBody() == null) {
			handleGetImageFailure();
			return;
		}

		try {
			String body = responseToString(response);
			JSONObject json = new JSONObject(body);
			// the returned json object contains two members, a json array
			// "images" that contains the image data, and an int giving us the
			// id to start the next batch with
			addNewImagesToDb(json.getJSONArray("images"));
			mIdIndex = getNextId(json);
			// reset the failure count on successful batch. We only count consecutive failures
			mNumFailures = 0;
		} catch (IOException e) {
			handleGetImageFailure();
		} catch (JSONException e) {
			handleGetImageFailure();
		}
	}

	private void handleGetImageFailure() {
		mNumFailures++;
	}

	/**
	 * Get the "nextId" field from the json object returned from the server
	 * 
	 * @param json
	 * @return -1 if no id was included. This indicates that there are no
	 *         further images to get
	 */
	private int getNextId(JSONObject json) {
		// check for the next id
		try {
			return json.getInt("nextId");
		} catch (JSONException e) {
			return -1;
		}
	}

	/**
	 * Parse the json array into image data and save the images to the database
	 * 
	 * @param array
	 */
	protected void addNewImagesToDb(JSONArray array) {
		//Util.log("Updating " + array.length() + " images in the db");
		//Date start = new Date();
		if (array == null || array.length() == 0) {
			return;
		}

		SQLiteDatabase db = Sprinkles.getDatabase();
		db.beginTransaction();

		try {
			int numImages = array.length();
			for (int i = numImages - 1; i >= 0; i--) {
				JSONObject imageJson = array.getJSONObject(i);

				ContentValues values = new ContentValues();
				values.put("id", imageJson.getLong("id"));
				values.put("imgurId", imageJson.getString("imgurId"));
				values.put("redditScore", imageJson.getLong("reddit_score"));
				values.put("nsfw", imageJson.getBoolean("nsfw"));
				values.put("gif", imageJson.getBoolean("gif"));
				values.put("categoryId", imageJson.getLong("category_id"));

				long id = db.insertWithOnConflict("Images", null, values,
						SQLiteDatabase.CONFLICT_REPLACE);
			}
			//Util.log("Batch successful!");
			db.setTransactionSuccessful();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}

		//Date end = new Date();
		//Util.log("DB update took " + (end.getTime() - start.getTime()));
	}

}
