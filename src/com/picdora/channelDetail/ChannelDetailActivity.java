package com.picdora.channelDetail;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.picdora.ChannelUtil;
import com.picdora.PicdoraActivity;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.models.Channel;
import com.picdora.ui.PicdoraDialog;

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
		ChannelUtil.playChannel(mChannel, this,  true);
	}

	@OptionsItem
	protected void deleteChannel() {
		new PicdoraDialog.Builder(this)
				.setTitle(R.string.channel_detail_delete_dialog_title)
				.setMessage(R.string.channel_detail_delete_dialog_message)
				.setPositiveButton(
						R.string.channel_detail_delete_dialog_positive,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// delete channel and finish
								mChannel.delete();
								finish();
							}
						})
				.setNegativeButton(
						R.string.channel_detail_delete_dialog_negative, null)
				.show();

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
