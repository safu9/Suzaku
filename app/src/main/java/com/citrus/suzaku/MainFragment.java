package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.support.design.widget.TabLayout;
import android.support.v4.app.*;
import android.support.v4.view.*;
import android.view.*;
import android.widget.*;

// Attached to MainActivity
public class MainFragment extends Fragment
{
	private TabLayout mTabs;
	private ViewPager mViewPager;
	
	private MyPagerAdapter mPagerAdapter;
	
	private int mPosition = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		
		setHasOptionsMenu(true);
		((MainActivity)getActivity()).showDrawerIndicator(true);
		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.app_name);
		
		mPosition = PreferenceUtils.getInt(PreferenceUtils.TAB_POSITION);
		if(getArguments() != null){
			mPosition = getArguments().getInt("POSITION", mPosition);
			setArguments(null);
		}
		
		// Tab

		mTabs = (TabLayout)view.findViewById(R.id.tabs);
		mViewPager = (ViewPager)view.findViewById(R.id.pager);
		
		mPagerAdapter = new MyPagerAdapter(getChildFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);

		mViewPager.setCurrentItem(mPosition);
		mViewPager.addOnPageChangeListener(new PageChangeListener());

		mTabs.setupWithViewPager(mViewPager);

		for(int i = 0; i < mPagerAdapter.getCount(); i++){
			View tab = inflater.inflate(R.layout.tab_item_main, null, false);
			TextView titleView = (TextView)tab.findViewById(R.id.title);
			titleView.setText(mPagerAdapter.getPageTitle(i));

			mTabs.getTabAt(i).setCustomView(tab);
		}
		return view;
	}

	@Override
	public void onPause()
	{
		super.onPause();
		PreferenceUtils.putInt(PreferenceUtils.TAB_POSITION, mPosition);
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mViewPager.clearOnPageChangeListeners();
		mTabs.removeAllTabs();

		mViewPager = null;
		mPagerAdapter = null;
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
			case R.id.menu_shuffle:
				Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_ALL, null, 0, true);
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
	
	// Called by MainActivity Drawer
	public void setCurrentPage(int position)
	{
		if(mViewPager != null){
			mViewPager.setCurrentItem(position);
		}else{
			Bundle bundle = new Bundle();
			bundle.putInt("POSITION", position);
			setArguments(bundle);
		}
	}

	
	private class MyPagerAdapter extends FragmentStatePagerAdapter
	{
		private static final int NUM_PAGES = 5;

		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment fragment;

			switch(position){
				case 0:
					fragment = PlaylistListFragment.newInstance(1000);
					break;
				case 1:
					fragment = ArtistListFragment.newInstance(2000);
					break;
				case 2:
					fragment = AlbumListFragment.newInstance(3000);
					break;
				case 3:
					fragment = TrackListFragment.newInstance(4000);
					break;
				case 4:
					fragment = GenreListFragment.newInstance(5000);
					break;
				default:
					fragment = null;
			}

			return fragment;
		}

		@Override
		public int getCount()
		{
			return NUM_PAGES;
		}

		@Override
		public CharSequence getPageTitle(int position){
			switch(position){
				case 0:
					return getString(R.string.playlists);
				case 1:
					return getString(R.string.artists);
				case 2:
					return getString(R.string.albums);
				case 3:
					return getString(R.string.songs);
				case 4:
					return getString(R.string.genres);
			}
			return null;
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
