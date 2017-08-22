package com.citrus.suzaku.artist;

import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;

import com.citrus.suzaku.App;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.TrackGroupListFragment;

import java.util.*;


public class ArtistListFragment extends TrackGroupListFragment<Artist>
{
	private ArtistCompilation compilation;

	
	public static ArtistListFragment newInstance(int loaderId)
	{
		ArtistListFragment fragment = new ArtistListFragment();
		Bundle bundle = new Bundle();

		bundle.putInt(LOADER_ID, loaderId);
		fragment.setArguments(bundle);

		return fragment;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new ArtistListAdapter());
	}

	@Override
	public void onDestroyView()
	{
		setListAdapter(null);
		super.onDestroyView();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Fragment fragment;
		if(compilation != null && position == 0){
			fragment = ArtistCompilationFragment.newInstance(compilation);
		}else{
			fragment = ArtistFragment.newInstance((Artist)l.getItemAtPosition(position));
		}
		
		((MainActivity)getActivity()).replaceFragment(fragment);
	}
	
	@Override
	protected List<Artist> getDataList()
	{
		MusicDB mdb = new MusicDB();
		List<Artist> artists = mdb.getAllArtists();

		updateArtistCompilation();
		if(compilation != null && compilation.numSongs != 0){
			artists.add(0, compilation);
		}

		return artists;
	}

	private void updateArtistCompilation()
	{
		boolean gc = PreferenceUtils.getBoolean(PreferenceUtils.GROUP_COMPILATION);
		
		if(!gc){
			compilation = null;
			return;
		}

		MusicDB mdb = new MusicDB();
		compilation = mdb.createArtistCompilation();

		if(compilation == null || compilation.numSongs == 0){
			compilation = null;
		}
	}
	
	// Adapter
	private class ArtistListAdapter extends BaseListAdapter<Artist>
	{
		private LayoutInflater inflater;


		public ArtistListAdapter()
		{
			Context context = App.getContext();
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();

			Artist item = getItem(position);

			holder.artistTextView.setText(item.getArtistString());
			holder.numAlbumsTextView.setText(item.getNumOfAlbumsString());
			holder.numSongsTextView.setText(item.getNumOfSongsString());
			
			view.setActivated(getListView().isItemChecked(position));
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_artist, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.artistTextView = (TextView)view.findViewById(R.id.title_view);
			holder.numAlbumsTextView = (TextView)view.findViewById(R.id.albums_view);
			holder.numSongsTextView = (TextView)view.findViewById(R.id.songs_view);

			view.setTag(holder);

			return view;
		}
	}
	
	private static class ViewHolder
	{
		TextView artistTextView;
		TextView numSongsTextView;
		TextView numAlbumsTextView;
	}
	
}
