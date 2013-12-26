package com.picdora;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

import android.text.TextUtils;

import com.picdora.models.Category;
import com.picdora.models.Channel;
import com.picdora.models.Image;


public class ChannelPlayer {
	private Channel mChannel;
	private List<Image> mImages;
	
	public ChannelPlayer(Channel channel){
		mChannel = channel;
		mImages = new ArrayList<Image>();
	}
	
	public Image getImage(int index) {
		if (index >= mImages.size()) {
			loadImageBatchFromDb(index - mImages.size() + 1 + 10, mImages);
		}

		return mImages.get(index);

	}

	/**
	 * Get the specified number of images from the database and load them into
	 * the array
	 * 
	 * @param count
	 */
	private void loadImageBatchFromDb(int count, List<Image> images) {
		String query = "SELECT * FROM Images WHERE categoryId IN " + getCategoryIds() + " ORDER BY viewCount ASC, redditScore DESC LIMIT "
				+ Integer.toString(count);
//		String query = "SELECT * FROM Images WHERE nsfw=0 AND gif=1 ORDER BY redditScore DESC";

		CursorList<Image> list = Query.many(Image.class, query, null).get();
		images.addAll(list.asList());
		list.close();
	}
	
	// get a comma separated list of categories ids for use in a sql query
	private String getCategoryIds(){
		List<Category> categories = mChannel.getCategories();

		List<Integer> ids = new ArrayList<Integer>();
		for(Category cat : categories){
			ids.add(cat.getId());
		}
		
		return ("(" + TextUtils.join(",", ids) + ")");
	}

}
