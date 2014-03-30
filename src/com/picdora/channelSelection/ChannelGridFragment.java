package com.picdora.channelSelection;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;
import com.picdora.models.Channel;

@EFragment(R.layout.fragment_channel_selection)
public class ChannelGridFragment extends Fragment implements
		OnItemClickListener {
	@ViewById
	protected RelativeLayout noChannelsView;
	@ViewById
	protected GridView grid;
	@ViewById
	protected ProgressBar progress;

	@Bean
	ChannelListAdapter adapter;

	private OnChannelClickListener mChannelSelectListener;
	private boolean mMultiSelect = false;
	private List<Channel> mSelectedChannels = new ArrayList<Channel>();

	// the clicked channel
	private OnItemClickListener mParentItemListener;

	@AfterViews
	void initViews() {
		// TODO: Load list in background and show loading icon
		grid.setAdapter(adapter);

		setProgressVisible(true);

		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;

		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		
		grid.setOnScrollListener(listener);

		grid.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Channel channel = adapter.getItem(position);
		channelClicked(channel);

		if (mParentItemListener != null) {
			mParentItemListener.onItemClick(parent, view, position, id);
		}

	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mParentItemListener = listener;
	}

	private void setProgressVisible(boolean visible) {
		if (visible) {
			progress.setVisibility(View.VISIBLE);
		} else {
			progress.setVisibility(View.GONE);
		}
	}

	private void channelClicked(Channel channel) {
		// don't alert the listener of the click until after we have updated the
		// list
		if (mMultiSelect) {
			if (mSelectedChannels.contains(channel)) {
				selectChannel(channel);
			} else {
				deselectChannel(channel);
			}
		}

		if (mChannelSelectListener != null) {
			mChannelSelectListener.onChannelClicked(channel);
		}
	}

	private void deselectChannel(Channel channel) {
		mSelectedChannels.remove(channel);
		// Unhighlight channel
	}

	private void selectChannel(Channel channel) {
		mSelectedChannels.add(channel);
		// highlight channel

	}

	public void setChannels(List<Channel> channels) {
		adapter.setChannels(channels);
		clearSelectedChannels();

		if (adapter.isEmpty()) {
			noChannelsView.setVisibility(View.VISIBLE);
			grid.setVisibility(View.GONE);
		} else {
			noChannelsView.setVisibility(View.GONE);
			grid.setVisibility(View.VISIBLE);
		}

		setProgressVisible(false);
	}

	public void setOnChannelClickListener(OnChannelClickListener listener) {
		this.mChannelSelectListener = listener;
	}

	public void clearSelectedChannels() {
		for (Channel channel : mSelectedChannels) {
			deselectChannel(channel);
		}
	}

	/**
	 * Whether multiple channels can be selected
	 * 
	 * @param selectMultiple
	 */
	public void setMultiSelect(boolean selectMultiple) {
		mMultiSelect = selectMultiple;
	}

	public List<Channel> getSelectedChannels() {
		return mSelectedChannels;
	}

	public interface OnChannelClickListener {
		public void onChannelClicked(Channel channel);
	}

}
