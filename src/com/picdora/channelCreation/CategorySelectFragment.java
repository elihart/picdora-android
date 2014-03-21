package com.picdora.channelCreation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.FrameLayout;

import com.picdora.CategoryUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.channelCreation.ChannelCreationActivity.OnFilterCategoriesListener;
import com.picdora.models.Category;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.grid.GridItemView;
import com.picdora.ui.grid.ImageGridSelector;
import com.picdora.ui.grid.ImageGridSelector.OnGridItemClickListener;

/**
 * This fragment allows the user to select categories to use in the channel.
 * They are filtered depending on the nsfw setting chosen in the info fragment.
 * The user can choose to either preview or create the channel. Both launch the
 * new channel, however, create will save it to the db and finish the Create
 * Activity, whereas preview will return here
 * 
 */
@EFragment(R.layout.fragment_category_selection)
public class CategorySelectFragment extends Fragment {
	@Bean
	CategoryListAdapter adapter;
	@ViewById
	Button previewButton;
	@ViewById
	Button createButton;
	@ViewById
	Button clearButton;
	@ViewById
	protected FrameLayout gridContainer;

	private List<Category> selectedCategories;
	private List<Category> allCategories;
	private List<Category> nsfwCategories;
	private List<Category> sfwCategories;
	private ChannelCreationActivity activity;
	private NsfwSetting nsfwFilter;
	protected ImageGridSelector<Category> mCategorySelector;

	@AfterViews
	void initViews() {
		// TODO: Load list in background and show loading icon

		// check if there is state to restore
		List<Category> selectedState = ChannelCreationActivity
				.getSelectedCategoriesState();
		if (selectedState != null) {
			selectedCategories = new ArrayList<Category>(selectedState);
		} else {
			selectedCategories = new ArrayList<Category>();
		}

		// create the filtered lists
		// TODO: Async
		setupCategoryLists();

		mCategorySelector = new ImageGridSelector<Category>(getActivity(),
				allCategories, selectedCategories, adapter);

		filterCategories(ChannelCreationActivity.getNsfwFilter());

		gridContainer.addView(mCategorySelector.getView());

		// Update the button state
		mCategorySelector
				.setOnClickListener(new OnGridItemClickListener<Category>() {

					@Override
					public void OnGridItemClick(GridItemView view, Category item) {
						setCreateButtonEnabled(!selectedCategories.isEmpty());
					}
				});

		// set fonts
		FontHelper.setTypeFace(previewButton, FontStyle.MEDIUM);
		FontHelper.setTypeFace(createButton, FontStyle.MEDIUM);
		FontHelper.setTypeFace(clearButton, FontStyle.MEDIUM);
	}

	/**
	 * Separate the categories into filtered lists depending on nsfw
	 */
	private void setupCategoryLists() {
		allCategories = Util.all(Category.class);
		CategoryUtils.sortByName(allCategories);
		nsfwCategories = new ArrayList<Category>();
		sfwCategories = new ArrayList<Category>();

		for (Category c : allCategories) {
			if (c.isNsfw()) {
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

	/**
	 * Update the filter setting for the categories
	 * 
	 * @param setting
	 */
	private void filterCategories(NsfwSetting setting) {
		if (adapter == null) {
			return;
		}

		nsfwFilter = setting;

		List<Category> newList;

		switch (nsfwFilter) {
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

		mCategorySelector.setItems(newList);

		setCreateButtonEnabled(!selectedCategories.isEmpty());
	}

	private void setCreateButtonEnabled(boolean enabled) {
		createButton.setEnabled(enabled);
		previewButton.setEnabled(enabled);
		clearButton.setEnabled(enabled);
	}

	@Click
	void clearButtonClicked() {
		selectedCategories.clear();
		adapter.notifyDataSetChanged();
		setCreateButtonEnabled(false);
	}

	@Click
	void previewButtonClicked() {
		activity.submitChannelCategories(selectedCategories, true);
	}

	@Click
	void createButtonClicked() {
		activity.submitChannelCategories(selectedCategories, false);
	}
}
