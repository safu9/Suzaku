package com.citrus.suzaku.album;

import android.content.*;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.app.*;
import android.support.v4.content.ContextCompat;
import android.view.*;
import android.widget.*;

import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.R;
import com.citrus.suzaku.track.TrackActivity;
import com.citrus.suzaku.base.TrackGroupTrackListFragment;

// Attached to MainActivity
public class AlbumFragment extends Fragment
{
	private static final String FRAGMENT_TAG = "AlbumTrackListFragment";
	private static final int LOADER_ID = 3001;
	
	private Album albumItem;
	
	private ImageView artworkImageView;
	
	
	public static AlbumFragment newInstance(Album album)
	{
		AlbumFragment fragment = new AlbumFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable("ALBUM", album);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_album, container, false);
		
		((MainActivity)getActivity()).showDrawerIndicator(false);
		setHasOptionsMenu(true);
		
		// Data
		
		albumItem = (Album)getArguments().getSerializable("ALBUM");
		
		// UI

		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.album);

		TextView albumTextView = view.findViewById(R.id.album_view);
		TextView artistTextView = view.findViewById(R.id.artist_view);
		TextView songsTextView = view.findViewById(R.id.songs_view);
		artworkImageView = view.findViewById(R.id.artwork_view);

		albumTextView.setText(albumItem.getAlbumString());
		artistTextView.setText(albumItem.getArtistString());
		songsTextView.setText(albumItem.getNumOfSongsString());

		if(ArtworkCache.isCorrectHash(albumItem.artworkHash)){
			ArtworkCache.Large.setArtworkView(artworkImageView, albumItem);
		}

		// Fragment

		if(savedInstanceState == null){			// 非再生成時
			getChildFragmentManager()
			.beginTransaction()
			.replace(R.id.list, TrackGroupTrackListFragment.newInstance(LOADER_ID, albumItem), FRAGMENT_TAG)
			.commit();
		}

		return view;
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		
		artworkImageView.setImageDrawable(null);
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

				Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, albumItem, 0, true);
				ContextCompat.startForegroundService(getActivity(), intent);

				boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
				if(ps){
					startActivity(new Intent(getActivity(), TrackActivity.class));
				}

				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}

/*
	public static class AlbumTrackListFragment extends TrackListFragment
	{
		private static final String ALBUM = "ALBUM";

		private Album album;


		public static AlbumTrackListFragment newInstance(int loaderId, Album album)
		{
			AlbumTrackListFragment fragment = new AlbumTrackListFragment();
			Bundle bundle = new Bundle();

			bundle.putInt(LOADER_ID, loaderId);
			bundle.putSerializable(ALBUM, album);
			fragment.setArguments(bundle);

			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			album = (Album)getArguments().getSerializable(ALBUM);
			return super.onCreateView(inflater, container, savedInstanceState);
		}
		
		@Override
		protected List<Track> getDataList()
		{
			return album.getTracks();
		}
	}
*/
}
