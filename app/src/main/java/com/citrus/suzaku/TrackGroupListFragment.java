package com.citrus.suzaku;

import java.util.*;

public abstract class TrackGroupListFragment<T extends TrackGroup> extends BaseListFragment<T>
{

	@Override
	protected List<Track> getCheckedTracks()
	{
		List<T> items = getCheckedItems();
		List<Track> tracks = new ArrayList<>();

		for(T trackGroup : items){
			tracks.addAll(trackGroup.getTracks());
		}
		
		return tracks;
	}
	
}
