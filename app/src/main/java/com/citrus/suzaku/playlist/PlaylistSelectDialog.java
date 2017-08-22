package com.citrus.suzaku.playlist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDBService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.Serializable;
import java.util.List;


// Select Playlist and Add Tracks
public class PlaylistSelectDialog extends DialogFragment implements ListView.OnItemClickListener
{
	private static final String TRACK_IDS = "TRACK_IDS";
	
	private List<Playlist> playlists;
	private PlaylistListAdapter adapter;
	
	
	public static PlaylistSelectDialog newInstance(List<Long> trackIds)
	{
		PlaylistSelectDialog dialog = new PlaylistSelectDialog();
		
		Bundle bundle = new Bundle();
		bundle.putSerializable(TRACK_IDS, (Serializable)trackIds);
		dialog.setArguments(bundle);
		
		return dialog;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View content = inflater.inflate(R.layout.dialog_select_playlist, null, false);
		
		updatePlaylistList();
		adapter = new PlaylistListAdapter();
		adapter.setDataList(playlists);
		
		View header = inflater.inflate(R.layout.listitem_text, null, false);
		TextView textView = (TextView)header.findViewById(R.id.title_view);
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
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
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
		
		List<Long> tracks = (List<Long>)getArguments().getSerializable(TRACK_IDS);

		Intent intent = new Intent(MusicDBService.ACTION_ADD_TO_PLAYLIST);
		intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlists.get(position - 1));
		intent.putExtra(MusicDBService.INTENT_KEY_TRACK_IDS, (Serializable)tracks);
		intent.setPackage(App.PACKAGE);
		getActivity().startService(intent);
		
		dismiss();
	}
	
	private void updatePlaylistList()
	{
		playlists = (new MusicDB()).getAllPlaylists();
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.PlaylistChangedEvent event)
	{
		updatePlaylistList();
		adapter.setDataList(playlists);
		adapter.notifyDataSetChanged();
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
			View view = inflater.inflate(R.layout.dialog_listitem_playlist, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.titleTextView = (TextView)view.findViewById(R.id.title_view);
			holder.songsTextView = (TextView)view.findViewById(R.id.songs_view);

			view.setTag(holder);

			return view;
		}
		
		private static class ViewHolder
		{
			TextView titleTextView;
			TextView songsTextView;
		}
	}
	
}
