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
		public static final String VIEW = "tracksView";

		public static final String PATH = "path";
		public static final String TITLE = "title";
		public static final String TITLE_SORT = "titleSort";
		public static final String ARTIST_ID = "artistId";
		public static final String ARTIST = "artist";						// view
		public static final String ARTIST_SORT = "artistSort";				// view
		public static final String ALBUM_ID = "albumId";
		public static final String ALBUM = "album";							// view
		public static final String ALBUM_SORT = "albumSort";				// view
		public static final String ALBUMARTIST_ID = "albumArtistId";		// view
		public static final String ALBUMARTIST = "albumArtist";				// view
		public static final String ALBUMARTIST_SORT = "albumArtistSort";	// view
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
		public static final String VIEW = "albumsView";

		public static final String ALBUM = "album";
		public static final String ALBUM_SORT = "albumSort";
		public static final String ARTIST_ID = "artistId";
		public static final String ARTIST = "artist";				// view
		public static final String ARTIST_SORT = "artistSort";		// view
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
		public static final String TABLE = "playlistTracks";
		public static final String VIEW = "playlistTracksView";

		public static final String TRACK_ID = "trackId";
		public static final String PLAYLIST_ID = "playlistId";
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
		
		Cursor cursor = db.query(Tracks.VIEW, null, Tracks._ID + "= ?", selectionArgs, null, null, null);
								 
		if(!cursor.moveToFirst()){
			return null;
		}
			
		Track track = new Track(cursor);
		cursor.close();

		return track;
	}

	public List<Track> getTracks(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Tracks.VIEW, null, selection, selectionArgs, null, null, sortOrder);
		
		List<Track> tracks = new ArrayList<>();

		while(cursor.moveToNext()){
			tracks.add(new Track(cursor));
		}
		
		cursor.close();

		return tracks;
	}

	public List<Track> getAllTracks()
	{
		return getTracks(null, null, Tracks.TITLE_SORT + COLLATE_LOCALIZED);
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
	
	public long insertTrack(Track track)
	{
		if(track.artistId <= 0){
			List<Artist> artists = getArtists(Artists.ARTIST + "=?", new String[]{ track.artist }, null);

			if(artists.size() > 0){
				track.artistId = artists.get(0).id;
			}else{
				track.artistId = insertArtist(extractArtistFromTrack(track));
			}
		}
		if(track.albumId <= 0){
			List<Album> albums = getAlbums(Albums.ALBUM + "=?", new String[]{ track.album }, null);

			if(albums.size() > 0){
				Album album = albums.get(0);

				if(!album.compilation && !album.artist.equals(track.albumArtist)){		// アルバムをコンピレーションに指定
					ContentValues values = new ContentValues(2);
					values.put(Albums.ARTIST_ID, 0);
					values.put(Albums.COMPILATION, 1);
					updateAlbum(album.id, values);
				}
				track.albumId = album.id;
			}else{
				track.albumId = insertAlbum(extractAlbumFromTrack(track));
			}
		}

		ContentValues values = getTrackContentValues(track, false);
		return db.insertWithOnConflict(Tracks.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
		
		Cursor cursor = db.query(Albums.VIEW, null, Albums._ID + "= ?", selectionArgs, null, null, null);
		
		if(!cursor.moveToFirst()){
			return null;
		}
			
		Album album = new Album(cursor);
		cursor.close();

		return album;
	}

	public List<Album> getAlbums(String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor cursor = db.query(Albums.VIEW, null, selection, selectionArgs, null, null, Albums.ALBUM + " = '" + _NULL + "'," + sortOrder);
		
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
		
		Cursor cursor = db.query(Tracks.VIEW, columns, Tracks.ARTIST_ID + "= ?", selectionArgs,
								 Tracks.ALBUM_ID, null, Tracks.ALBUM + COLLATE_LOCALIZED);

		List<Album> albums = new ArrayList<>();

		while(cursor.moveToNext()){
			albums.add(getAlbum(cursor.getInt(cursor.getColumnIndex(Tracks.ALBUM_ID))));
		}

		cursor.close();

		return albums;
	}

	public long insertAlbum(Album album)
	{
		if(album.artistId <= 0){
			List<Artist> artists = getArtists(Artists.ARTIST + "=?", new String[]{ album.artist }, null);

			if(artists.size() > 0){
				album.artistId = artists.get(0).id;
			}else{
				album.artistId = insertArtist(extractArtistFromAlbum(album));
			}
		}

		ContentValues values = getAlbumContentValues(album, false);
		return db.insertWithOnConflict(Albums.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void updateAlbum(long albumId, ContentValues values)
	{
		String[] whereArgs = { String.valueOf(albumId) };
		db.update(Albums.TABLE, values, Albums._ID + " = ?", whereArgs);
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
		return getArtists(Artists.NUMBER_OF_SONGS + "!=0", null, Artists.ARTIST_SORT + COLLATE_LOCALIZED);
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

	public long insertArtist(Artist artist)
	{
		ContentValues values = getArtistContentValues(artist, false);
		return db.insertWithOnConflict(Artists.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
		ContentValues values = getGenreContentValues(genre, false);
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
		ContentValues values = getPlaylistContentValues(playlist, false);
		db.insert(Playlists.TABLE, null, values);
	}
	
	public void updatePlaylist(Playlist playlist)
	{
		ContentValues values = getPlaylistContentValues(playlist, false);
		
		String[] whereArgs = { String.valueOf(playlist.id) };
		db.update(Playlists.TABLE, values, Playlists._ID + " = ?", whereArgs);
	}

	public void deletePlaylist(long playlistId)
	{
		String[] selectionArgs = { String.valueOf(playlistId) };
		db.delete(Playlists.TABLE, Playlists._ID + " = ?", selectionArgs);
		db.delete(PlaylistTracks.TABLE, PlaylistTracks.PLAYLIST_ID + " = ?", selectionArgs);
	}


	// PlaylistTrack
	//

	public List<PlaylistTrack> getPlaylistTracks(long playlistId)
	{
		String[] selectionArgs = { String.valueOf(playlistId) };
		Cursor cursor = db.query(PlaylistTracks.VIEW, null, PlaylistTracks.PLAYLIST_ID + " = ?", selectionArgs,null, null, PlaylistTracks.PLAYLIST_TRACK_NO);

		List<PlaylistTrack> tracks = new ArrayList<>();

		while(cursor.moveToNext()){
			tracks.add(new PlaylistTrack(cursor));
		}

		cursor.close();

		return tracks;
	}
	
	public void insertPlaylistTrack(PlaylistTrack track)
	{
		ContentValues values = getPlaylistTrackContentValues(track, false);
		db.insert(PlaylistTracks.TABLE, null, values);
	}


	// Utils

	private ContentValues getTrackContentValues(Track t, boolean hasId)
	{
		ContentValues values = new ContentValues(21);

		if(hasId){
			values.put(Tracks._ID, t.id);
		}
		values.put(Tracks.PATH, t.path);
		values.put(Tracks.TITLE, t.title);
		values.put(Tracks.TITLE_SORT, t.titleSort);
		values.put(Tracks.ARTIST_ID, t.artistId);
		values.put(Tracks.ALBUM_ID, t.albumId);
		values.put(Tracks.COMPOSER, t.composer);
		values.put(Tracks.GENRE, t.genre);
		values.put(Tracks.TRACK_NO, t.trackNo);
		values.put(Tracks.DISC_NO, t.discNo);
		values.put(Tracks.DURATION, t.duration);
		values.put(Tracks.YEAR, t.year);
		values.put(Tracks.COMPILATION, t.compilation);
		values.put(Tracks.FILE_LAST_MODIFIED, t.fileLastModified);
		values.put(Tracks.ARTWORK_HASH, t.artworkHash);

		return values;
	}

	private ContentValues getAlbumContentValues(Album a, boolean hasId)
	{
		ContentValues values = new ContentValues(8);

		if(hasId){
			values.put(Albums._ID, a.id);
		}
		values.put(Albums.ALBUM, a.album);
		values.put(Albums.ALBUM_SORT, a.albumSort);
		values.put(Albums.ARTWORK_HASH, a.artworkHash);
		values.put(Albums.ARTIST_ID, a.artistId);
		values.put(Albums.YEAR, a.year);
		values.put(Albums.NUMBER_OF_SONGS, a.numSongs);
		values.put(Albums.COMPILATION, a.compilation);

		return values;
	}

	private ContentValues getArtistContentValues(Artist a, boolean hasId)
	{
		ContentValues values = new ContentValues(5);

		if(hasId){
			values.put(Artists._ID, a.id);
		}
		values.put(Artists.ARTIST, a.artist);
		values.put(Artists.ARTIST_SORT, a.artistSort);
		values.put(Artists.NUMBER_OF_ALBUMS, a.numAlbums);
		values.put(Artists.NUMBER_OF_SONGS, a.numSongs);

		return values;
	}

	private ContentValues getGenreContentValues(Genre g, boolean hasId)
	{
		ContentValues values = new ContentValues(2);

		if(hasId){
			values.put(Genres._ID, g.id);
		}
		values.put(Genres.GENRE, g.genre);

		return values;
	}

	private ContentValues getPlaylistContentValues(Playlist p, boolean hasId)
	{
		ContentValues values = new ContentValues(3);

		if(hasId){
			values.put(Playlists._ID, p.id);
		}
		values.put(Playlists.TITLE, p.title);
		values.put(Playlists.NUMBER_OF_SONGS, p.numSongs);

		return values;
	}

	private ContentValues getPlaylistTrackContentValues(PlaylistTrack t, boolean hasId)
	{
		ContentValues values = new ContentValues(20);

		if(hasId){
			values.put(PlaylistTracks._ID, t.id);
		}
		values.put(PlaylistTracks.TRACK_ID, t.trackId);
		values.put(PlaylistTracks.PLAYLIST_ID, t.playlistId);
		values.put(PlaylistTracks.PLAYLIST_TRACK_NO, t.playlistTrackNo);

		return values;
	}

	private Album extractAlbumFromTrack(Track t)
	{
		Album a = new Album(null);

		a.id = t.albumId;
		a.album = t.album;
		a.albumSort = t.albumSort;
		a.artist = t.albumArtist;
		a.artistSort = t.albumArtistSort;
		a.artworkHash = t.artworkHash;
		a.year = t.year;
		a.compilation = t.compilation;

		return a;
	}

	private Artist extractArtistFromTrack(Track t)
	{
		Artist a = new Artist(null);

		a.id = t.artistId;
		a.artist = t.artist;
		a.artistSort = t.artistSort;

		return a;
	}

	private Artist extractArtistFromAlbum(Album al)
	{
		Artist a = new Artist(null);

		a.id = al.artistId;
		a.artist = al.artist;
		a.artistSort = al.artistSort;

		return a;
	}
}

