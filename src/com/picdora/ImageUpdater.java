package com.picdora;

import java.util.Date;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.picdora.ImageManager.OnImageUpdateListener;

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
	private static final Integer BATCH_SIZE = null;
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

	public ImageUpdater() {
		// empty constructor for enhanced class
	}

	@Background
	public void getUpdates() {

		mStartTime = new Date().getTime();
		// get all images on the server that have changed since the last time we
		// updated. Start with id 0 and increment in batches till we have them
		// all
		mLastUpdated = prefs.lastUpdated().get();
		mIdIndex = 0;
		// We retry on error, but if we run into too many consecutive errors we
		// give up
		mNumFailures = 0;

		Util.log("Last updated " + new Date(mLastUpdated));
		doUpdate(0);

	}

	// has to be run on the ui thread so http async can work
	@UiThread
	protected void doUpdate(final int index) {
		Util.log("Getting update at id " + index);
		ImageManager.getImageUpdates(index, mLastUpdated, BATCH_SIZE,
				new OnImageUpdateListener() {

					@Override
					public void onSuccess(JSONObject json) {
						handleUpdateSuccess(json);
					}

					@Override
					public void onFailure() {
						mNumFailures++;
						// try again if we haven't hit the retry limit
						Util.log("Update failure. Retrying " + index);
						if (mNumFailures < FAILURE_LIMIT) {
							doUpdate(index);
						}

					}
				});
	}

	@Background
	protected void handleUpdateSuccess(JSONObject json) {
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
			ImageManager.saveImagesToDb(arr);
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
			doUpdate(mIdIndex);
		}
	}

}
