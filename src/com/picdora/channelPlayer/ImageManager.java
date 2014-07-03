package com.picdora.channelPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Sprinkles;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.picdora.PicdoraPreferences_;
import com.picdora.Timer;
import com.picdora.Util;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.ChannelImage;
import com.picdora.models.Image;

/**
 * Decides what images to show for a channel.
 * 
 */
@EBean
public class ImageManager {
	// TODO: Explore loading all images into cursorlist initially and then
	// pulling them from there. Could be a lot more efficient

	// TODO: Don't go in straight order of reddit score. Take different
	// categories into account and likes (in the future)

	@Pref
	protected PicdoraPreferences_ mPrefs;

	private Channel mChannel;
	private CategoryManager mCategoryManager;
	private volatile List<ChannelImage> mImages;

	/** List of all image ids that have been loaded so far. */
	private volatile List<Integer> mImageIds;

	/**
	 * Initialize the player with the given channel and start loading images to
	 * show.
	 * 
	 * @param channel
	 *            The channel to load that we are going to play.
	 * @param listener
	 *            Callback for when load completes
	 */
	@Background
	public void loadChannel(Channel channel, OnLoadListener listener) {
		ChannelError error = null;

		if (channel == null) {
			error = ChannelError.BAD_CHANNEL;
		} else {
			// init fields
			mChannel = channel;

			mCategoryManager = new CategoryManager(channel);
			mImageIds = new ArrayList<Integer>();
			mImages = new ArrayList<ChannelImage>();

			/*
			 * Create a CategoryWrapper for each category in the channel and add
			 * it to the manager.
			 */
			for (Category c : mChannel.getCategories()) {
				CategoryWrapper wrapper = new CategoryWrapper(c);
				// Only use the category if it has images to show
				if (wrapper.hasImages()) {
					mCategoryManager.addCategory(wrapper);
				}
			}

			/*
			 * If no categories were successfully added then we have no images
			 * to show.
			 */
			if (mCategoryManager.getCategories().isEmpty()) {
				error = ChannelError.NO_IMAGES;
			}

		}

		// alert the listener on the ui thread of the result
		loadChannelCompleted(error, listener);
	}

	/**
	 * Return the result of loading the channel on the UI Thread.
	 * 
	 * @param error
	 *            If the load failed then this will be the cause of the fail. On
	 *            success this should be null.
	 * @param listener
	 *            The listener to alert. If null then no callback will be made.
	 */
	@UiThread
	protected void loadChannelCompleted(ChannelError error,
			OnLoadListener listener) {
		// if no listener was passed then we have nobody to alert
		if (listener == null) {
			return;
		}
		// if an error was passed then alert the failure
		else if (error != null) {
			listener.onFailure(error);
		}
		// No error. Success!
		else {
			listener.onSuccess();
		}
	}

	public interface OnGetChannelImageResultListener {
		public void onGetChannelImageResult(ChannelImage image);
	}

	/**
	 * Get the image at the given index in a background thread.
	 * 
	 * @param index
	 *            The index of the image to retrieve
	 * @param replace
	 *            True if a we should replace the image at the given index with
	 *            a new one, for the case where the first image was bad.
	 * @param listener
	 *            Callback for when the image is ready
	 */
	@Background
	public void getImageAsync(int index, boolean replace,
			OnGetChannelImageResultListener listener) {
		ChannelImage result = getImage(index, replace);

		// return the image on the ui thread
		returnGetImageAsyncResult(result, listener);
	}

	@UiThread
	protected void returnGetImageAsyncResult(ChannelImage image,
			OnGetChannelImageResultListener listener) {
		if (listener != null) {
			listener.onGetChannelImageResult(image);
		}
	}

	/**
	 * Get the image at the given index on the ui thread. Careful, this might do
	 * a db access.
	 * 
	 * @param index
	 *            The index of the image to retrieve
	 * @param replace
	 *            True if we should replace the image at the given index with a
	 *            new one, for the case where the first image was bad.
	 */
	public synchronized ChannelImage getImage(int index, boolean replace) {

		// If they have requested a replacement then get a new image and replace
		// the old one, but only if it's one we've already loaded
		if (replace && index >= mImages.size()) {
			/*
			 * Try to get another image to use as a replacement. If successful
			 * swap it out with the current image at the index. If getting
			 * another image fails then keep the current one.
			 */
			ChannelImage replacement = getNextImage();
			if (replacement != null) {
				mImages.set(index, replacement);
			}
			return mImages.get(index);
		} else {
			// keep getting new images until either we have enough to satisfy
			// the index requested, or we don't have anymore to give
			while (mImages.size() <= index) {
				ChannelImage img = getNextImage();
				if (img == null) {
					break;
				} else {
					mImages.add(img);
				}
			}

			// return the index requested if we have enough images, otherwise
			// wrap
			// around
			return mImages.get(index % mImages.size());
		}
	}

	/**
	 * Get the next image to show.
	 * 
	 * @return
	 */
	private ChannelImage getNextImage() {
		/*
		 * Get a randomized category from the manager and use that category's
		 * next image.
		 */
		CategoryWrapper cat = mCategoryManager.nextCategory();
		if (cat == null) {
			return null;
		} else {
			return cat.nextImage();
		}
	}

	/**
	 * Run a query generated with {@link #generateImageQuerySql(Category, int)}.
	 * Parses the results into images and adds the ids to
	 * {@link ImageManager#mImageIds} to avoid duplicates later. Places the
	 * result images into the given collection and returns the number of images
	 * retrieved.
	 * 
	 * @param query
	 *            The sql query to run, generated with
	 *            {@link #generateImageQuerySql(Category, int)}
	 * @param images
	 *            The collection that the result images should be added to.
	 * @return resultCount The number of images retrieved from the db
	 */
	private synchronized int runImageQuery(String query,
			Collection<ChannelImage> images) {

		/*
		 * Run query and parse result into views and images with sprinkles.
		 * Since our query specifies values for when a view is null we can
		 * safely instantiate a new view for each image using the cursorlists.
		 */
		SQLiteDatabase db = Sprinkles.getDatabase();
		Cursor c = db.rawQuery(query, null);
		CursorList<Image> imagesCursor = new CursorList<Image>(c, Image.class);
		CursorList<ChannelImage> viewsCursor = new CursorList<ChannelImage>(c,
				ChannelImage.class);

		/*
		 * Get each view and image. The view only has the image id at the moment
		 * so give it the image object as well. Store the image id to prevent
		 * getting it again and add it to the result list.
		 */
		int resultCount = imagesCursor.size();
		for (int i = 0; i < resultCount; i++) {
			ChannelImage view = viewsCursor.get(i);
			Image image = imagesCursor.get(i);
			view.setImage(image);
			images.add(view);
			mImageIds.add((int) image.getId());
		}

		imagesCursor.close();
		viewsCursor.close();

		return resultCount;
	}

	/**
	 * Generate a query string that can be run to get images from a category
	 * based on this channel's settings. This takes into account the image ids
	 * in {{@link ImageManager#mImageIds} so that duplicate images aren't
	 * retrieved. This means the query should not be saved for use later and
	 * must be regenerated every time new images are retrieved. This should also
	 * not be called concurrently with a db call as there would be a race
	 * condition with the ids retrieved.
	 * 
	 * 
	 * @param category
	 *            The category to get images in
	 * @param numImagesToGet
	 *            The query limit
	 * @return
	 */
	private String generateImageQuerySql(Category category, int numImagesToGet) {
		// Build subquery for images in category
		String imagesFromCategorySubquery = "(SELECT distinct imageId FROM ImageCategories WHERE categoryId="
				+ category.getId() + ")";

		/*
		 * The columns we want to get, combining the image data with the view
		 * data. For the images we can ignore deleted and reported as we only
		 * get images where that is false. We also don't need the creation or
		 * update times. For the views we want all columns, but expect null
		 * values for images that haven't been viewed yet. In that case we
		 * should initialize the view to match the channel and image, have no
		 * views, and a neutral liked status.
		 */
		String columns = String.format(Locale.US,
				"id, imgurId, redditScore, nsfw, gif, "
						+ "ifnull(channelId, %d) as channelId, "
						+ "ifnull(imageId, Images.id) as imageId, "
						+ "ifnull(count, 0) as count, "
						+ "ifnull(lastSeen, 0) as lastSeen, "
						+ "ifnull(liked, %d) as liked", mChannel.getId(),
				ChannelImage.LIKE_STATUS.NEUTRAL.getId());

		StringBuilder query = new StringBuilder();

		// Get image and view data. Using outer join images without views will
		// have null view data.
		query.append("SELECT ");
		query.append(columns);
		query.append(" FROM Images LEFT OUTER JOIN Views ON Images.id=Views.imageId");

		// limit images to the categories in the channel
		query.append(" WHERE Images.id IN ");
		query.append(imagesFromCategorySubquery);

		// limit views to ones from this channel
		/*
		 * TODO: Incorporate view information from other channels as well so
		 * duplicate images aren't seen across channels.
		 */
		query.append(" AND (channelId=");
		query.append(mChannel.getId());
		query.append(" OR channelId IS NULL)");
		// and not disliked
		// TODO: Check dislikes from other channels too!
		query.append(" AND (liked != ");
		query.append(ChannelImage.LIKE_STATUS.DISLIKED.getId());
		query.append(" OR liked IS NULL)");

		// add the gif setting
		switch (mChannel.getGifSetting()) {
		case ALLOWED:
			break;
		case NONE:
			query.append(" AND gif=0");
			break;
		case ONLY:
			query.append(" AND gif=1");
			break;
		}

		// add the nsfw setting
		if (!mPrefs.showNsfw().get()) {
			query.append(" AND nsfw=0");
		}

		// not reported or deleted
		query.append(" AND reported=0 AND deleted=0");

		// not one of the images we've already loaded
		query.append(" AND id NOT IN ");
		query.append(getImageIdsInUse());

		/*
		 * order by view count, then liked vs neutral (Note: This depends on how
		 * LIKE_STATUS enum sets enum values so careful with that changing),
		 * then reddit score.
		 */
		query.append(" ORDER BY count ASC, liked DESC, redditScore DESC");
		// add limit
		query.append(" LIMIT ");
		query.append(numImagesToGet);

		return query.toString();
	}

	/**
	 * Get a string of the image ids that have already been loaded, comma
	 * separated and in parenthesis.
	 * 
	 * @return
	 */
	private String getImageIdsInUse() {
		return "(" + TextUtils.join(",", mImageIds) + ")";
	}

	/**
	 * Callback methods for when the player is ready to start playing
	 * 
	 */
	public interface OnLoadListener {
		public void onSuccess();

		public void onFailure(ChannelError error);
	}

	/** Error codes for loading a channel */
	public enum ChannelError {

		/** The channel doesn't contain any images to show */
		NO_IMAGES,
		/** The given channel is null or is non functional */
		BAD_CHANNEL;
	}

	/**
	 * Encapsulate image management by category.
	 * 
	 */
	private class CategoryWrapper {
		private Category category;

		/** The next set of images to use. */
		private LinkedList<ChannelImage> imageQueue;
		/**
		 * Whether all images available to this channel have been loaded
		 * already. If true we can stop trying to load more.
		 */
		private boolean allImagesUsed = false;
		/**
		 * The relative weight assigned to this category for use in randomly
		 * choosing which category to source pictures from.
		 */
		private double categoryWeight;
		/**
		 * The size that we'll try to keep the image queue at so we have enough
		 * images without doing too many loads.
		 * 
		 */
		private static final int TARGET_QUEUE_SIZE = 25;
		/**
		 * How low the queue count can go before we try to refill it to the
		 * target size.
		 */
		private static final int QUEUE_REFILL_THRESHOLD = TARGET_QUEUE_SIZE / 2;

		public CategoryWrapper(Category category) {
			this.category = category;
			// initialize queue and fill it
			imageQueue = new LinkedList<ChannelImage>();
			refillQueue();
		}

		/**
		 * Attempt to add more images to the queue by generating and running an
		 * image query. Avoids images already in use this session. If no more
		 * images are available to the category then {@link #allImagesUsed} is
		 * set to true.
		 * 
		 * 
		 */
		private void refillQueue() {
			//Timer timer = new Timer();
			//timer.start();
			int numImagesToGet = TARGET_QUEUE_SIZE;
			String query = generateImageQuerySql(category, numImagesToGet);
			int resultCount = runImageQuery(query, imageQueue);
			// randomized images
			Collections.shuffle(imageQueue,
					new Random(System.currentTimeMillis()));
			/*
			 * If the actual number of images retrieved is less than we asked
			 * for there aren't enough images and we shouldn't try to query
			 * again. We already have them all!
			 */
			if (resultCount < numImagesToGet) {
				allImagesUsed = true;
			}
			//timer.lap("Queue refilled");
		}

		/**
		 * Whether this category has more images to show that haven't yet been
		 * shown this session.
		 * 
		 * @return
		 */
		public boolean hasImages() {
			return !imageQueue.isEmpty();
		}

		/**
		 * Peek at the next image this category has to offer. Null if the
		 * category doesn't have any more images.
		 * 
		 * @return
		 */
		public ChannelImage peekNextImage() {
			return imageQueue.peek();
		}

		/**
		 * Get the next image this category has to offer. Null if the category
		 * doesn't have any more images. May run a db query to retrieve more
		 * images so run in background.
		 * 
		 * @return
		 */
		public ChannelImage nextImage() {
			ChannelImage image = imageQueue.poll();
			// Check if we need to get more images
			if (!allImagesUsed && imageQueue.size() < QUEUE_REFILL_THRESHOLD) {
				refillQueue();
			}
			return image;
		}

		/**
		 * Set the relative weight of this category.
		 * 
		 * @param weight
		 */
		public void setWeight(double weight) {
			categoryWeight = weight;
		}

		/**
		 * Get the weight assigned to this category.
		 * 
		 * @return
		 */
		public double getWeight() {
			return categoryWeight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((category == null) ? 0 : category.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof CategoryWrapper)) {
				return false;
			}

			CategoryWrapper other = (CategoryWrapper) obj;
			if (category == null) {
				if (other.category != null) {
					return false;
				}
			} else if (!category.equals(other.category)) {
				return false;
			}
			return true;
		}

	}

	/**
	 * Keep track of all categories in this channel and decide which channel to
	 * source an image from next. Calculates weights for each category based on
	 * image info, usage history, and likes and then does a weighted random
	 * search to decide which category to source from.
	 * 
	 */
	private class CategoryManager {
		private List<CategoryWrapper> categories;
		private Channel channel;

		public CategoryManager(Channel channel) {
			this.channel = channel;

			int numCategories = channel.getCategories().size();
			categories = new ArrayList<ImageManager.CategoryWrapper>(
					numCategories);
		}

		/**
		 * Get the categories added to the manager.
		 * 
		 * @return
		 */
		public List<CategoryWrapper> getCategories() {
			return categories;
		}

		/**
		 * Add a category to manage. Does not calculate a weight for the
		 * category - you should recalculate weights manually after adding
		 * categories.
		 * 
		 * 
		 * @param category
		 */
		public void addCategory(CategoryWrapper category) {
			/*
			 * Note, weighting calculation isn't done here. Recalculation must
			 * be manually called afterward.
			 */
			if (categories.contains(category)) {
				Util.log("Trying to add duplicate category.");
			} else {
				categories.add(category);
			}
		}

		/**
		 * Calculate and set weights for all categories added.
		 * 
		 */
		public void recalculateCategoryWeights() {
			/*
			 * TODO: Better weight calculation with all variables taken into
			 * account. Also, refactor to modularize. Plus, more testing!
			 */
			long currentTime = new Date().getTime();

			/*
			 * Calculate the time in millis that we can think of as our view
			 * reset point. This is a certain number of days in the past, and
			 * the closer the last view point is to it there's an exponentially
			 * better chance of being chosen.
			 */
			int numDaysTillReshow = 15;
			int hoursPerDay = 24;
			int minutesPerHour = 60;
			int secondsPerMinute = 60;
			int millisPerSecond = 1000;
			long millisUntilViewReset = numDaysTillReshow * hoursPerDay
					* minutesPerHour * secondsPerMinute * millisPerSecond;

			/*
			 * No weight if the category is out of images, else initialize
			 * weight to one.
			 */
			for (CategoryWrapper c : categories) {
				if (!c.hasImages()) {
					c.setWeight(0);
					break;
				}

				double weight = 1;
				ChannelImage image = c.peekNextImage();
				/* Weight based on how long ago the image was seen. */
				if (image.getViewCount() > 0) {

					/*
					 * Our approach to handling already viewed images will be to
					 * start with a maximum fraction of the original weight that
					 * the image can have. This max value is reduced by the
					 * number of times the image has been viewed. Finally, we'll
					 * use a cubic function to exponentially increase the chance
					 * the longer ago the image was seen and weight this against
					 * the max
					 */

					/* The starting point for the weighting adjustment compared to an unviewed image weighting. */
					double maxViewWeighting = 0.15d;
					/*
					 * Reduced by the number of views. TODO: Use global channel
					 * view count?.
					 */
					maxViewWeighting /= image.getViewCount();
					/*
					 * Exponentially reduced the more recent an image was seen
					 * with the function (x^3 / (x^3 + a)). This has a horizontal
					 * asymptote at 1 where we can say the image was viewed long
					 * enough ago to not matter, ie after that the function will
					 * return ~1. a is chosen so that this asymptote is hit
					 * roughly at x=1. x will be time normalized around 1 => (how long ago the image was
					 * viewed / how much time we want until the image view
					 * doesn't matter much anymore).
					 */
					long millisSinceLastSeen = currentTime - image.getLastSeen();
					/*
					 * x - ie (how long ago the image was viewed / how much time
					 * we want until the image view doesn't matter much anymore)
					 */
					double fractionOfViewReset = millisSinceLastSeen
							/ (double) millisUntilViewReset;
					double fractionCubed = Math.pow(fractionOfViewReset, 3);
					/*
					 * The function x^3 / (x^3 + a) with a chosen so that
					 * roughly f(x)=1 for x>1;.
					 */
					double viewModifier = fractionCubed
							/ (fractionCubed + 0.05);
					// apply the calculated weighting
					weight *= maxViewWeighting * viewModifier;
				}

				c.setWeight(weight);
			}

		}

		/**
		 * Use the calculated category weights to randomly choose a category.
		 * 
		 * @return
		 */
		public CategoryWrapper nextCategory() {
			if (categories.isEmpty()) {
				throw new IllegalStateException("Categories is empty");
			}

			//Timer timer = new Timer();
			//timer.start();

			// TODO: Optimizations to not recalculate every time.
			recalculateCategoryWeights();
			//timer.lap("calculate weights");

			// Compute the total weight of all categories together
			double totalWeight = 0.0d;
			for (CategoryWrapper c : categories) {
				totalWeight += c.getWeight();
			}

			// Choose a random category. TODO: More efficient selection?
			double random = Math.random() * totalWeight;
			for (CategoryWrapper c : categories) {
				random -= c.getWeight();
				if (random <= 0.0d) {
					//timer.lap("choose random category");
					return c;
				}
			}

			/* Algorithm shouldn't get here unless there is a bug. */
			Util.log("Category selection failure");
			return null;
		}
	}
}
