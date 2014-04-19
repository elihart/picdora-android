package com.picdora.ui.grid;

import org.androidannotations.annotations.EBean;

@EBean
public class SelectionAdapter extends ModelGridAdapter<Selectable> {

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	protected String getImgurId(Selectable item) {
		return item.getIconId();
	}

	@Override
	protected String getText(Selectable item) {
		return item.getName();
	}
}
