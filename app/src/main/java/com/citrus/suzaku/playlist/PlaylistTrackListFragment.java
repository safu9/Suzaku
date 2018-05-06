package com.citrus.suzaku.playlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.base.BaseListFragment;
import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.track.TrackActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlaylistTrackListFragment extends BaseListFragment<PlaylistTrack>
{
	private static final String PLAYLIST = "PLAYLIST";
	
	private Playlist playlist;

	public static PlaylistTrackListFragment newInstance(int loaderId, Playlist playlist)
	{
		PlaylistTrackListFragment fragment = new PlaylistTrackListFragment();
		Bundle bundle = new Bundle();

		bundle.putInt(LOADER_ID, loaderId);
		bundle.putSerializable(PLAYLIST, playlist);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		playlist = (Playlist)getArguments().getSerializable(PLAYLIST);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		setListAdapter(new PlaylistTrackListAdapter());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_TRACKS, playlist, position, false);
		ContextCompat.startForegroundService(getActivity(), intent);

		boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
		if(ps){
			getActivity().startActivity(new Intent(getActivity(), TrackActivity.class));
		}
	}
	
	// ActionMode

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		menu.findItem(R.id.menu_delete).setVisible(true);
		return true;
	}
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.menu_delete:
				Intent intent = new Intent(MusicDBService.ACTION_DELETE_PLAYLISTTRACKS);
				intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlist);
				intent.putExtra(MusicDBService.INTENT_KEY_TRACK_IDS, (Serializable)getCheckedTrackIds());
				intent.setPackage(App.PACKAGE);
				getActivity().startService(intent);
				
				resetChoiceMode();
				return true;

			default:
				return super.onActionItemClicked(mode, item);
		}
	}
	

	@Override
	protected List<Long> getCheckedTrackIds()
	{
		List<PlaylistTrack> ptracks = getCheckedItems();
		List<Long> trackIds = new ArrayList<>();
		for(PlaylistTrack ptrack : ptracks){
			trackIds.add(ptrack.trackId);
		}

		return trackIds;
	}

	@Override
	protected List<PlaylistTrack> getDataList()
	{
		return playlist.getPlaylistTracks();
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.PlaylistChangedEvent event)
	{
		startLoader(true);
	}


	// Adapter
	private class PlaylistTrackListAdapter extends BaseListAdapter<PlaylistTrack> // implements PopupMenu.OnMenuItemClickListener
	{
		private LayoutInflater inflater;

	//	private int menuPosition;

		public PlaylistTrackListAdapter()
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

		/*	holder.popupButton = (ImageButton)view.findViewById(R.id.popupButton);
			holder.popupButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					PopupMenu menu = new PopupMenu(App.getContext(), v);
					menu.getMenuInflater().inflate(R.menu.menu_listitem_playlisttrack, menu.getMenu());
					menu.setOnMenuItemClickListener(PlaylistTrackListAdapter.this);
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
			Intent intent;
			List<Track> tracks;
			
			switch(item.getItemId()){
				case R.id.menu_delete:
					List<PlaylistTrack> ptracks = new ArrayList<PlaylistTrack>();
					ptracks.add(getItem(menuPosition));
					
					intent = new Intent(MusicDBService.ACTION_DELETE_PLAYLISTTRACKS);
					intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlist);
					intent.putExtra(MusicDBService.INTENT_KEY_TRACKS, (Serializable)ptracks);
					intent.setPackage(App.PACKAGE);
					getActivity().startService(intent);
					return true;
				
				case R.id.menu_add_to_playlist:
					tracks = new ArrayList<Track>();
					tracks.add(getItem(menuPosition));

					PlaylistSelectDialog dialog = PlaylistSelectDialog.newInstance(tracks);
					dialog.setTargetFragment(PlaylistTrackListFragment.this, 0);
					dialog.show(getChildFragmentManager(), "PlaylistSelectDialog");
					return true;

				case R.id.menu_add_to_queue:
					return true;

				case R.id.menu_detail:
					tracks = new ArrayList<Track>();
					tracks.add(getItem(menuPosition));
					
					intent = new Intent(getActivity(), TrackDetailActivity.class);
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
