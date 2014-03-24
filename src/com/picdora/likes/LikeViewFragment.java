package com.picdora.likes;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.support.v4.app.Fragment;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.picdora.R;

@EFragment(R.layout.fragment_like_view)
public class LikeViewFragment extends Fragment {
	@ViewById
	protected ProgressBar progress;
	@ViewById
	protected GridView grid;
	@Bean
	protected ImageGridAdapter mAdapter;
	
	
	@AfterViews
	void initViews() {
		
	}

}
