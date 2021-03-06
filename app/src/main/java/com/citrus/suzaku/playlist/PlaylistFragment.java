package com.citrus.suzaku.playlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.track.TrackActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

// Attached to MainActivity
public class PlaylistFragment extends Fragment
{
	private static final String FRAGMENT_TAG = "PlaylistTrackListFragment";
	private static final int LOADER_ID = 1001;

	private static final String EDIT_FRAGMENT_TAG = "PlaylistTrackListEditFragment";
	
	private Playlist playlistItem;
	
	private TextView titleTextView;
	private TextView songsTextView;
	private ImageView artworkImageView;

	private boolean isEditMode = false;
	
	
	public static PlaylistFragment newInstance(Playlist playlist)
	{
		PlaylistFragment fragment = new PlaylistFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable("PLAYLIST", playlist);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		((MainActivity)getActivity()).showDrawerIndicator(false);
		setHasOptionsMenu(true);

		// Data

		playlistItem = (Playlist)getArguments().getSerializable("PLAYLIST");

		// UI

		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.playlist);

		titleTextView = view.findViewById(R.id.title_view);
		songsTextView = view.findViewById(R.id.songs_view);
		artworkImageView = view.findViewById(R.id.artwork_view);
		
		updateView();

		// Fragment

		if(savedInstanceState == null){			// 非再生成時
			getChildFragmentManager()
			.beginTransaction()
			.replace(R.id.list, PlaylistTrackListFragment.newInstance(LOADER_ID, playlistItem), FRAGMENT_TAG)
			.commit();
		}

		getChildFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener(){
			@Override
			public void onBackStackChanged()
			{
				int count = getChildFragmentManager().getBackStackEntryCount();
				isEditMode = (count != 0);
				getActivity().invalidateOptionsMenu();
			}
		});
		
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
		inflater.inflate(R.menu.menu_fragment_playlist, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.menu_edit).setEnabled(!isEditMode);
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
				ContextCompat.startForegroundService(getActivity(), intent);

				boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
				if(ps){
					startActivity(new Intent(getActivity(), TrackActivity.class));
				}

				return true;

			case R.id.menu_edit:
				getChildFragmentManager()
					.beginTransaction()
					.add(R.id.list, PlaylistTrackListEditFragment.newInstance(playlistItem), EDIT_FRAGMENT_TAG)
					.addToBackStack(EDIT_FRAGMENT_TAG)
					.commit();
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
