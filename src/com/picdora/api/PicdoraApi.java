package com.picdora.api;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public interface PicdoraApi {
	/*
	 * TODO: For some reason retrofit's default GSON parser errors out on a lot
	 * of the big json files from our server. I gave up trying to fix it, so
	 * we're manually parsing the response with our own json reader right now.
	 */

	/**
	 * Get new images for a category. We want to avoid images we already have,
	 * and get the highest scored images we don't have.
	 * 
	 * @param categoryId
	 *            The id of the category we want images for.
	 * @param score
	 *            The lowest reddit score of the images that we currently have.
	 *            We should already have all images with this score or higher
	 *            that were created before the "last created" param. We are
	 *            asking for images that have a score higher than this but were
	 *            created after ours or, if necessary, images with a lower score
	 *            than this. This should prevent getting duplicates.
	 * 
	 * @param createdAfter
	 *            The date of our most recently created image in unix time. We
	 *            already have all images created before this date that have at
	 *            least the score provided.
	 * @param limit
	 *            The number of images requested.
	 * @return JSON data with the number of images requested. If we receive less
	 *         than this it is because there are not enough unique images to
	 *         meet our request.
	 */
	@GET("/images/top")
	public Response topImages(@Query("category_id") long categoryId, @Query("count") int count);

	/**
	 * Check for updates of images that we already have. Unfortunately it isn't
	 * practical to send a list of what images we have, so we instead give the
	 * last time we updated, and our most recently created image. We will
	 * receive the subset of images that match these, although this may include
	 * images that we don't have we can ignore those.
	 * <p>
	 * We can also place a limit on the batch size of the update. Matching
	 * images will be returned in order of ascending id so we can get subsequent
	 * batches by using the next id.
	 * 
	 * @param id
	 *            Only return images with an id greater than this one. Does not include this id.
	 * @param lastUpdated
	 *            The date of the last time we updated successfully in unix time
	 * @param createdBefore
	 *            The date that all of our images were created before in unix
	 *            time
	 * @param limit
	 *            The largest amount of images we are willing to receive
	 * @return JSON array of images in order of ascending id, with an id that is
	 *         greater than or equal to the minimum given, and that were updated
	 *         after and created before the given dates.
	 * 
	 */
	@GET("/images/update")
	public Response updateImages(@Query("id") int id,
			@Query("last_updated") long lastUpdated,
			@Query("created_before") long createdBefore,
			@Query("limit") int limit);

	@GET("/categories")
	public Response categories();

	@POST("/users/login")
	public Response login(@Query("key") String key);

}
