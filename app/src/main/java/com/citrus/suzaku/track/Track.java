package com.citrus.suzaku.track;

import android.database.Cursor;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDB.Tracks;

import java.io.Serializable;
import java.util.Locale;


public class Track implements Serializable
{
	private static final long serialVersionUID = 4000L;
	
	public long id;
	public String path;
	public String title;
	public String titleSort;
	public String artist;
	public long artistId;
	public String artistSort;
	public String album;
	public long albumId;
	public String albumSort;
	public long albumArtistId;
	public String albumArtist;
	public String albumArtistSort;
	public String composer;
	public String genre;
	public int trackNo;
	public int discNo;
	public long duration;			// 再生時間
	public int year;
	public boolean compilation;
	
	public long fileLastModified;	// 変更確認用
	
	public String artworkHash;
	
	
	public Track(Cursor cursor)
	{
		if(cursor == null)
			return;
		
		id = cursor.getLong(cursor.getColumnIndex(Tracks._ID));
		path = cursor.getString(cursor.getColumnIndex(Tracks.PATH));
		title = cursor.getString(cursor.getColumnIndex(Tracks.TITLE));
		titleSort = cursor.getString(cursor.getColumnIndex(Tracks.TITLE_SORT));
		artist = cursor.getString(cursor.getColumnIndex(Tracks.ARTIST));
		artistId = cursor.getLong(cursor.getColumnIndex(Tracks.ARTIST_ID));
		artistSort = cursor.getString(cursor.getColumnIndex(Tracks.ARTIST_SORT));
		album = cursor.getString(cursor.getColumnIndex(Tracks.ALBUM));
		albumId = cursor.getLong(cursor.getColumnIndex(Tracks.ALBUM_ID));
		albumSort = cursor.getString(cursor.getColumnIndex(Tracks.ALBUM_SORT));
		albumArtistId = cursor.getLong(cursor.getColumnIndex(Tracks.ALBUMARTIST_ID));
		albumArtist = cursor.getString(cursor.getColumnIndex(Tracks.ALBUMARTIST));
		albumArtistSort = cursor.getString(cursor.getColumnIndex(Tracks.ALBUMARTIST_SORT));
		composer = cursor.getString(cursor.getColumnIndex(Tracks.COMPOSER));
		genre = cursor.getString(cursor.getColumnIndex(Tracks.GENRE));
		trackNo = cursor.getInt(cursor.getColumnIndex(Tracks.TRACK_NO));
		discNo = cursor.getInt(cursor.getColumnIndex(Tracks.DISC_NO));
		duration = cursor.getLong(cursor.getColumnIndex(Tracks.DURATION));
		year = cursor.getInt(cursor.getColumnIndex(Tracks.YEAR));
		compilation = (cursor.getInt(cursor.getColumnIndex(Tracks.COMPILATION)) != 0);
		
		fileLastModified = cursor.getLong(cursor.getColumnIndex(Tracks.FILE_LAST_MODIFIED));
		
		artworkHash = cursor.getString(cursor.getColumnIndex(Tracks.ARTWORK_HASH));
	}

	// For Displaying String

	public String getArtistString(){
		return (!artist.equals(MusicDB._NULL))? artist : App.getContext().getString(R.string.unknown_artist);
	}

	public String getAlbumString(){
		return (!album.equals(MusicDB._NULL))? album : App.getContext().getString(R.string.unknown_album);
	}
	
	public String getDurationString()
	{
		long dmin = duration / 1000 / 60;
		long dsec = duration / 1000 % 60;
		
		return String.format(Locale.getDefault(), "%d:%02d", dmin, dsec);
	}

}
