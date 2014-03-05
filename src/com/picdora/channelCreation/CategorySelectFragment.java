package com.picdora.channelCreation;

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
import com.picdora.models.Category;

@EFragment(R.layout.fragment_selection_grid)
public class CategorySelectFragment extends Fragment {
	@ViewById
	GridView grid;
	@Bean
	CategoryListAdapter adapter;

	private OnCategoryClickListener listener;
	private List<Category> selectedCategories;

	@AfterViews
	void initViews() {
		// TODO: Load list in background and show loading icon

		// share the selected categories with the adapter so the adapter knows
		// which images to highlight
		selectedCategories = new ArrayList<Category>();
		adapter.setSelectedCategories(selectedCategories);
		
		grid.setAdapter(adapter);

		// tell the image loader to pause on fling scrolling
		boolean pauseOnScroll = false;
		boolean pauseOnFling = true;
		PauseOnScrollListener listener = new PauseOnScrollListener(
				ImageLoader.getInstance(), pauseOnScroll, pauseOnFling);
		grid.setOnScrollListener(listener);

		// setup click listener that selects/deselects image and reports click to listener
		grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				categoryClicked(view, adapter.getItem(pos));
			}
		});
	}

	private void categoryClicked(View v, Category category) {
		// don't alert the listener of the click until after we have updated the
		// list
		if (selectedCategories.contains(category)) {
			selectedCategories.remove(category);
			// highlight
		} else {
			selectedCategories.add(category);
		}

		if (listener != null) {
			listener.onCategoryClick(category);
		}
	}

	public void setOnCategoryClickListener(OnCategoryClickListener listener) {
		this.listener = listener;
	}

	public void clearSelectedCategories() {
		selectedCategories.clear();
		adapter.notifyDataSetChanged();
	}

	public List<Category> getSelectedCategories() {
		return selectedCategories;
	}

	public interface OnCategoryClickListener {
		public void onCategoryClick(Category category);
	}
}
