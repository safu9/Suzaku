package com.citrus.suzaku;

import android.os.*;
import android.support.v4.app.*;
import android.view.*;

import java.util.*;

// Attached to MainActivity
public class ArtistCompilationFragment extends Fragment
{
	private static final String FRAGMENT_TAG = "CompilationAlbumListFragment";
	private static final int LOADER_ID = 2001;

	private ArtistCompilation compilationItem;


	public static ArtistCompilationFragment newInstance(ArtistCompilation compilation)
	{
		ArtistCompilationFragment fragment = new ArtistCompilationFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable("COMPILATION", compilation);
		fragment.setArguments(bundle);

		return fragment;
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_base, container, false);

		((MainActivity)getActivity()).showDrawerIndicator(false);
		((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.compilations);

		// Data

		compilationItem = (ArtistCompilation)getArguments().getSerializable("COMPILATION");

		// Fragment
		
		if(savedInstanceState == null){			// 非再生成時
			getActivity().getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.list, CompilationAlbumListFragment.newInstance(LOADER_ID, compilationItem), FRAGMENT_TAG)
			.commit();
		}
	
		return view;
	}
	
	
	public static class CompilationAlbumListFragment extends AlbumListFragment
	{
		private ArtistCompilation compilationItem;

		public static CompilationAlbumListFragment newInstance(int loaderId, ArtistCompilation compilation)
		{
			CompilationAlbumListFragment fragment = new CompilationAlbumListFragment();
			Bundle bundle = new Bundle();

			bundle.putInt(LOADER_ID, loaderId);
			bundle.putSerializable("COMPILATION", compilation);
			fragment.setArguments(bundle);

			return fragment;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);
			compilationItem = (ArtistCompilation)getArguments().getSerializable("COMPILATION");
		}

		@Override
		protected List<Album> getDataList()
		{
			return compilationItem.getAlbums();
		}
	}
	
}
