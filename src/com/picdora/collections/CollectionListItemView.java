package com.picdora.collections;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.picdora.R;
import com.picdora.ui.FontHelper;
import com.picdora.ui.FontHelper.FontStyle;

@EViewGroup(R.layout.collection_list_item)
public class CollectionListItemView extends RelativeLayout {

	    @ViewById
	    TextView name;

	    public CollectionListItemView(Context context) {
	        super(context);
	    }
	    
	    @AfterViews
	    void init(){
	    	FontHelper.setTypeFace(name, FontStyle.REGULAR);
	    }

	    public void bind(Collection collection) {
	        name.setText(collection.getName());
	    }
}
