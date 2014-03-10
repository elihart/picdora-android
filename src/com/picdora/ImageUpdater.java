package com.picdora;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.sprinkles.Sprinkles;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.picdora.api.PicdoraApi;

/**
 * This class keeps the local image database up to date with the server. This
 * includes updating existing images and adding new images.
 * 
 * @author Eli
 * 
 */

@EBean
public class ImageUpdater {
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

	@Pref
	PicdoraPreferences_ prefs;

	PicdoraApi api;

	public ImageUpdater() {
		// empty constructor for enhanced class
	}

	@Background
	public void getNewImages() {
		mStartTime = new Date().getTime();
		// get all images on the server that have changed since the last time we
		// updated. Start with id 0 and increment in batches till we have them
		// all
		mLastUpdated = prefs.lastUpdated().get();
		mIdIndex = 0;

		// We retry on error, but if we run into too many consecutive errors we
		// give up
		mNumFailures = 0;

		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(
				PicdoraApiClient.BASE_URL).build();

		api = restAdapter.create(PicdoraApi.class);

		Util.log("Last updated " + new Date(mLastUpdated));
		getNewImageBatch(0);
	}

	// has to be run on the ui thread so http async can work
	@UiThread
	protected void getNewImageBatch(final int index) {
		Util.log("Getting update at id " + index);

		api.newImages(index, mLastUpdated / 1000, BATCH_SIZE, new Callback<Response>() {

			@Override
			public void success(Response response, Response arg1) {
				try {
					InputStream is = response.getBody().in();
					BufferedReader r = new BufferedReader(new InputStreamReader(is));
					StringBuilder total = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
					    total.append(line);
					}
					is.close();
					
					handleNewImageSuccess(new JSONObject(total.toString()));
					//InputStream is = response.getBody().in();

					//handleNewImageSuccess(new JsonFactory().createJsonParser(is));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void failure(RetrofitError error) {
				error.printStackTrace();
			}
		});

		// To convert to unix time we have to go from millisecond to seconds
		// ImageManager.getNewImagesFromServer(index, mLastUpdated / 1000,
		// BATCH_SIZE, new OnImageUpdateListener() {
		//
		// @Override
		// public void onSuccess(JSONObject json) {
		// handleNewImageSuccess(json);
		// }
		//
		// @Override
		// public void onFailure() {
		// mNumFailures++;
		// // try again if we haven't hit the retry limit
		// Util.log("Update failure. Retrying " + index);
		// if (mNumFailures < FAILURE_LIMIT) {
		// getNewImageBatch(index);
		// }
		//
		// }
		// });
	}

	@Background
	protected void handleNewImageSuccess(JSONObject json) {
		// reset consecutive error count
		mNumFailures = 0;

		// get the image data
		JSONArray arr = null;
		try {
			arr = json.getJSONArray("images");
		} catch (JSONException e1) {
			Util.log("Error getting images json from update");
		}

		// add images to database or quit if there was an error
		if (arr != null) {
			Util.log("Updating " + arr.length() + " images in the db");
			Date start = new Date();
			addNewImagesToDb(arr);
			Date end = new Date();
			Util.log("DB update took " + (end.getTime() - start.getTime()));
		} else {
			// unknown error getting images, let's not continue
			Util.log("Error updating. Stopping.");
			return;
		}

		// check for the next id
		try {
			mIdIndex = json.getInt("nextId");
		} catch (JSONException e) {
			mIdIndex = -1;
		}

		if (mIdIndex == -1) {
			// We're done! No more images to get
			Util.log("Finished updating");
			prefs.lastUpdated().put(mStartTime);
		} else {
			// do another batch starting with the returned id
			getNewImageBatch(mIdIndex);
		}
	}

	protected void addNewImagesToDb(JSONArray array) {
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

				if (id == -1) {
					Util.log("Db insert error");
				}
			}
			Util.log("Batch successful!");
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
}
