package com.citrus.suzaku.playlist;

import android.database.Cursor;

import com.citrus.suzaku.database.MusicDB.PlaylistTracks;
import com.citrus.suzaku.track.Track;

import java.io.Serializable;


public class PlaylistTrack extends Track implements Serializable, Cloneable
{
	private static final long serialVersionUID = 6L;

	public long trackId;
	public int playlistTrackNo;
	
	
	public PlaylistTrack(Cursor cursor)
	{
		super(cursor);
		
		if(cursor == null)
			return;
		
		trackId = cursor.getLong(cursor.getColumnIndex(PlaylistTracks.TRACK_ID));
		playlistTrackNo = cursor.getInt(cursor.getColumnIndex(PlaylistTracks.PLAYLIST_TRACK_NO));
	}
	
	public void setTrackInfo(Track track)
	{
		trackId = track.id;
		path = track.path;
		title = track.title;
		titleSort = track.titleSort;
		album = track.album;
		albumId = track.albumId;
		albumArtist = track.albumArtist;
		artworkHash = track.artworkHash;
		artist = track.artist;
		artistId = track.artistId;
		composer = track.composer;
		genre = track.genre;
		trackNo = track.trackNo;
		discNo = track.discNo;
		duration = track.duration;
		year = track.year;
		compilation = track.compilation;

		fileLastModified = track.fileLastModified;
	}
	
	public Track getTrack()
	{
		Track track = null;
		try{
			track = clone();
			track.id = trackId;
		}catch(CloneNotSupportedException e){
			e.printStackTrace();
		}
		return track;
	}
	
	@Override
	protected PlaylistTrack clone() throws CloneNotSupportedException
	{
		return (PlaylistTrack)super.clone();
	}
	
}
