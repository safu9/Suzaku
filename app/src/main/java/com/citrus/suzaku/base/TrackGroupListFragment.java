package com.citrus.suzaku.base;

import java.util.*;

public abstract class TrackGroupListFragment<T extends TrackGroup> extends BaseListFragment<T>
{

	@Override
	protected List<Long> getCheckedTrackIds()
	{
		List<T> items = getCheckedItems();
		List<Long> ids = new ArrayList<>();

		for(T item : items){
			ids.addAll(item.getTrackIds());
		}
		
		return ids;
	}
	
}
