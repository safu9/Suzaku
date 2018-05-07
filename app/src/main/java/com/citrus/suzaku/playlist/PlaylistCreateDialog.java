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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDBService;

import java.util.List;


// Get Title and Create New Playlist
public class PlaylistCreateDialog extends DialogFragment implements View.OnClickListener
{
/*	public static interface Callback
	{
		public void onPlaylistCreateDialogResult();
	}

	private Callback mCallback;
*/
	private EditText editText;

/*	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		Fragment fragment = getTargetFragment();
		try{
			mCallback = (Callback)((fragment != null)? fragment : activity);
		}catch(ClassCastException e){
			throw(e);
		}
	}
*/
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View content = inflater.inflate(R.layout.dialog_create_playlist, null, false);

		editText = content.findViewById(R.id.title_edit);
		Button okButton = content.findViewById(R.id.ok_button);
		Button cancelButton = content.findViewById(R.id.cancel_button);
		
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.create_new_playlist);
		builder.setView(content);
	//	builder.setPositiveButton(android.R.string.ok, this);
	//	builder.setNegativeButton(android.R.string.cancel, this);
		
		return builder.create();
	}
/*
	@Override
	public void onDetach()
	{
		super.onDetach();
		mCallback = null;
	}
*/
	@Override
	public void onClick(View v)
	{
		switch(v.getId()){
			case R.id.ok_button:
				
				Playlist playlist = new Playlist(null);
				playlist.title = editText.getText().toString();

				if(playlist.title.isEmpty()){
					Toast.makeText(App.getContext(), R.string.warn_input_playlist_title, Toast.LENGTH_SHORT).show();
					return;
				}

				String[] selectionArgs = { playlist.title };
				List<Playlist> playlists = (new MusicDB()).getPlaylists(MusicDB.Playlists.TITLE + " = ?", selectionArgs, null);
				if(playlists.size() != 0){
					Toast.makeText(App.getContext(), R.string.warn_title_already_exists, Toast.LENGTH_SHORT).show();
					return;
				}

				Intent intent = new Intent(MusicDBService.ACTION_CREATE_PLAYLIST);
				intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlist);
				intent.setPackage(App.PACKAGE);
				getActivity().startService(intent);
			
				dismiss();

				break;
				
			case R.id.cancel_button:
				dismiss();
				break;
		}
	}

}
