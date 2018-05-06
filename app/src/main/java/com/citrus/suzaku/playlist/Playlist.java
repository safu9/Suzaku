package com.citrus.suzaku.playlist;

import android.database.Cursor;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.TrackGroup;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDB.Playlists;
import com.citrus.suzaku.track.Track;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Playlist extends TrackGroup implements Serializable
{
	private static final long serialVersionUID = 1000L;
	
	public long id;
	public String title;
	public int numSongs;
	
//	private List<Track> tracks;
	
	
	public Playlist(Cursor cursor)
	{
		if(cursor == null)
			return;
			
		id = cursor.getLong(cursor.getColumnIndex(Playlists._ID));
		title = cursor.getString(cursor.getColumnIndex(Playlists.TITLE));
		numSongs = cursor.getInt(cursor.getColumnIndex(Playlists.NUMBER_OF_SONGS));
	}

	@Override
	public List<Track> getTracks()
	{
		List<PlaylistTrack> ptracks = getPlaylistTracks();
		List<Track> tracks = new ArrayList<>();
		for(PlaylistTrack ptrack : ptracks){
			tracks.add(ptrack.getTrack());
		}
		
		return tracks;
	}

	@Override
	public List<Long> getTrackIds()
	{
		List<PlaylistTrack> ptracks = getPlaylistTracks();
		List<Long> trackIds = new ArrayList<>();
		for(PlaylistTrack ptrack : ptracks){
			trackIds.add(ptrack.trackId);
		}

		return trackIds;
	}

	public List<PlaylistTrack> getPlaylistTracks()
	{
		MusicDB mdb = new MusicDB();
		return mdb.getPlaylistTracks(id);
	}
	
	// For Displaying String
	
	public String getNumOfSongsString(){
		return App.getContext().getResources().getQuantityString(R.plurals.num_songs, numSongs, numSongs);
	}
}
