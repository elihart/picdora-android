package com.picdora.channelDetail;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;

import com.picdora.R;
import com.picdora.likes.LikesFragment;
import com.picdora.models.Channel;

/**
 * Use the Likes Fragment in the channel detail view for only one channel.
 */
@EFragment(R.layout.fragment_basic_grid)
public class DetailLikesFragment extends LikesFragment {
	private ChannelDetailActivity mActivity;
	private Channel mChannel;

	@AfterViews
	protected void initDetailLikes() {
		mActivity = (ChannelDetailActivity) getActivity();
		mChannel = mActivity.getChannel();
		setChannel(mChannel);
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (!isVisibleToUser) {
			/*
			 * We need to hide the action mode when the fragment is paged out of
			 * view. If fragment isn't init'd yet then channel will be null.
			 */
			if (mChannel != null) {
				clearSelection();
			}
			/*
			 * TODO: Better way to handle hiding action mode instead of clearing
			 * selection. We want to save state! 
			 */

		}
	}

}
