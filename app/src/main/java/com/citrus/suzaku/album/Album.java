package com.citrus.suzaku.album;

import android.database.Cursor;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.TrackGroup;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDB.Albums;
import com.citrus.suzaku.database.MusicDB.Tracks;
import com.citrus.suzaku.track.Track;

import java.io.Serializable;
import java.util.List;

public class Album extends TrackGroup implements Serializable
{
	private static final long serialVersionUID = 3000L;
	
	public long id;
	public String album;
	public String albumSort;
	public String artworkHash;
	public long artistId;
	public String artist;
	public String artistSort;
	public int numSongs;
	public int year;
	public boolean compilation;
	
	
	public Album(Cursor cursor)
	{
		if(cursor == null)
			return;
		
		id = cursor.getLong(cursor.getColumnIndex(Albums._ID));
		album = cursor.getString(cursor.getColumnIndex(Albums.ALBUM));
		albumSort = cursor.getString(cursor.getColumnIndex(Albums.ALBUM_SORT));
		artworkHash = cursor.getString(cursor.getColumnIndex(Albums.ARTWORK_HASH));
		artistId = cursor.getLong(cursor.getColumnIndex(Albums.ARTIST_ID));
		artist = cursor.getString(cursor.getColumnIndex(Albums.ARTIST));
		artistSort = cursor.getString(cursor.getColumnIndex(Albums.ARTIST_SORT));
		numSongs = cursor.getInt(cursor.getColumnIndex(Albums.NUMBER_OF_SONGS));
		year = cursor.getInt(cursor.getColumnIndex(Albums.YEAR));
		compilation = (cursor.getInt(cursor.getColumnIndex(Albums.COMPILATION)) != 0);
	}
	
	@Override
	public List<Track> getTracks()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { String.valueOf(id) };
		
		return mdb.getTracks(Tracks.ALBUM_ID + "= ?", selectionArgs, Tracks.DISC_NO + "," + Tracks.TRACK_NO);
	}

	@Override
	public List<Long> getTrackIds()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { String.valueOf(id) };

		return mdb.getTrackIds(Tracks.ALBUM_ID + "= ?", selectionArgs, Tracks.DISC_NO + "," + Tracks.TRACK_NO);
	}
	
	// For Displaying String 
	
	public String getAlbumString()
	{
		return (!album.equals(MusicDB._NULL))? album : App.getContext().getString(R.string.unknown_album);
	}
	
	public String getArtistString()
	{
		if(compilation){
			return App.getContext().getString(R.string.various_artists);
		}else if(artist.equals(MusicDB._NULL)){
			return App.getContext().getString(R.string.unknown_artist);
		}else {
			return artist;
		}
	}
	
	public String getNumOfSongsString()
	{
		return App.getContext().getResources().getQuantityString(R.plurals.num_songs, numSongs, numSongs);
	}
}
