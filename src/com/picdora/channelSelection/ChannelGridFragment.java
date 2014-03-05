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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.R;
import com.picdora.models.Channel;

@EFragment(R.layout.fragment_selection_grid)
public class ChannelGridFragment extends Fragment {
	@ViewById
	GridView grid;

	@Bean
	ChannelListAdapter adapter;

	private OnChannelClickListener listener;
	private boolean multiSelect = false;
	private List<Channel> selectedChannels = new ArrayList<Channel>();

	@AfterViews
	void initViews() {
		// TODO: Load list in background and show loading icon
		grid.setAdapter(adapter);

		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;
		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		grid.setOnScrollListener(listener);

		grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				channelClicked(adapter.getItem(arg2));
			}
		});
	}

	private void channelClicked(Channel channel) {
		// don't alert the listener of the click until after we have updated the
		// list
		if (multiSelect) {
			if (selectedChannels.contains(channel)) {
				selectChannel(channel);
			} else {
				deselectChannel(channel);
			}
		}

		if (listener != null) {
			listener.onChannelClick(channel);
		}
	}

	private void deselectChannel(Channel channel) {
		selectedChannels.remove(channel);
		// Unhighlight channel
	}

	private void selectChannel(Channel channel) {
		selectedChannels.add(channel);
		// highlight channel

	}

	public void setChannels(List<Channel> channels) {
		adapter.setChannels(channels);
		clearSelectedChannels();				
	}

	public void setOnChannelClickListener(OnChannelClickListener listener) {
		this.listener = listener;
	}

	public void clearSelectedChannels() {
		for(Channel channel : selectedChannels){
			deselectChannel(channel);
		}
	}

	/**
	 * Whether multiple channels can be selected
	 * 
	 * @param selectMultiple
	 */
	public void setMultiSelect(boolean selectMultiple) {
		multiSelect = selectMultiple;
	}

	public List<Channel> getSelectedChannels() {
		return selectedChannels;
	}

	public interface OnChannelClickListener {
		public void onChannelClick(Channel channel);
	}
}
