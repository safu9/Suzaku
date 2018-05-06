package com.citrus.suzaku.artist;

import android.database.Cursor;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.album.Album;
import com.citrus.suzaku.base.TrackGroup;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDB.Albums;
import com.citrus.suzaku.database.MusicDB.Artists;
import com.citrus.suzaku.database.MusicDB.Tracks;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.track.Track;

import java.util.List;


public class Artist extends TrackGroup
{
	private static final long serialVersionUID = 2000L;
	
	public long id;
	public String artist;
	public String artistSort;
	public int numAlbums;
	public int numSongs;

	
	public Artist(Cursor cursor)
	{
		if(cursor == null)
			return;
		
		id = cursor.getLong(cursor.getColumnIndex(Artists._ID));
		artist = cursor.getString(cursor.getColumnIndex(Artists.ARTIST));
		artistSort = cursor.getString(cursor.getColumnIndex(Artists.ARTIST_SORT));
		numAlbums = cursor.getInt(cursor.getColumnIndex(Artists.NUMBER_OF_ALBUMS));
		numSongs = cursor.getInt(cursor.getColumnIndex(Artists.NUMBER_OF_SONGS));
	}

	@Override
	public List<Track> getTracks()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { String.valueOf(id) };
		
		boolean gc = PreferenceUtils.getBoolean(PreferenceUtils.GROUP_COMPILATION);
		if(!gc){
			return mdb.getTracks(Tracks.ARTIST_ID + "= ?", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
		}else{
			return mdb.getTracks(Tracks.ALBUMARTIST_ID + "= ?", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
		}
	}

	@Override
	public List<Long> getTrackIds()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { String.valueOf(id) };

		boolean gc = PreferenceUtils.getBoolean(PreferenceUtils.GROUP_COMPILATION);
		if(!gc){
			return mdb.getTrackIds(Tracks.ARTIST_ID + "= ?", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
		}else{
			return mdb.getTrackIds(Tracks.ARTIST_ID + "= ? AND " + Tracks.COMPILATION + "= 0", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
		}
	}

	public List<Album> getAlbums()
	{
		MusicDB mdb = new MusicDB();
		
		boolean gc = PreferenceUtils.getBoolean(PreferenceUtils.GROUP_COMPILATION);
		if(!gc){
			return mdb.getAlbumsByArtistFromTracks(id);
		}else{
			String[] selectionArgs = { String.valueOf(id) };
			return mdb.getAlbums(Albums.ARTIST_ID + "= ? AND " + Albums.COMPILATION + "= 0", selectionArgs, Albums.ALBUM_SORT + MusicDB.COLLATE_LOCALIZED);
		}
	}
	
	// For Displaying String 

	public String getArtistString(){
		return (!artist.equals(MusicDB._NULL))? artist : App.getContext().getString(R.string.unknown_artist);
	}
	
	public String getNumOfAlbumsString(){
		return App.getContext().getResources().getQuantityString(R.plurals.num_albums, numAlbums, numAlbums);
	}

	public String getNumOfSongsString(){
		return App.getContext().getResources().getQuantityString(R.plurals.num_songs, numSongs, numSongs);
	}
	
}
