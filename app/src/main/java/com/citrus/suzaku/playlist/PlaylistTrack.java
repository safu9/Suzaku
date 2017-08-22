package com.citrus.suzaku.playlist;

import android.content.ContentValues;
import android.database.Cursor;

import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.database.MusicDB.PlaylistTracks;

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
	public ContentValues getContentValues()
	{
		ContentValues values = new ContentValues(20);

		values.put(PlaylistTracks._ID, id);
		values.put(PlaylistTracks.PATH, path);
		values.put(PlaylistTracks.TITLE, title);
		values.put(PlaylistTracks.TITLE_SORT, titleSort);
		values.put(PlaylistTracks.ALBUM, album);
		values.put(PlaylistTracks.ALBUM_ID, albumId);
		values.put(PlaylistTracks.ALBUMARTIST, albumArtist);
		values.put(PlaylistTracks.ARTWORK_HASH, artworkHash);
		values.put(PlaylistTracks.ARTIST, artist);
		values.put(PlaylistTracks.ARTIST_ID, artistId);
		values.put(PlaylistTracks.COMPOSER, composer);
		values.put(PlaylistTracks.GENRE, genre);
		values.put(PlaylistTracks.TRACK_NO, trackNo);
		values.put(PlaylistTracks.DISC_NO, discNo);
		values.put(PlaylistTracks.DURATION, duration);
		values.put(PlaylistTracks.YEAR, year);
		values.put(PlaylistTracks.COMPILATION, compilation);
		values.put(PlaylistTracks.FILE_LAST_MODIFIED, fileLastModified);

		values.put(PlaylistTracks.TRACK_ID, trackId);
		values.put(PlaylistTracks.PLAYLIST_TRACK_NO, playlistTrackNo);
		
		return values;
	}
	
	@Override
	protected PlaylistTrack clone() throws CloneNotSupportedException
	{
		return (PlaylistTrack)super.clone();
	}
	
}
