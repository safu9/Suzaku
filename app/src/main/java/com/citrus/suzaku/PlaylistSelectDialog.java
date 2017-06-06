package com.citrus.suzaku;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

import android.support.v4.app.DialogFragment;


// Select Playlist and Add Tracks
public class PlaylistSelectDialog extends DialogFragment implements ListView.OnItemClickListener
{
	private static final String TRACKS = "TRACKS";
	
	private List<Playlist> playlists;
	private PlaylistListAdapter adapter;
	
	private MyPlaylistBroadcastReceiver receiver;
	private IntentFilter filter;
	
	
	public static PlaylistSelectDialog newInstance(List<Track> tracks)
	{
		PlaylistSelectDialog dialog = new PlaylistSelectDialog();
		
		Bundle bundle = new Bundle();
		bundle.putSerializable(TRACKS, (Serializable)tracks);
		dialog.setArguments(bundle);
		
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		receiver = new MyPlaylistBroadcastReceiver();
		filter = receiver.createIntentFilter();
		
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View content = inflater.inflate(R.layout.dialog_select_playlist, null, false);
		
		updatePlaylistList();
		adapter = new PlaylistListAdapter();
		adapter.setDataList(playlists);
		
		View header = inflater.inflate(R.layout.listitem_text, null, false);
		TextView textView = (TextView)header.findViewById(R.id.title);
		textView.setText(R.string.create_new_playlist);
		
		ListView listView = (ListView)content.findViewById(R.id.list);
		listView.addHeaderView(header);
		listView.setOnItemClickListener(this);
		listView.setAdapter(adapter);
	
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_playlist);
		builder.setView(content);
		
		return builder.create();
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
	public void onItemClick(AdapterView<?> l, View v, int position, long id)
	{
		if(position == 0){					// Header
			PlaylistCreateDialog dialog = new PlaylistCreateDialog();
			dialog.setTargetFragment(this, 0);
			dialog.show(getChildFragmentManager(), "PlaylistCreateDialog");

			return;
		}
		
		List<Track> tracks = (List<Track>)getArguments().getSerializable(TRACKS);

		Intent intent = new Intent(MusicDBService.ACTION_ADD_TO_PLAYLIST);
		intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlists.get(position - 1));
		intent.putExtra(MusicDBService.INTENT_KEY_TRACKS, (Serializable)tracks);
		intent.setPackage(App.PACKAGE);
		getActivity().startService(intent);
		
		dismiss();
	}
	
	private void updatePlaylistList()
	{
		playlists = (new MusicDB()).getAllPlaylists();
	}
	
	// Adapter
	private static class PlaylistListAdapter extends BaseListAdapter<Playlist>
	{
		private LayoutInflater inflater;


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
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.dialog_listitem_playlist, null, false);

			ViewHolder holder = new ViewHolder();

			holder.titleTextView = (TextView)view.findViewById(R.id.title);
			holder.songsTextView = (TextView)view.findViewById(R.id.songs);

			view.setTag(holder);

			return view;
		}
		
		private static class ViewHolder
		{
			TextView titleTextView;
			TextView songsTextView;
		}
	}


	private class MyPlaylistBroadcastReceiver extends PlaylistBroadcastReceiver
	{
		@Override
		public void onReceive(Context c, Intent i)
		{
			updatePlaylistList();
			adapter.setDataList(playlists);
			adapter.notifyDataSetChanged();
		}
	}
	
}
