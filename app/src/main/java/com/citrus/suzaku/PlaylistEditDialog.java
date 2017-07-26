package com.citrus.suzaku;

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

import java.util.List;


// Edit Title of Playlist
public class PlaylistEditDialog extends DialogFragment implements View.OnClickListener
{
	private static final String PLAYLIST = "PLAYLIST";
	
	private EditText editText;

	public static PlaylistEditDialog newInstance(Playlist playlist)
	{
		PlaylistEditDialog dialog = new PlaylistEditDialog();

		Bundle bundle = new Bundle();
		bundle.putSerializable(PLAYLIST, playlist);
		dialog.setArguments(bundle);

		return dialog;
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Playlist playlist = (Playlist)getArguments().getSerializable(PLAYLIST);
		
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View content = inflater.inflate(R.layout.dialog_create_playlist, null, false);

		editText = (EditText)content.findViewById(R.id.edit);
		Button okButton = (Button)content.findViewById(R.id.okButton);
		Button cancelButton = (Button)content.findViewById(R.id.cancelButton);
		
		editText.setText(playlist.title);
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.edit);
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
			case R.id.okButton:
				
				Playlist playlist = (Playlist)getArguments().getSerializable(PLAYLIST);
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

				Intent intent = new Intent(MusicDBService.ACTION_EDIT_PLAYLIST);
				intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlist);
				intent.setPackage(App.PACKAGE);
				getActivity().startService(intent);
			
				dismiss();

				break;
				
			case R.id.cancelButton:
				dismiss();
				break;
		}
	}
	
}
