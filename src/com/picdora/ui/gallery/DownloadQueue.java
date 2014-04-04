package com.picdora.ui.gallery;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Activity;

import com.picdora.ImageUtils;
import com.picdora.ImageUtils.OnDownloadCompleteListener;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Image;

/**
 * Helper class that downloads a list of images one by one and alerts the
 * progress along the way.
 * 
 */
public class DownloadQueue {
	/** Initial count of images to download */
	private int mCount;
	/** Remaining images to download */
	private Queue<Image> mImages;
	/** Whether we have started downloading yet */
	private boolean mDownloading;
	private Activity mContext;

	/**
	 * Create a new queue of images to download.
	 * 
	 * @param imagesToDownload
	 *            The images that should be downloaded.
	 */
	public DownloadQueue(Activity context, List<Image> imagesToDownload) {
		mImages = new LinkedList<Image>(imagesToDownload);
		mCount = mImages.size();
		mDownloading = false;
		mContext = context;
	}

	/**
	 * Start downloading the images in the queue.
	 * 
	 */
	public void start() {
		/*
		 * Start the download process. Don't start it again if it is already
		 * going
		 */
		if (!mDownloading) {
			if (!mImages.isEmpty()) {
				downloadNextImage();
			}
		}
	}

	/**
	 * Download the next image in the queue. This does not check if the queue is
	 * empty, that is the responsibility of the caller.
	 * 
	 */
	private void downloadNextImage() {
		/* Delegate download to helper */
		ImageUtils.saveImgurImage(mContext, mImages.poll().getImgurId(),
				new OnDownloadCompleteListener() {

					@Override
					public void onDownloadComplete(boolean success) {
						/*
						 * On success show a progress alert and start the next
						 * image downloading if there is another one
						 */
						if (success) {
							int numDownloaded = mCount - mImages.size();
							String progress = mContext.getResources()
									.getString(
											R.string.gallery_download_progress,
											numDownloaded, mCount);
							Util.makeBasicToast(mContext, progress);
							if (!mImages.isEmpty()) {
								downloadNextImage();
							}
						}

						/* On failure show an error and abort the download */
						else {
							String error = mContext.getResources().getString(
									R.string.gallery_download_failure);
							Util.makeBasicToast(mContext, error);
						}
					}
				});
	}
}
