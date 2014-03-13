package com.picdora.channelDetail;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.picdora.ChannelHelper;
import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;

/**
 * Show details about a channel, allow edits, deletion, and starting the
 * channel.
 */
@EActivity(R.layout.activity_channel_detail)
@OptionsMenu(R.menu.channel_detail)
public class ChannelDetailActivity extends PicdoraActivity {
	@FragmentById
	protected ChannelInfoFragment infoFragment;
	protected Channel mChannel;

	@AfterViews
	void initChannel() {
		// Load bundled channel and play when ready
		String json = getIntent().getStringExtra("channel");
		mChannel = Util.fromJson(json, Channel.class);

		setActionBarTitle(mChannel.getName());

		infoFragment.setChannel(mChannel);
	}

	@OptionsItem
	protected void playChannel() {
		ChannelHelper.playChannel(mChannel, true, this);
	}

	@OptionsItem
	protected void deleteChannel() {
		// show a confirmation dialog before deleting channel
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.channel_detail_delete_dialog_message)
				.setTitle(R.string.channel_detail_delete_dialog_title)
				.setPositiveButton(
						R.string.channel_detail_delete_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// delete channel and finish
								mChannel.delete();
								finish();
							}
						})
				.setNegativeButton(
						R.string.channel_detail_delete_dialog_negative,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// canceled, don't do anything
							}
						});

		builder.show();

	}

	/**
	 * Change the channel name and save the change to the db async. Update the
	 * action bar title with the new name
	 * 
	 * @param name
	 */
	public void updateChannelName(String name) {
		mChannel.setName(name);
		setActionBarTitle(name);
		mChannel.saveAsync();
	}

}
