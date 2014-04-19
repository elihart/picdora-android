package com.picdora.channelCreation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.widget.Button;

import com.picdora.CategoryUtils;
import com.picdora.R;
import com.picdora.Util;
import com.picdora.channelCreation.ChannelCreationActivity.NsfwSetting;
import com.picdora.models.Category;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;
import com.picdora.ui.grid.Selectable;
import com.picdora.ui.grid.SelectionFragment;

/**
 * This fragment allows the user to select categories to use in the channel.
 * They are filtered depending on the nsfw setting chosen in the info fragment.
 * The user can choose to either preview or create the channel. Both launch the
 * new channel, however, create will save it to the db and finish the Create
 * Activity, whereas preview will return here
 * 
 */
@EFragment(R.layout.fragment_category_selection)
public class CategorySelectFragment extends SelectionFragment {
	/*
	 * TODO: Minor issue- if the activity is destroyed the selection state isn't
	 * saved. It is only saved on config changes.
	 */

	@ViewById
	Button previewButton;
	@ViewById
	Button createButton;
	@ViewById
	Button clearButton;

	private List<Category> allCategories;
	private List<Category> nsfwCategories;
	private List<Category> sfwCategories;
	private ChannelCreationActivity mActivity;

	@AfterViews
	void initViews() {
		mActivity = (ChannelCreationActivity) getActivity();

		setSelectOnShortClick(true);
		setShowSelectionOptions(false);

		// set button fonts
		FontHelper.setTypeFace(previewButton, FontStyle.MEDIUM);
		FontHelper.setTypeFace(createButton, FontStyle.MEDIUM);
		FontHelper.setTypeFace(clearButton, FontStyle.MEDIUM);
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		/*
		 * When we are shown make sure we are filtering using the most up to
		 * date setting. If this is called before the categories are loaded in
		 * onResume then there's a null pointer since it tries to load a non
		 * existing list. To prevent that we don't filter if the categories
		 * aren't set yet and wait for it to be done in the normal load.
		 */
		if (isVisibleToUser && mActivity != null && allCategories != null) {
			filterCategories(mActivity.getNsfwSetting());
		}
	}

	/**
	 * Separate the categories into filtered lists depending on nsfw
	 */
	private void setupCategoryLists() {
		/* Get all categories from db and sort them alphabetically */
		allCategories = Util.all(Category.class);
		CategoryUtils.sortByName(allCategories);

		/* Sort the categories into nsfw and sfw lists. */
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

	/**
	 * Get the list of categories that corresponds to the given filter.
	 * 
	 * @param filter
	 * @return
	 */
	private List<Category> getFilteredList(NsfwSetting filter) {
		switch (filter) {
		case ALLOWED:
			return allCategories;
		case NONE:
			return sfwCategories;
		case ONLY:
			return nsfwCategories;
		default:
			return sfwCategories;
		}
	}

	/**
	 * Set whether the create/preview/clear buttons should be enabled. This
	 * should only be when there are categories selected.
	 * 
	 * @param enabled
	 */
	private void setOptionsEnabled(boolean enabled) {
		createButton.setEnabled(enabled);
		previewButton.setEnabled(enabled);
		clearButton.setEnabled(enabled);
	}

	@Click
	void clearButtonClicked() {
		clearSelection();
	}

	@Click
	void previewButtonClicked() {
		mActivity.setChannelCategories(getSelectedCategories(), true);
	}

	@Click
	void createButtonClicked() {
		mActivity.setChannelCategories(getSelectedCategories(), false);
	}

	@SuppressWarnings("unchecked")
	private List<Category> getSelectedCategories() {
		return (List<Category>) (List<?>) getSelection();
	}

	@Override
	protected void onSelectionChanged(List<Selectable> selection) {
		super.onSelectionChanged(selection);
		/* Enable the menu buttons if the selection isn't empty. */
		setOptionsEnabled(!selection.isEmpty());
	}

	@Override
	protected void onSelectionDeleted(List<Selectable> selection) {
		// N/A
	}

	@Override
	protected void onClick(Selectable item) {
		// N/A
	}

	@Override
	protected List<Selectable> doItemLoad() {
		setupCategoryLists();
		/*
		 * Get the current nsfw filter setting and use it to get the correctly
		 * filtered category list.
		 */
		return toSelectable(getFilteredList(mActivity.getNsfwSetting()));
	}

	@Override
	protected String getEmptyMessage() {
		/*
		 * This shouldn't happen unless the db isn't preloaded with categories.
		 */
		return "Uh Oh, we couldn't find any categories...";
	}

	/**
	 * Filter which categories are shown based on a nsfw setting.
	 * 
	 * @param setting
	 */
	public void filterCategories(NsfwSetting setting) {
		/* Decide which category list to use based on the setting. */
		List<Category> filteredList = getFilteredList(setting);

		/*
		 * Make a copy of the currently selected items and remove the ones not
		 * in the newly filtered list.
		 */
		List<Category> selectedCategories = new ArrayList<Category>(
				getSelectedCategories());

		Iterator<Category> it = selectedCategories.iterator();
		while (it.hasNext()) {
			if (!filteredList.contains(it.next())) {
				it.remove();
			}
		}

		/*
		 * Set the filtered list as the new items. This will clear any selected
		 * items so we have to reset the selected items too.
		 */
		setItemsToShow(toSelectable(filteredList));
		setSelectedItems(toSelectable(selectedCategories));
		/*
		 * The list of selected items may have been cleared so check if the menu
		 * should be visible.
		 */
		setOptionsEnabled(!getSelectedCategories().isEmpty());

	}
}
