package com.picdora.channelPlayer;

import java.util.ArrayList;
import java.util.List;

import com.picdora.Timer;
import com.picdora.Util;
import com.picdora.models.ChannelImage;

/**
 * Keep track of all categories in this channel and decide which channel to
 * source an image from next. Calculates weights for each category based on
 * image info, usage history, and likes and then does a weighted random search
 * to decide which category to source from.
 * 
 */
public class CategoryManager {
	private List<CategoryWrapper> mCategories;

	/*
	 * Calculate the time in millis that we can think of as our view reset
	 * point. This is a certain number of days in the past, and the closer the
	 * last view point is to it there's an exponentially better chance of being
	 * chosen.
	 */
	private static final int NUM_DAYS_TILL_VIEW_RESET = 15;
	private static final int HOURS_PER_DAY = 24;
	private static final int MINUTES_PER_HOUR = 60;
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int MILLIS_PER_SECOND = 1000;
	private static final long MILLIS_TILL_VIEW_RESET = NUM_DAYS_TILL_VIEW_RESET
			* HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE
			* MILLIS_PER_SECOND;

	/**
	 * The starting point for the weighting modifier of an already viewed image
	 * compared to an image without any views. This may be further reduced if
	 * the image has multiple views or if the view was recent.
	 */
	private static final double MAX_LAST_SEEN_MODIFIER = 0.15d;

	public CategoryManager() {
		mCategories = new ArrayList<CategoryWrapper>();
	}

	/**
	 * Get the categories added to the manager.
	 * 
	 * @return
	 */
	public List<CategoryWrapper> getCategories() {
		return mCategories;
	}

	/**
	 * Add a category to manage. Does not calculate a weight for the category -
	 * you should recalculate weights manually after adding categories.
	 * 
	 * 
	 * @param category
	 */
	public void addCategory(CategoryWrapper category) {
		/*
		 * Note, weighting calculation isn't done here. Recalculation must be
		 * manually called afterward.
		 */
		if (mCategories.contains(category)) {
			Util.log("Trying to add duplicate category.");
		} else {
			mCategories.add(category);
		}
	}

	/**
	 * Calculate and set weights for all categories added.
	 * 
	 */
	private void setCategoryWeights() {
		/*
		 * TODO: Better weight calculation with all variables (category
		 * likes/dislikes) taken into account. Plus, more testing!
		 */

		/*
		 * No weight if the category is out of images, else initialize weight to
		 * one.
		 */
		for (CategoryWrapper c : mCategories) {
			if (!c.hasImages()) {
				c.setWeight(0);
				continue;
			} else if (!c.needsNewWeight()) {
				continue;
			}

			ChannelImage image = c.peekNextImage();
			// highest possible weighting is 1
			double weight = 1;

			weight *= getLastSeenModifier(image.getViewCount(),
					image.getLastSeen());

			c.setWeight(weight);
		}

	}

	/**
	 * Calculate a modifier for the chance of showing an image based on how long
	 * ago and how many times the image has been seen.
	 * 
	 * @param viewCount
	 * @param lastSeen
	 * @return
	 */
	private double getLastSeenModifier(int viewCount, long lastSeen) {
		/*
		 * Start the modifier at 1, ie no change, and reduce it based on how
		 * many times and how recently it was seen.
		 */
		double modifier = 1.0;
		
		/* If it's never been seen then don't reduce it's chance. */
		if (viewCount == 0) {
			return modifier;
		}

		/*
		 * Our approach to handling already viewed images will be to start with
		 * a maximum fraction of the original weight that the image can have.
		 * This max value is reduced by the number of times the image has been
		 * viewed. Finally, we'll use a cubic function to exponentially increase
		 * the chance the longer ago the image was seen and weight this against
		 * the max
		 */

		/*
		 * Reduced by the number of views. TODO: Use global channel view count?.
		 */
		modifier /= viewCount;

		/*
		 * Exponentially reduced the more recently an image was seen with the
		 * function (x^3 / (x^3 + a)). This has a horizontal asymptote at 1
		 * where we can say the image was viewed long enough ago to not matter,
		 * ie after that the function will return ~1. a is chosen so that this
		 * asymptote is hit roughly at x=1. x will be time normalized around 1
		 * => (how long ago the image was viewed / how much time we want until
		 * the image view doesn't matter much anymore).
		 */
		long millisSinceLastSeen = System.currentTimeMillis() - lastSeen;
		/*
		 * x - ie (how long ago the image was viewed / how much time we want
		 * until the image view doesn't matter much anymore)
		 */
		double fractionOfViewReset = millisSinceLastSeen
				/ (double) MILLIS_TILL_VIEW_RESET;
		double fractionCubed = Math.pow(fractionOfViewReset, 3);
		/*
		 * The function x^3 / (x^3 + a) with 'a' chosen so that roughly f(x)=1
		 * for x>1;.
		 */
		modifier *= fractionCubed / (fractionCubed + 0.05);
		// apply the calculated weighting
		return MAX_LAST_SEEN_MODIFIER * modifier;
	}

	/**
	 * Use the calculated category weights to randomly choose a category.
	 * 
	 * @return
	 */
	public CategoryWrapper nextCategory() {
		if (mCategories.isEmpty()) {
			throw new IllegalStateException("Categories is empty");
		}

		Timer timer = new Timer();
		timer.start();

		// TODO: Optimizations to not recalculate every time.
		setCategoryWeights();
		timer.lap("calculate weights");

		// Compute the total weight of all categories together
		double totalWeight = 0.0d;
		for (CategoryWrapper c : mCategories) {
			totalWeight += c.getWeight();
		}

		// Choose a random category. TODO: More efficient selection?
		double random = Math.random() * totalWeight;
		for (CategoryWrapper c : mCategories) {
			random -= c.getWeight();
			if (random <= 0.0d) {
				// timer.lap("choose random category");
				return c;
			}
		}

		/* Algorithm shouldn't get here unless there is a bug. */
		Util.log("Category selection failure");
		return null;
	}
}