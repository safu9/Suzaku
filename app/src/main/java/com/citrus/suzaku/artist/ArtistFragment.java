package com.citrus.suzaku.artist;

import android.content.*;
import android.os.*;
import android.support.design.widget.*;
import android.support.v4.app.*;
import android.support.v4.view.*;
import android.view.*;
import android.widget.*;

import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.base.BaseListFragment;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.R;
import com.citrus.suzaku.track.TrackActivity;
import com.citrus.suzaku.base.TrackGroupTrackListFragment;
import com.citrus.suzaku.album.Album;

import java.util.*;

// Attached to MainActivity
public class ArtistFragment extends Fragment
{
	private static final int LOADER_ID = 2002;
	
	private Artist artistItem;
	
	private List<Album> albums;
	private int mPosition = 0;
	
	private TextView titleTextView;
	private TextView subTextView1;
	private TextView subTextView2;
	private ImageView artworkImageView;

	private TabLayout mTabs;
	private ViewPager mViewPager;

	private ArtistPagerAdapter mPagerAdapter;
	

	public static ArtistFragment newInstance(Artist artist)
	{
		ArtistFragment fragment = new ArtistFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable("ARTIST", artist);
		fragment.setArguments(bundle);

		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_artist, container, false);

		((MainActivity)getActivity()).showDrawerIndicator(false);
		setHasOptionsMenu(true);
	
		// Data

		artistItem = (Artist)getArguments().getSerializable("ARTIST");
		
		albums = new ArrayList<>();
		Album all = new Album(null);
		albums.add(all);
		albums.addAll(artistItem.getAlbums());
	
		// UI
		
		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.artist);
		
		titleTextView = (TextView)view.findViewById(R.id.artist_view);
		subTextView1 = (TextView)view.findViewById(R.id.albums_view);
		subTextView2 = (TextView)view.findViewById(R.id.songs_view);
		artworkImageView = (ImageView)view.findViewById(R.id.artwork_view);

		updateView();
		
		// Tab

		mTabs = (TabLayout)view.findViewById(R.id.tabs);
		mViewPager = (ViewPager)view.findViewById(R.id.pager);
		
		mPagerAdapter = new ArtistPagerAdapter(getChildFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);

		mViewPager.setCurrentItem(mPosition);
		mViewPager.addOnPageChangeListener(new PageChangeListener());

		mTabs.setupWithViewPager(mViewPager);

		for(int i = 0; i < mPagerAdapter.getCount(); i++){
			Album item = albums.get(i);

			View tab = inflater.inflate(R.layout.tab_item_album, null, false);
			ImageView imageView = (ImageView)tab.findViewById(R.id.artwork_view);

			if(i == 0){
				imageView.setImageResource(R.drawable.all);
			}else if(!ArtworkCache.isCorrectHash(item.artworkHash)){
				imageView.setImageResource(R.drawable.blank);
			}else{
				ArtworkCache.Small.setArtworkView(imageView, item);
			}

			mTabs.getTabAt(i).setCustomView(tab);
		}
		
		return view;
	}
	
	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		
		artworkImageView.setImageDrawable(null);

		mViewPager.clearOnPageChangeListeners();
		mTabs.removeAllTabs();

		mPagerAdapter = null;
		mViewPager = null;
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
			case android.R.id.home:			// up button
				//! TENTATIVE
			//	finish();
				return true;
				
			case R.id.menu_shuffle:

				Intent intent;
				if(mPosition == 0){
					intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, artistItem, 0, true);
				}else{
					intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, albums.get(mPosition), 0, true);
				}
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
		if(mPosition == 0){					// all album
			titleTextView.setText(artistItem.getArtistString());
			subTextView1.setText(artistItem.getNumOfAlbumsString());
			subTextView2.setText(artistItem.getNumOfSongsString());
			
			Album firstAlbum = albums.get(1);
			if(ArtworkCache.isCorrectHash(firstAlbum.artworkHash)){
				ArtworkCache.Large.setArtworkView(artworkImageView, firstAlbum);
			}else{
				artworkImageView.setImageBitmap(null);
			}
		}else{
			Album album_item = albums.get(mPosition);
			
			titleTextView.setText(album_item.getAlbumString());
			subTextView1.setText(album_item.getArtistString());
			subTextView2.setText(album_item.getNumOfSongsString());
			
			if(ArtworkCache.isCorrectHash(album_item.artworkHash)){
				ArtworkCache.Large.setArtworkView(artworkImageView, album_item);
			}else{
				artworkImageView.setImageBitmap(null);
			}
		}
	}

	
	private class ArtistPagerAdapter extends FragmentStatePagerAdapter
	{
		private int size;

		public ArtistPagerAdapter(FragmentManager fm)
		{
			super(fm);
			
			size = albums.size();
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment fragment;
			
			if(position == 0){
				fragment = TrackGroupTrackListFragment.newInstance(LOADER_ID + position, artistItem);
			}else{
				fragment = TrackGroupTrackListFragment.newInstance(LOADER_ID + position, albums.get(position));
			}
			
			return fragment;
		}

		@Override
		public int getCount()
		{
			return size;
		}
		
		public Fragment getFragmentAtPosition(int position)
		{
			return (Fragment)instantiateItem(mViewPager, position);
		}
	}
	
	private class PageChangeListener implements ViewPager.OnPageChangeListener
	{
		@Override
		public void onPageSelected(int position)
		{
			((BaseListFragment)mPagerAdapter.getFragmentAtPosition(mPosition)).resetChoiceMode();
			mPosition = position;

			updateView();
		}

		@Override
		public void onPageScrollStateChanged(int state)
		{
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
		{
		}
	}

}
