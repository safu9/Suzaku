package com.citrus.suzaku.album;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.artwork.GlideApp;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.base.TrackGroupListFragment;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.main.MainActivity;

import java.util.List;


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

/*
			holder.artworkImageView.setImageResource(R.drawable.img_blank);
			holder.artworkImageView.setTag(null);
			if(ArtworkCache.isCorrectHash(item.artworkHash)){
				ArtworkCache.Small.setArtworkView(holder.artworkImageView, item);
			}
*/

			GlideApp.with(AlbumListFragment.this)
					.load(item.getTracks().get(0))
					.placeholder(R.drawable.img_blank)
					.into(holder.artworkImageView);
			
			view.setActivated(getListView().isItemChecked(position));
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_album, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.albumTextView = (TextView)view.findViewById(R.id.title_view);
			holder.artistTextView = (TextView)view.findViewById(R.id.artist_view);
			holder.numSongsTextView = (TextView)view.findViewById(R.id.songs_view);
			holder.artworkImageView = (ImageView)view.findViewById(R.id.artwork_view);

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
