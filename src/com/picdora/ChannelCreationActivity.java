package com.picdora;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Channel.GifSetting;
import com.picdora.ui.SlidingMenuHelper;

@EActivity(R.layout.activity_channel_creation)
public class ChannelCreationActivity extends PicdoraActivity {
	@ViewById
	RadioGroup gifSetting;
	@ViewById
	ListView categoryList;
	@ViewById
	EditText channelName;
	@Bean
	CategoryListAdapter categoryListAdapter;

	private Set<Category> mSelectedCategories = new HashSet<Category>();

	@AfterViews
	void initViews() {
		SlidingMenuHelper.addMenuToActivity(this, false);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		categoryList.setAdapter(categoryListAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.channel_creation, menu);
		return true;
	}

	@AfterViews
	void initClickListener() {
		categoryList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				// toggle the check in the view
				CategoryItemView itemView = (CategoryItemView) view;
				itemView.toggleChecked();

				// set the item as selected
				CategoryItem categoryItem = categoryListAdapter
						.getItem(position);
				categoryItem.selected = itemView.isChecked();
			}
		});
	}
	
	@Click
	void channelNameClicked(){
		channelName.setError(null);
	}

	@Click
	void createButtonClicked() {
		GifSetting gif = getGifSetting();

		String name = channelName.getText().toString();

		List<Category> categories = categoryListAdapter.getSelectedCategories();

		if (categories.isEmpty()) {
			Util.makeBasicToast(this, "You must select at least one category!");
			return;
		} else if (Util.isStringBlank(name)) {
			channelName.setError("You have to give this channel a name!");
			return;
		}

		Channel channel = new Channel(name, categories, gif);
		boolean success = channel.save();
		Util.log("Create channel success? " + success);

		if (success) {
			ChannelHelper.playChannel(channel, this);
			finish();
		}
	}

	/**
	 * Get the gif setting the user has chosen in the gifSetting radio group
	 * 
	 * @return
	 */
	private GifSetting getGifSetting() {
		switch (gifSetting.getCheckedRadioButtonId()) {
		case R.id.gif_none:
			return GifSetting.NONE;
		case R.id.gif_allowed:
			return GifSetting.ALLOWED;
		case R.id.gif_only:
			return GifSetting.ONLY;
		default:
			return GifSetting.ALLOWED;
		}
	}

}
