package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.*;

// Attached to MainActivity
public class PlaylistFragment extends Fragment
{
	private static final String FRAGMENT_TAG = "PlaylistTrackListFragment";
	private static final int LOADER_ID = 1001;
	
	private Playlist playlistItem;
	
	private TextView titleTextView;
	private TextView songsTextView;
	private ImageView artworkImageView;
	
	
	public static PlaylistFragment newInstance(Playlist playlist)
	{
		PlaylistFragment fragment = new PlaylistFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable("PLAYLIST", playlist);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		((MainActivity)getActivity()).showDrawerIndicator(false);
		setHasOptionsMenu(true);

		// Data

		playlistItem = (Playlist)getArguments().getSerializable("PLAYLIST");

		// UI

		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.playlist);

		titleTextView = (TextView)view.findViewById(R.id.title);
		songsTextView = (TextView)view.findViewById(R.id.songs);
		artworkImageView = (ImageView)view.findViewById(R.id.artwork);
		
		updateView();

		// Fragment

		if(savedInstanceState == null){			// 非再生成時
			getChildFragmentManager()
			.beginTransaction()
			.replace(R.id.list, PlaylistTrackListFragment.newInstance(LOADER_ID, playlistItem), FRAGMENT_TAG)
			.commit();
		}
		
		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu,inflater);

		inflater.inflate(R.menu.menu_fragment, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case android.R.id.home:				// up button
				//! TENTATIVE
			//	finish();
				return true;

			case R.id.menu_shuffle:

				Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, playlistItem, 0, true);
				getActivity().startService(intent);

				boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
				if(ps){
					startActivity(new Intent(getActivity(), TrackActivity.class));
				}

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void updateView()
	{
		titleTextView.setText(playlistItem.title);
		songsTextView.setText(playlistItem.getNumOfSongsString());
		
		List<Track> tracks = playlistItem.getTracks();
		if(tracks != null && tracks.size() >= 1){
			Track firstTrack = tracks.get(0);
			if(ArtworkCache.isCorrectHash(firstTrack.artworkHash)){
				ArtworkCache.Large.setArtworkView(artworkImageView, firstTrack);
				return;
			}
		}
		
		artworkImageView.setImageBitmap(null);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.PlaylistChangedEvent event)
	{
		playlistItem = (new MusicDB()).getPlaylist(playlistItem.id);
		updateView();
	}

}
