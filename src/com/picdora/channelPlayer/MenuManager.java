package com.picdora.channelPlayer;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.picdora.ImageUtils;
import com.picdora.ImageUtils.OnDownloadCompleteListener;
import com.picdora.R;
import com.picdora.models.ChannelImage;
import com.picdora.ui.PicdoraDialog;
import com.picdora.ui.SatelliteMenu.SatelliteMenu;
import com.picdora.ui.SatelliteMenu.SatelliteMenu.SateliteClickedListener;
import com.picdora.ui.SatelliteMenu.SatelliteMenuItem;

/**
 * Handles creation of the menu, click listening for menu items, and responses
 * to the clicks.
 */
@EBean
public class MenuManager {
	@RootContext
	protected ChannelViewActivity mActivity;

	protected SatelliteMenu mMenu;

	/** The image currently being displayed in the visible fragment */
	private ChannelImage mCurrentImage;

	/**
	 * Create a satellite menu to provide access to options for each picture.
	 * 
	 * @param menu
	 */
	public void initMenu(SatelliteMenu menu) {
		mMenu = menu;

		List<SatelliteMenuItem> items = new ArrayList<SatelliteMenuItem>();
		items.add(new SatelliteMenuItem(R.id.sat_item_report,
				R.drawable.ic_sat_menu_item_report));
		items.add(new SatelliteMenuItem(R.id.sat_item_download,
				R.drawable.ic_sat_menu_item_download));
		items.add(new SatelliteMenuItem(R.id.sat_item_star,
				R.drawable.ic_sat_menu_item_star));
		items.add(new SatelliteMenuItem(R.id.sat_item_share,
				R.drawable.ic_sat_menu_item_share));
		items.add(new SatelliteMenuItem(R.id.sat_item_search,
				R.drawable.ic_sat_menu_item_search));

		menu.addItems(items);

		menu.setOnItemClickedListener(new SateliteClickedListener() {

			@Override
			public void eventOccured(int id) {
				/*
				 * Get the current image so we can use it in the response. If it
				 * isn't available then ignore the click because we can't do
				 * anything without an image.
				 */
				mCurrentImage = mActivity.getCurrentImage();
				if (mCurrentImage == null) {
					return;
				}

				switch (id) {
				case R.id.sat_item_search:
					searchClicked();
					break;
				case R.id.sat_item_share:
					shareClicked();
					break;
				case R.id.sat_item_star:
					starClicked();
					break;
				case R.id.sat_item_download:
					downloadClicked();
					break;
				case R.id.sat_item_report:
					reportClicked();
					break;
				}
			}
		});
	}

	/**
	 * Send a browser intent to lookup the current image in google.
	 */
	protected void searchClicked() {
		ImageUtils.lookupImage(mActivity, mCurrentImage.getImgurId());
	}

	/**
	 * Show a dialog to confirm if the user wants to report the current image.
	 * If the user confirms then report the image.
	 */
	protected void reportClicked() {
		new PicdoraDialog.Builder(mActivity)
				.setTitle(R.string.channel_view_report_dialog_title)
				.setMessage(R.string.channel_view_report_dialog_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.channel_view_report_dialog_positive_button,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mActivity.showNotification("Image reported");
								ImageUtils.reportImage(mCurrentImage);
							}
						}).show();

	}

	protected void downloadClicked() {
		mActivity.showNotification("Downloading image...");
		ImageUtils.saveImgurImage(mActivity, mCurrentImage.getImgurId(),
				new OnDownloadCompleteListener() {

					@Override
					public void onDownloadComplete(boolean success) {
						mActivity.showNotification("Downloading complete!");
					}
				});
	}

	/**
	 * Show a dialog to add the current image to a collection.
	 */
	protected void starClicked() {
		// TODO: Create collection select view

		new PicdoraDialog.Builder(mActivity)
				.setTitle(R.string.channel_view_star_dialog_title)
				.setMessage(R.string.channel_view_star_dialog_message)
				.setNegativeButton(R.string.dialog_default_negative, null)
				.setPositiveButton(
						R.string.channel_view_star_dialog_positive_button,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mActivity.showNotification("Image added to collection");
							}
						}).show();
	}

	/**
	 * Lauch the share chooser
	 */
	protected void shareClicked() {
		ImageUtils.shareImage(mActivity, mCurrentImage.getImgurId());
	}

	/**
	 * Close the menu if it is open.
	 */
	public void closeMenu() {
		mMenu.close();
	}

}