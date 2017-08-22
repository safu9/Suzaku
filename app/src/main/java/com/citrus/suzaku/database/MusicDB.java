package com.citrus.suzaku.database;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.provider.*;

import com.citrus.suzaku.App;
import com.citrus.suzaku.playlist.Playlist;
import com.citrus.suzaku.playlist.PlaylistTrack;
import com.citrus.suzaku.R;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.album.Album;
import com.citrus.suzaku.artist.Artist;
import com.citrus.suzaku.artist.ArtistCompilation;
import com.citrus.suzaku.genre.Genre;

import java.util.*;


// SQLiteDatabase Util
public class MusicDB
{
	public static final String _NULL = "#NULL";
	public static final String COLLATE_LOCALIZED = " COLLATE LOCALIZED";
//	public static final String COLLATE_UNICODE = " COLLATE UNICODE";
	
	public static class Tracks implements BaseColumns
	{
		public static final String TABLE = "tracks";
		
		public static final String PATH = "path";
		public static final String TITLE = "title";
		public static final String TITLE_SORT = "titleSort";
		public static final String ARTIST = "artist";
		public static final String ARTIST_ID = "artistId";
		public static final String ARTIST_SORT = "artistSort";
		public static final String ALBUM = "album";
		public static final String ALBUM_ID = "albumId";
		public static final String ALBUM_SORT = "albumSort";
		public static final String ALBUMARTIST = "albumArtist";
		public static final String ALBUMARTIST_SORT = "albumArtistSort";
		public static final String ARTWORK_HASH = "artworkHash";
		public static final String COMPOSER = "composer";
		public static final String GENRE = "genre";
		public static final String TRACK_NO = "trackNo";
		public static final String DISC_NO = "discNo";
		public static final String DURATION = "duration";
		public static final String YEAR = "year";
		public static final String COMPILATION = "compilation";
		public static final String FILE_LAST_MODIFIED = "fileLastModified";
		public static final String FLAG = "flag";
		
	//	public static final int FLAG_HIDDEN = 1;
	//	public static final int FLAG_INCOMPATIBLE = 2;
	//	public static final int FLAG_SKIP_IN_SHUFFLEMODE = 4;
	}
	
	public static class Albums implements BaseColumns
	{
		public static final String TABLE = "albums";

		public static final String ALBUM = "album";
		public static final String ALBUM_SORT = "albumSort";
		public static final String ARTIST = "artist";
		public static final String ARTIST_ID = "artistId";
		public static final String ARTIST_SORT = "artistSort";
		public static final String ARTWORK_HASH = "artworkHash";
		public static final String YEAR = "year";
		public static final String COMPILATION = "compilation";
		public static final String NUMBER_OF_SONGS = "numSongs";
	}
	
	public static class Artists implements BaseColumns
	{
		public static final String TABLE = "artists";

		public static final String ARTIST = "artist";
		public static final String ARTIST_SORT = "artistSort";
		public static final String NUMBER_OF_SONGS = "numSongs";
		public static final String NUMBER_OF_ALBUMS = "numAlbums";
	}
	
	public static class Genres implements BaseColumns
	{
		public static final String TABLE = "genres";
		
		public static final String GENRE = "genre";
	}
	
	public static class Playlists implements BaseColumns
	{
		public static final String TABLE = "playlists";
		
		public static final String TITLE = "title";
	//	public static final String TITLE_SORT = "titleSort";
		public static final String NUMBER_OF_SONGS = "numSongs";
	}
	
	public static class PlaylistTracks extends Tracks implements BaseColumns
	{
		public static final String TABLE = "playlistTracks_";				// playlistTracks_#(No.)

		public static final String TRACK_ID = "trackId";
		public static final String PLAYLIST_TRACK_NO = "playlistTrackNo";
	}
	

	private SQLiteDatabase db;
	
	public MusicDB()
	{
		this(MusicDBHelper.getInstanceForReading().getReadableDatabase());
	}
	
	public MusicDB(SQLiteDatabase db)
	{
		this.db = db;
	}
	
	// App Methods
	
	// Track
	//

	public Track getTrack(long id)
	{
		String[] selectionArgs = { String.valueOf(id) };
		
		Cursor cursor = db.query(Tracks.TABLE, null, Tracks._ID + "= ?", selectionArgs, null, null, null);
								 
		if(!cursor.moveToFirst()){
			return null;
		}
			
		Track track = new Track(cursor);
		cursor.close();

		return track;
	}

	public List<Track> getTracks(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Tracks.TABLE, null, selection, selectionArgs, null, null, sortOrder);
		
		List<Track> tracks = new ArrayList<>();

		while(cursor.moveToNext()){
			tracks.add(new Track(cursor));
		}
		
		cursor.close();

		return tracks;
	}

	public List<Long> getTrackIds(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Tracks.TABLE, null, selection, selectionArgs, null, null, sortOrder);

		List<Long> trackIds = new ArrayList<>();

		while(cursor.moveToNext()){
			long id = cursor.getLong(cursor.getColumnIndex(Tracks._ID));
			trackIds.add(id);
		}

		cursor.close();

		return trackIds;
	}
	
	public List<Track> getAllTracks()
	{
		return getTracks(null, null, Tracks.TITLE_SORT + COLLATE_LOCALIZED);
	}
	
	public void insertTrack(Track track)
	{
		ContentValues values = track.getContentValues();
		db.insertWithOnConflict(Tracks.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public void updateTrack(long trackId, ContentValues values)
	{
		String[] whereArgs = { String.valueOf(trackId) };
		db.update(Tracks.TABLE, values, Tracks._ID + " = ?", whereArgs);
	}
	
	public void deleteTracks(String selection, String[] selectionArgs)
	{
		db.delete(Tracks.TABLE, selection, selectionArgs);
	}
	
	
	// Album
	//

	public Album getAlbum(long id)
	{
		String[] selectionArgs = { String.valueOf(id) };
		
		Cursor cursor = db.query(Albums.TABLE, null, Albums._ID + "= ?", selectionArgs, null, null, null);
		
		if(!cursor.moveToFirst()){
			return null;
		}
			
		Album album = new Album(cursor);
		cursor.close();

		return album;
	}

	public List<Album> getAlbums(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Albums.TABLE, null, selection, selectionArgs, null, null, Albums.ALBUM + " = '" + _NULL + "'," + sortOrder);
		
		List<Album> albums = new ArrayList<>();

		while(cursor.moveToNext()){
			albums.add(new Album(cursor));
		}
		
		cursor.close();

		return albums;
	}
	
	public List<Album> getAllAlbums()
	{
		return getAlbums(null, null, Albums.ALBUM_SORT + COLLATE_LOCALIZED);
	}
	
	public List<Album> getAlbumsByArtistFromTracks(long id)
	{
		String[] columns = { Tracks.ALBUM_ID };
		String[] selectionArgs = { String.valueOf(id) };
		
		Cursor cursor = db.query(Tracks.TABLE, columns, Tracks.ARTIST_ID + "= ?", selectionArgs,
								 Tracks.ALBUM_ID, null, Tracks.ALBUM + COLLATE_LOCALIZED);

		List<Album> albums = new ArrayList<>();

		while(cursor.moveToNext()){
			albums.add(getAlbum(cursor.getInt(cursor.getColumnIndex(Tracks.ALBUM_ID))));
		}

		cursor.close();

		return albums;
	}

	//! UNUSED
	public void insertAlbum(Album album)
	{
		ContentValues values = album.getContentValues();
		db.insertWithOnConflict(Albums.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	
	// Artist
	//

	public Artist getArtist(long id)
	{
		String[] selectionArgs = { String.valueOf(id) };

		Cursor cursor = db.query(Artists.TABLE, null, Artists._ID + "= ?", selectionArgs, null, null, null);

		if(!cursor.moveToFirst()){
			return null;
		}

		Artist artist = new Artist(cursor);
		cursor.close();

		return artist;
	}

	public List<Artist> getArtists(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Artists.TABLE, null, selection, selectionArgs, null, null, Artists.ARTIST + " = '" + _NULL + "'," + sortOrder);
						
		List<Artist> artists = new ArrayList<>();

		while(cursor.moveToNext()){
			artists.add(new Artist(cursor));
		}

		cursor.close();

		return artists;
	}

	public List<Artist> getAllArtists()
	{
		return getArtists(null, null, Artists.ARTIST_SORT + COLLATE_LOCALIZED);
	}
	
	public ArtistCompilation createArtistCompilation()
	{
		String[] selection = {
			"COUNT(*) AS " + Artists.NUMBER_OF_ALBUMS,
			"SUM(" + Albums.NUMBER_OF_SONGS + ") AS " + Artists.NUMBER_OF_SONGS
		};
		
		Cursor cursor = db.query(Albums.TABLE, selection, Albums.COMPILATION + "= 1", null, null, null, null);

		if(!cursor.moveToFirst()){
			return null;
		}

		ArtistCompilation compilation = new ArtistCompilation();
		compilation.artist = App.getContext().getString(R.string.compilations);
		compilation.numAlbums = cursor.getInt(cursor.getColumnIndex(Artists.NUMBER_OF_ALBUMS));
		compilation.numSongs = cursor.getInt(cursor.getColumnIndex(Artists.NUMBER_OF_SONGS));
		cursor.close();

		return compilation;
	}

	public void insertArtist(Artist artist)
	{
		ContentValues values = artist.getContentValues();
		db.insertWithOnConflict(Artists.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	
	// Genre
	//
	
	public Genre getGenre(long id)
	{
		String[] selectionArgs = { String.valueOf(id) };

		Cursor cursor = db.query(Genres.TABLE, null,
								  Genres._ID + "= ?", selectionArgs, null, null, null);

		if(!cursor.moveToFirst()){
			return null;
		}

		Genre item = new Genre(cursor);
		cursor.close();

		return item;
	}

	public List<Genre> getAllGenres()
	{
		Cursor cursor = db.query(Genres.TABLE, null, null, null, null, null, Genres.GENRE + " = '" + _NULL + "'," + Genres.GENRE + COLLATE_LOCALIZED);
						
		List<Genre> genres = new ArrayList<>();

		while(cursor.moveToNext()){
			genres.add(new Genre(cursor));
		}

		cursor.close();

		return genres;
	}

	//! UNUSED
	public void insertGenre(Genre genre)
	{
		ContentValues values = genre.getContentValues();
		db.insertWithOnConflict(Genres.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	
	// Playlist
	//
	
	public Playlist getPlaylist(long id)
	{
		String[] selectionArgs = { String.valueOf(id) };

		Cursor cursor = db.query(Playlists.TABLE, null,
								 Playlists._ID + "= ?", selectionArgs, null, null, null);

		if(!cursor.moveToFirst()){
			return null;
		}

		Playlist item = new Playlist(cursor);
		cursor.close();

		return item;
	}
	
	public List<Playlist> getPlaylists(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Playlists.TABLE, null, selection, selectionArgs, null, null, sortOrder);

		List<Playlist> playlists = new ArrayList<>();

		while(cursor.moveToNext()){
			playlists.add(new Playlist(cursor));
		}

		cursor.close();

		return playlists;
	}
	
	public List<Playlist> getAllPlaylists()
	{
		return getPlaylists(null, null, Playlists.TITLE + COLLATE_LOCALIZED);
	}
	
	public void insertPlaylist(Playlist playlist)
	{
		ContentValues values = playlist.getContentValues();
		values.remove(Playlists._ID);
		
		db.insert(Playlists.TABLE, null, values);
	}
	
	public void updatePlaylist(Playlist playlist)
	{
		ContentValues values = playlist.getContentValues();
		values.remove(Playlists._ID);
		
		String[] whereArgs = { String.valueOf(playlist.id) };
		db.update(Playlists.TABLE, values, Playlists._ID + " = ?", whereArgs);
	}

	public void deletePlaylist(long playlistId)
	{
		String[] selectionArgs = { String.valueOf(playlistId) };
		db.delete(Playlists.TABLE, Playlists._ID + " = ?", selectionArgs);

		db.execSQL("DROP TABLE IF EXISTS " +  PlaylistTracks.TABLE + String.valueOf(playlistId) + ";");
	}


	// PlaylistTrack
	//

	public List<PlaylistTrack> getPlaylistTracks(long playlistId)
	{
		Cursor cursor = db.query(PlaylistTracks.TABLE + String.valueOf(playlistId), null, null, null,null, null, PlaylistTracks.PLAYLIST_TRACK_NO);

		List<PlaylistTrack> tracks = new ArrayList<>();

		while(cursor.moveToNext()){
			tracks.add(new PlaylistTrack(cursor));
		}

		cursor.close();

		return tracks;
	}
	
	public void insertPlaylistTrack(long playlistId, PlaylistTrack track)
	{
		ContentValues values = track.getContentValues();
		values.remove(PlaylistTracks._ID);
		
		db.insert(PlaylistTracks.TABLE + String.valueOf(playlistId), null, values);
	}
	
}

