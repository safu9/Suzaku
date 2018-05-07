package com.citrus.suzaku.base;

import android.content.*;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.*;
import android.widget.*;

import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.track.TrackActivity;
import com.citrus.suzaku.track.TrackListFragment;

import java.util.*;


public class TrackGroupTrackListFragment<T extends TrackGroup> extends TrackListFragment
{
	private static final String TRACK_GROUP = "TRACK_GROUP";

	private TrackGroup trackGroup;


	public static TrackGroupTrackListFragment newInstance(int loaderId, TrackGroup trackGroup)
	{
		TrackGroupTrackListFragment fragment = new TrackGroupTrackListFragment();
		Bundle bundle = new Bundle();

		bundle.putInt(LOADER_ID, loaderId);
		bundle.putSerializable(TRACK_GROUP, trackGroup);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		trackGroup = (TrackGroup)getArguments().getSerializable(TRACK_GROUP);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, trackGroup, position, false);
		ContextCompat.startForegroundService(getActivity(), intent);

		boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
		if(ps){
			getActivity().startActivity(new Intent(getActivity(), TrackActivity.class));
		}
	}
	
	@Override
	protected List<Track> getDataList()
	{
		return trackGroup.getTracks();
	}
}
