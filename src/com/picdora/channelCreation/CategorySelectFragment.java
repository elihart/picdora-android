package com.picdora.channelCreation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.picdora.CategoryHelper;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.channelCreation.ChannelCreationActivity.OnFilterCategoriesListener;
import com.picdora.models.Category;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.STYLE;
import com.picdora.ui.SquareImage;

@EFragment(R.layout.fragment_category_selection)
public class CategorySelectFragment extends Fragment {
	@ViewById
	GridView grid;
	@Bean
	CategoryListAdapter adapter;
	@ViewById
	Button previewButton;
	@ViewById
	Button createButton;
	@ViewById
	TextView numImages;

	private List<Category> selectedCategories;
	private List<Category> allCategories;
	private List<Category> nsfwCategories;
	private List<Category> sfwCategories;
	private ChannelCreationActivity activity;
	private NsfwSetting nsfwFilter;

	@AfterViews
	void initViews() {
		// TODO: Load list in background and show loading icon

		// share the selected categories with the adapter so the adapter knows
		// which images to highlight
		selectedCategories = new ArrayList<Category>();
		adapter.setSelectedCategories(selectedCategories);

		setupCategoryLists();

		nsfwFilter = NsfwSetting.NONE;

		adapter.setCategoryList(sfwCategories);

		grid.setAdapter(adapter);

		// tell the image loader to pause on fling scrolling
		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;
		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		grid.setOnScrollListener(listener);

		// setup click listener that selects/deselects image and reports click
		// to listener
		grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				categoryClicked(view, adapter.getItem(pos));
			}
		});
		
		// set fonts
		FontHelper.setTypeFace(previewButton, STYLE.MEDIUM);
		FontHelper.setTypeFace(createButton, STYLE.MEDIUM);
		FontHelper.setTypeFace(numImages, STYLE.REGULAR);
	}

	private void setupCategoryLists() {
		allCategories = Util.all(Category.class);
		CategoryHelper.sortCategoryListAlphabetically(allCategories);
		nsfwCategories = new ArrayList<Category>();
		sfwCategories = new ArrayList<Category>();

		for (Category c : allCategories) {
			if (c.getNsfw()) {
				nsfwCategories.add(c);
			} else {
				sfwCategories.add(c);
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle state) {
		super.onActivityCreated(state);
		activity = (ChannelCreationActivity) getActivity();

		activity.setOnFilterCategoriesListener(new OnFilterCategoriesListener() {

			@Override
			public void onFilterCategories(NsfwSetting setting) {
				filterCategories(setting);
			}
		});
	}

	private void filterCategories(NsfwSetting setting) {
		// if the filter hasn't changed then we don't have to update the adapter
		if (setting == nsfwFilter || adapter == null) {
			return;
		}

		nsfwFilter = setting;

		List<Category> newList;

		switch (activity.getCategoryFilter()) {
		case ALLOWED:
			newList = allCategories;
			break;
		case NONE:
			newList = sfwCategories;
			break;
		case ONLY:
			newList = nsfwCategories;
			break;
		default:
			newList = sfwCategories;
		}

		// remove selected categories that aren't in the new list
		Iterator<Category> it = selectedCategories.iterator();
		for (; it.hasNext();) {
			if (!newList.contains(it.next())) {
				it.remove();
			}
		}

		adapter.setCategoryList(newList);
	}

	private void categoryClicked(View v, Category category) {
		RelativeLayout root = (RelativeLayout) v;
		SquareImage img = (SquareImage) root.findViewById(R.id.image);

		// highlight/unhighlight category image and add/remove to the list
		if (selectedCategories.contains(category)) {
			selectedCategories.remove(category);
			Util.setImageHighlight(activity, img, false);
		} else {
			selectedCategories.add(category);
			Util.setImageHighlight(activity, img, true);
		}
	}

	public void clearSelectedCategories() {
		selectedCategories.clear();
		adapter.notifyDataSetChanged();
	}

}
