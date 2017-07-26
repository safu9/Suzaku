package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.util.*;


public class PlaylistListFragment extends TrackGroupListFragment<Playlist>
{
	private MyPlaylistBroadcastReceiver receiver;
	private IntentFilter filter;
	

	public static PlaylistListFragment newInstance(int loaderId)
	{
		PlaylistListFragment fragment = new PlaylistListFragment();
		Bundle bundle = new Bundle();

		bundle.putInt(LOADER_ID, loaderId);
		fragment.setArguments(bundle);

		return fragment;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		receiver = new MyPlaylistBroadcastReceiver();
		filter = receiver.createIntentFilter();
		
		setListAdapter(new PlaylistListAdapter());
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getActivity().registerReceiver(receiver, filter);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		getActivity().unregisterReceiver(receiver);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		PlaylistFragment fragment = PlaylistFragment.newInstance((Playlist)l.getItemAtPosition(position));
		((MainActivity)getActivity()).replaceFragment(fragment);
	}
	
	@Override
	protected List<Playlist> getDataList()
	{
		return (new MusicDB()).getAllPlaylists();
	}
	
	// Adapter
	private class PlaylistListAdapter extends BaseListAdapter<Playlist> implements PopupMenu.OnMenuItemClickListener
	{
		private LayoutInflater inflater;

		private int menuPosition;

		public PlaylistListAdapter()
		{
			Context context = App.getContext();
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();

			Playlist item = getItem(position);

			holder.titleTextView.setText(item.title);
			holder.songsTextView.setText(item.getNumOfSongsString());
			
			holder.popupButton.setTag(position);
			
			view.setActivated(getListView().isItemChecked(position));
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_playlist, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.titleTextView = (TextView)view.findViewById(R.id.title);
			holder.songsTextView = (TextView)view.findViewById(R.id.songs);


			holder.popupButton = (ImageButton)view.findViewById(R.id.popupButton);
			holder.popupButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					PopupMenu menu = new PopupMenu(getActivity(), v);
					menu.getMenuInflater().inflate(R.menu.menu_listitem_playlist, menu.getMenu());
					menu.setOnMenuItemClickListener(PlaylistListAdapter.this);
					menu.show();

					menuPosition = (int)v.getTag();
					
					resetChoiceMode();
				}
			});
			
			view.setTag(holder);

			return view;
		}
		
		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			switch(item.getItemId()){
				case R.id.menu_add_to_playlist:
					List<Long> trackIds = getItem(menuPosition).getTrackIds();
					PlaylistSelectDialog selectDialog = PlaylistSelectDialog.newInstance(trackIds);
					selectDialog.setTargetFragment(PlaylistListFragment.this, 0);
					selectDialog.show(getChildFragmentManager(), "PlaylistSelectDialog");
					return true;

				case R.id.menu_add_to_queue:
					return true;
					
				case R.id.menu_edit:
					PlaylistEditDialog editDialog = PlaylistEditDialog.newInstance(getItem(menuPosition));
					editDialog.setTargetFragment(PlaylistListFragment.this, 0);
					editDialog.show(getChildFragmentManager(), "PlaylistSelectDialog");
					return true;

				case R.id.menu_delete:
					Intent intent = new Intent(MusicDBService.ACTION_DELETE_PLAYLIST);
					intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST_ID, getItem(menuPosition).id);
					intent.setPackage(App.PACKAGE);
					getActivity().startService(intent);
					return true;

				default:
					return false;
			}
		}
	}
	
	private static class ViewHolder
	{
		TextView titleTextView;
		TextView songsTextView;
		
		ImageButton popupButton;
	}

	
	private class MyPlaylistBroadcastReceiver extends PlaylistBroadcastReceiver
	{
		@Override
		public void onReceive(Context c, Intent i)
		{
			startLoader(true);
		}
	}
	
}
