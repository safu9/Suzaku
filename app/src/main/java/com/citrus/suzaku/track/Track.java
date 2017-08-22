package com.citrus.suzaku.track;

import android.content.ContentValues;
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

	public ContentValues getContentValues()
	{
		ContentValues values = new ContentValues(21);

		values.put(Tracks._ID, id);
		values.put(Tracks.PATH, path);
		values.put(Tracks.TITLE, title);
		values.put(Tracks.TITLE_SORT, titleSort);
		values.put(Tracks.ARTIST, artist);
		values.put(Tracks.ARTIST_ID, artistId);
		values.put(Tracks.ARTIST_SORT, artistSort);
		values.put(Tracks.ALBUM, album);
		values.put(Tracks.ALBUM_ID, albumId);
		values.put(Tracks.ALBUM_SORT, albumSort);
		values.put(Tracks.ALBUMARTIST, albumArtist);
		values.put(Tracks.ALBUMARTIST_SORT, albumArtistSort);
		values.put(Tracks.COMPOSER, composer);
		values.put(Tracks.GENRE, genre);
		values.put(Tracks.TRACK_NO, trackNo);
		values.put(Tracks.DISC_NO, discNo);
		values.put(Tracks.DURATION, duration);
		values.put(Tracks.YEAR, year);
		values.put(Tracks.COMPILATION, compilation);

		values.put(Tracks.FILE_LAST_MODIFIED, fileLastModified);

		values.put(Tracks.ARTWORK_HASH, artworkHash);

		return values;
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