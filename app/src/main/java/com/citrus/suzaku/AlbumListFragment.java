package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;


// Attached to MainActivity
public class AlbumListFragment extends TrackGroupListFragment<Album>
{
	
	public static AlbumListFragment newInstance(int loaderId)
	{
		AlbumListFragment fragment = new AlbumListFragment();
		Bundle bundle = new Bundle();
		
		bundle.putInt(LOADER_ID, loaderId);
		fragment.setArguments(bundle);
		
		return fragment;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new AlbumListAdapter());
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		AlbumFragment fragment = AlbumFragment.newInstance((Album)l.getItemAtPosition(position));
		((MainActivity)getActivity()).replaceFragment(fragment);
	}

	@Override
	protected List<Album> getDataList()
	{
		return (new MusicDB()).getAllAlbums();
	}
	
	
	// Adapter
	private class AlbumListAdapter extends BaseListAdapter<Album>
	{
		private LayoutInflater inflater;

		public AlbumListAdapter()
		{
			Context context = App.getContext();
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();

			Album item = getItem(position);

			holder.albumTextView.setText(item.getAlbumString());
			holder.artistTextView.setText(item.getArtistString());
			holder.numSongsTextView.setText(item.getNumOfSongsString());

			holder.artworkImageView.setImageResource(R.drawable.blank);
			holder.artworkImageView.setTag(null);
			if(ArtworkCache.isCorrectHash(item.artworkHash)){
				ArtworkCache.Small.setArtworkView(holder.artworkImageView, item);
			}
			
			view.setActivated(getListView().isItemChecked(position));
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_album, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.albumTextView = (TextView)view.findViewById(R.id.title);
			holder.artistTextView = (TextView)view.findViewById(R.id.artist);
			holder.numSongsTextView = (TextView)view.findViewById(R.id.songs);
			holder.artworkImageView = (ImageView)view.findViewById(R.id.artwork);

			view.setTag(holder);

			return view;
		}
	}
	
	private static class ViewHolder
	{
		TextView albumTextView;
		TextView artistTextView;
		TextView numSongsTextView;
		ImageView artworkImageView;
	}

}
