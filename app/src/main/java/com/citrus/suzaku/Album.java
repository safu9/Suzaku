package com.citrus.suzaku;

import android.content.ContentValues;
import android.database.Cursor;

import com.citrus.suzaku.MusicDB.Albums;
import com.citrus.suzaku.MusicDB.Tracks;

import java.io.Serializable;
import java.util.List;

public class Album extends TrackGroup implements Serializable
{
	private static final long serialVersionUID = 3L;
	
	public long id;
	public String album;
	public String albumSort;
	public String artworkHash;
	public String artist;
//	public long artistId;
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
		artist = cursor.getString(cursor.getColumnIndex(Albums.ARTIST));
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

	public ContentValues getContentValues()
	{
		ContentValues values = new ContentValues(8);

		values.put(Albums._ID, id);
		values.put(Albums.ALBUM, album);
		values.put(Albums.ALBUM_SORT, albumSort);
		values.put(Albums.ARTWORK_HASH, artworkHash);
		values.put(Albums.ARTIST, artist);
		values.put(Albums.YEAR, year);
		values.put(Albums.NUMBER_OF_SONGS, numSongs);
		values.put(Albums.COMPILATION, compilation);
		
		return values;
	}
	
	// For Displaying String 
	
	public String getAlbumString()
	{
		return (!album.equals(MusicDB._NULL))? album : App.getContext().getString(R.string.unknown_album);
	}
	
	public String getArtistString()
	{
		if(artist.equals(MusicDB._NULL)){
			return App.getContext().getString(R.string.unknown_artist);
		}else if(compilation){
			return App.getContext().getString(R.string.various_artists);
		}else{
			return artist;
		}
	}
	
	public String getNumOfSongsString()
	{
		return App.getContext().getResources().getQuantityString(R.plurals.num_songs, numSongs, numSongs);
	}
}
