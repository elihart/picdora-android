package com.picdora.channelPlayer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import com.picdora.models.Category;
import com.picdora.models.ChannelImage;

/**
 * Encapsulate image management by category, used by {@link CategoryManager} and
 * {@link ImageManager};
 * 
 */
class CategoryWrapper {
	private Category mCategory;
	/** The {@link ImageManager} that created us. */
	private ImageManager mImageManager;

	/** The next set of images to use. */
	private LinkedList<ChannelImage> mImageQueue;
	/**
	 * Whether all images available to this channel have been loaded already. If
	 * true we can stop trying to load more.
	 */
	private boolean mAllImagesUsed = false;
	/**
	 * The relative weight assigned to this category for use in randomly
	 * choosing which category to source pictures from. This is partially based
	 * on the next image to be shown so the weighting will need to be changed as
	 * the first image in the queue changes.
	 */
	private double mCategoryWeight;
	/**
	 * Whether the categoryWeight needs to be changed because a new image is now
	 * at the front of the queue. This can be either because the last image was
	 * poppped off or the queue was shuffled.
	 */
	private boolean mNeedsNewWeight = true;

	/**
	 * The size that we'll try to keep the image queue at so we have enough
	 * images without doing too many loads.
	 * 
	 */
	private static final int TARGET_QUEUE_SIZE = 25;
	/**
	 * How low the queue count can go before we try to refill it to the target
	 * size.
	 */
	private static final int QUEUE_REFILL_THRESHOLD = TARGET_QUEUE_SIZE / 2;

	public CategoryWrapper(ImageManager imageManager, Category category) {
		mCategory = category;
		mImageManager = imageManager;
		// initialize queue and fill it
		mImageQueue = new LinkedList<ChannelImage>();
		refillQueue();
	}

	/**
	 * Attempt to add more images to the queue by generating and running an
	 * image query. Avoids images already in use this session. If no more images
	 * are available to the category then {@link #mAllImagesUsed} is set to
	 * true.
	 * 
	 * 
	 */
	private void refillQueue() {
		// Timer timer = new Timer();
		// timer.start();
		int numImagesToGet = TARGET_QUEUE_SIZE;
		String query = mImageManager.generateImageQuerySql(mCategory,
				numImagesToGet);
		int resultCount = mImageManager.runImageQuery(query, mImageQueue);
		// randomized images
		Collections
				.shuffle(mImageQueue, new Random(System.currentTimeMillis()));
		/* New image at front of queue. */
		mNeedsNewWeight = true;

		/*
		 * If the actual number of images retrieved is less than we asked for
		 * there aren't enough images and we shouldn't try to query again. We
		 * already have them all!
		 */
		if (resultCount < numImagesToGet) {
			mAllImagesUsed = true;
		}
		// timer.lap("Queue refilled");
	}

	/**
	 * Whether this category has more images to show that haven't yet been shown
	 * this session.
	 * 
	 * @return
	 */
	public boolean hasImages() {
		return !mImageQueue.isEmpty();
	}

	/**
	 * Peek at the next image this category has to offer. Null if the category
	 * doesn't have any more images.
	 * 
	 * @return
	 */
	public ChannelImage peekNextImage() {
		return mImageQueue.peek();
	}

	/**
	 * Get the next image this category has to offer. Null if the category
	 * doesn't have any more images. May run a db query to retrieve more images
	 * so run in background.
	 * 
	 * @return
	 */
	public ChannelImage nextImage() {
		ChannelImage image = mImageQueue.poll();
		// Check if we need to get more images
		if (!mAllImagesUsed && mImageQueue.size() < QUEUE_REFILL_THRESHOLD) {
			refillQueue();
		}
		/* New image at front of queue. */
		mNeedsNewWeight = true;
		return image;
	}

	/**
	 * Set the relative weight of this category.
	 * 
	 * @param weight
	 */
	public void setWeight(double weight) {
		mNeedsNewWeight = false;
		mCategoryWeight = weight;
	}

	/**
	 * Get the weight assigned to this category.
	 * 
	 * @return
	 */
	public double getWeight() {
		return mCategoryWeight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mCategory == null) ? 0 : mCategory.hashCode());
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
		if (mCategory == null) {
			if (other.mCategory != null) {
				return false;
			}
		} else if (!mCategory.equals(other.mCategory)) {
			return false;
		}
		return true;
	}

	/**
	 * Whether the image at the front of the queue has changed since the last
	 * weight was set.
	 * 
	 * @return
	 */
	public boolean needsNewWeight() {
		return mNeedsNewWeight;
	}

}
