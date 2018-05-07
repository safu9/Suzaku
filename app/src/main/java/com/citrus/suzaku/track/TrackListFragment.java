package com.citrus.suzaku.track;

import android.content.*;
import android.os.*;
import android.support.v4.content.ContextCompat;
import android.view.*;
import android.widget.*;

import com.citrus.suzaku.App;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.base.BaseListFragment;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;

import java.util.*;


public class TrackListFragment extends BaseListFragment<Track>
{

	public static TrackListFragment newInstance(int loaderId)
	{
		TrackListFragment fragment = new TrackListFragment();
		Bundle bundle = new Bundle();

		bundle.putInt(LOADER_ID, loaderId);
		fragment.setArguments(bundle);

		return fragment;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setListAdapter(new TrackListAdapter());
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_ALL, null, position, false);
		ContextCompat.startForegroundService(getActivity(), intent);

		boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
		if(ps){
			getActivity().startActivity(new Intent(getActivity(), TrackActivity.class));
		}
	}

	@Override
	protected List<Long> getCheckedTrackIds()
	{
		List<Track> tracks = getCheckedItems();
		List<Long> ids = new ArrayList<>();
		for(Track track : tracks){
			ids.add(track.id);
		}
		return ids;
	}

	@Override
	protected List<Track> getDataList()
	{
		return (new MusicDB()).getAllTracks();
	}
	
	
	// Adapter
	private class TrackListAdapter extends BaseListAdapter<Track> // implements PopupMenu.OnMenuItemClickListener
	{
		private LayoutInflater inflater;

	//	private int menuPosition;

		public TrackListAdapter()
		{
			Context context = App.getContext();
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();
			
			Track item = getItem(position);

			holder.trackTextView.setText(item.title);
			holder.artistTextView.setText(item.getArtistString());
			holder.durationTextView.setText(item.getDurationString());
			
		//	holder.popupButton.setTag(position);
			
			view.setActivated(getListView().isItemChecked(position));
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_track, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.trackTextView = (TextView)view.findViewById(R.id.title_view);
			holder.artistTextView = (TextView)view.findViewById(R.id.artist_view);
			holder.durationTextView = (TextView)view.findViewById(R.id.duration_view);

		/*
			holder.popupButton = (ImageButton)view.findViewById(R.id.popupButton);
			holder.popupButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					PopupMenu menu = new PopupMenu(getActivity(), v);
					menu.getMenuInflater().inflate(R.menu.menu_listitem_track, menu.getMenu());
					menu.setOnMenuItemClickListener(TrackListAdapter.this);
					menu.show();
					
					menuPosition = v.getTag();
				}
			});
		*/
			view.setTag(holder);

			return view;
		}

/*		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			List<Track> tracks;
			
			switch(item.getItemId()){
				case R.id.menu_add_to_playlist:
					tracks = new ArrayList<Track>();
					tracks.add(getItem(menuPosition));
					
					PlaylistSelectDialog dialog = PlaylistSelectDialog.newInstance(tracks);
					dialog.setTargetFragment(TrackListFragment.this, 0);
					dialog.show(getFragmentManager(), "PlaylistSelectDialog");
					return true;

				case R.id.menu_add_to_queue:
					return true;
				
				case R.id.menu_detail:
					tracks = new ArrayList<Track>();
					tracks.add(getItem(menuPosition));
					
					Intent intent = new Intent(getActivity(), TrackDetailActivity.class);
					intent.putExtra("TRACKS", (Serializable)tracks);
					startActivity(intent);
					
					return true;

				default:
					return false;
			}
		}
*/
	}
	
	private static class ViewHolder
	{
		TextView trackTextView;
		TextView artistTextView;
		TextView durationTextView;
		
	//	ImageButton popupButton;
	}
	
}
