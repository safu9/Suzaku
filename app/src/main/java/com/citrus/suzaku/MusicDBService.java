package com.citrus.suzaku;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.citrus.suzaku.MusicDB.Albums;
import com.citrus.suzaku.MusicDB.Artists;
import com.citrus.suzaku.MusicDB.Genres;
import com.citrus.suzaku.MusicDB.PlaylistTracks;
import com.citrus.suzaku.MusicDB.Playlists;
import com.citrus.suzaku.MusicDB.Tracks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


// For Updating MusicDB
public class MusicDBService extends IntentService
{
	// Actions From App
	public static final String ACTION_UPDATE_DATABASE = "Citrus.suzaku.action.ACTION_UPDATE_DATABASE";
	public static final String ACTION_UPDATE_TRACK = "Citrus.suzaku.action.ACTION_UPDATE_TRACK";
	public static final String ACTION_CREATE_PLAYLIST = "Citrus.suzaku.action.ACTION_CREATE_PLAYLIST";
	public static final String ACTION_EDIT_PLAYLIST = "Citrus.suzaku.action.ACTION_EDIT_PLAYLIST";
	public static final String ACTION_ADD_TO_PLAYLIST = "Citrus.suzaku.action.ACTION_ADD_TO_PLAYLIST";
	public static final String ACTION_DELETE_PLAYLISTTRACKS = "Citrus.suzaku.action.ACTION_DELETE_PLAYLISTTRACKS";
	public static final String ACTION_DELETE_PLAYLIST = "Citrus.suzaku.action.ACTION_DELETE_PLAYLIST";
	
	// BroadCast To ListFragments
	public static final String ACTION_DATABASE_CHANGED = "Citrus.suzaku.action.ACTION_DATABASE_CHANGED";
	public static final String ACTION_PLAYLIST_CHANGED = "Citrus.suzaku.action.ACTION_PLAYLIST_CHANGED";
	
	// Intent Extra Key
	public static final String INTENT_KEY_PATH = "PATH";
	public static final String INTENT_KEY_PLAYLIST = "PLAYLIST";
	public static final String INTENT_KEY_PLAYLIST_ID = "PLAYLIST_ID";
	public static final String INTENT_KEY_TRACKS = "TRACKS";
	
	
/*	public MusicDBService(String name)
	{
		super(name);
	}
*/
	public MusicDBService()
	{
		super("MusicDBService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String action = intent.getAction();
		
		App.logd("MDBS Received : " + action);

		if(action == null){
			return;
		}else switch(action){
			case ACTION_UPDATE_DATABASE:
				updateDB();

				notifyAction(ACTION_DATABASE_CHANGED);
				break;
		
			case ACTION_UPDATE_TRACK:
				String path = (String)intent.getSerializableExtra(INTENT_KEY_PATH);
				updateTrack(path);
			
				notifyAction(ACTION_DATABASE_CHANGED);
				break;
		
			case ACTION_CREATE_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				createPlaylist(playlist);

				notifyAction(ACTION_PLAYLIST_CHANGED);
				break;
			}
			case ACTION_EDIT_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				updatePlaylist(playlist);

				notifyAction(ACTION_PLAYLIST_CHANGED);
				break;
			}
			case ACTION_ADD_TO_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				List<Track> tracks = (List<Track>) intent.getSerializableExtra(INTENT_KEY_TRACKS);
				addPlaylistTracks(playlist, tracks);

				notifyAction(ACTION_PLAYLIST_CHANGED);
				break;
			}
			case ACTION_DELETE_PLAYLISTTRACKS: {
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				List<PlaylistTrack> tracks = (List<PlaylistTrack>) intent.getSerializableExtra(INTENT_KEY_TRACKS);
				deletePlaylistTracks(playlist, tracks);

				notifyAction(ACTION_PLAYLIST_CHANGED);
				break;
			}
			case ACTION_DELETE_PLAYLIST:
				long playlistId = intent.getLongExtra(INTENT_KEY_PLAYLIST_ID, -1);
				deletePlaylist(playlistId);
			
				notifyAction(ACTION_PLAYLIST_CHANGED);
				break;
		}
	}
	
	//
	// Update Database
	
	// ACTION_UPDATE_DATABASE
	private void updateDB()
	{
		long startTime = System.currentTimeMillis();
		
		showToast(R.string.notify_scanning);

		Log.d("Suzaku", "MDBS Updating DB");
		//! DEBUG
	//	long kb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
	//	Log.d("Suzaku", "mem : " + kb + " KB");
		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
		am.getMemoryInfo(info);
		double linuxFreeHeap = info.availMem * 100 / 1024 / 1024 / 100.0;


		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
	//	db.enableWriteAheadLogging();

		updateAllTracks(db);

		updateAlbums(db);
		updateArtists(db);
		updateGenres(db);

	//	db.disableWriteAheadLogging();
	//	db.close();

		//! DEBUG
		am.getMemoryInfo(info);
		double newLinuxFreeHeap = info.availMem * 100 / 1024 / 1024 / 100.0;

		showToast(R.string.notify_scan_finished);
		
		Log.d("Suzaku", "MDBS Updated DB  time:" + (System.currentTimeMillis() - startTime) / 1000.0 + "s memory:" + (newLinuxFreeHeap - linuxFreeHeap) + "MB");
	}
	
	//!! MEMORY LEAK
	private void updateAllTracks(SQLiteDatabase db)
	{
		long time = System.currentTimeMillis();

		ArrayList<String> rootPaths = MyPreference.getStringList(MyPreference.MUSIC_FOLDER);

		//! TENTATIVE
		if(rootPaths.size() == 0){
			rootPaths.add(Environment.getExternalStorageDirectory().getAbsolutePath());
			rootPaths.addAll(App.getSdCardFilesDirPathList());
		}

		String[] whereArgs = rootPaths.toArray(new String[rootPaths.size()]);
		String where = "";
		for(int i = 0; i < rootPaths.size(); i++){
			if(i != 0){
				where += "AND ";
			}
			where += Tracks.PATH + " NOT LIKE ? || '/%' ";
		}

		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			mdb.deleteTracks(where, whereArgs);
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}

		for(String path : rootPaths){
			App.logd("MDBS UT : " + path);
			File rootDir = new File(path);
			if(!rootDir.exists()){
				return;
			}

			updateAllTracksInStorage(db, path);
		}

		App.logd("MDBS UT " + (System.currentTimeMillis() - time));
	}

	// 各ストレージ内のトラックを更新
	private void updateAllTracksInStorage(SQLiteDatabase db, String rootPath)
	{
		String[] whereArgs = { rootPath + "/" };
		List<Track> tracks = MyMediaStoreManager.getTracks(MediaStore.Audio.Media.DATA + " LIKE ? || '%'", whereArgs);		
		
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			
			for(Track track : tracks){

				File file = new File(track.path);

				String[] columns = { Tracks.FILE_LAST_MODIFIED };
				String[] selectionArgs = { track.path };
				Cursor cursor = db.query(Tracks.TABLE, columns, Tracks.PATH + "= ?", selectionArgs, null, null, null);

				try{
					if(cursor.moveToFirst()){
						long lastModified = cursor.getLong(cursor.getColumnIndex(Tracks.FILE_LAST_MODIFIED));

						if(file.lastModified() == lastModified){
							continue;
						}
					}
				}finally{
					cursor.close();
				}

				App.logd("MDBS Updating : " + track.title + " (" + track.path + ")");

				getMediaMetadata(track);
				track.fileLastModified = file.lastModified();

				mdb.insertTrack(track);

				ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
				ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
				am.getMemoryInfo(info);
				double linuxFreeHeap = info.availMem * 100 / 1024 / 1024 / 100.0;
				App.logd("MDBS Free LinuxHeap : " + linuxFreeHeap + "MB");
			}
		
			db.setTransactionSuccessful();
			MyPreference.putLong(MyPreference.DB_LAST_UPDATED, System.currentTimeMillis());
		}finally{
			db.endTransaction();
		}
	}
	
	//! UNUSED
	private void updateTracks(SQLiteDatabase db)
	{
		String rootPath = MyPreference.getString(MyPreference.MUSIC_FOLDER, Environment.getExternalStorageDirectory().getAbsolutePath());
		long lastUpdated = MyPreference.getLong(MyPreference.DB_LAST_UPDATED);

		File rootDir = new File(rootPath);
		if(!rootDir.exists()){
			return;
		}
			
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			String[] selectionArgs = { rootDir.getAbsolutePath() + "/" };
			mdb.deleteTracks(Tracks.PATH + " NOT LIKE ? || '%'", selectionArgs);
			
			updateTracksInDirectory(db, rootDir, lastUpdated);
			
			db.setTransactionSuccessful();
			MyPreference.putLong(MyPreference.DB_LAST_UPDATED, System.currentTimeMillis());
		}finally{
			db.endTransaction();
		}
	}

	// フォルダ内のトラックを再起呼び出しで更新
	private void updateTracksInDirectory(SQLiteDatabase db, File dir, long lastUpdated)
	{
		File[] files = dir.listFiles();
		String[] names = dir.list();
		
		if(Arrays.asList(names).contains(".nomedia")){
			return;
		}
		
		MusicDB mdb = new MusicDB(db);
		
		for(File file : files){
			if(file.lastModified() < lastUpdated){
				continue;
			}
			
			if(file.isDirectory()){
				updateTracksInDirectory(db, file, lastUpdated);
			}else{
				String where = MediaStore.Audio.Media.DATA + " LIKE ? || '%' AND " + MediaStore.Audio.Media.DATA + " NOT LIKE ? || '%/%'";
				String[] whereArgs = { file.getAbsolutePath() };
				List<Track> tracks = MyMediaStoreManager.getTracks(where, whereArgs);

				if(tracks.size() == 0){
					return;
				}
				
				Track track = tracks.get(0);
				
				String[] columns = { Tracks.FILE_LAST_MODIFIED };
				String[] selectionArgs = { track.path };
				Cursor cursor = db.query(Tracks.TABLE, columns, Tracks.PATH + "= ?", selectionArgs, null, null, null);

				try{
					if(cursor.moveToFirst()){
						long lastModified = cursor.getLong(cursor.getColumnIndex(Tracks.FILE_LAST_MODIFIED));

						if(file.lastModified() == lastModified){
							continue;
						}
					}
				}finally{
					cursor.close();
				}

				App.logd("MDBS Updating : " + track.title + " (" + track.path + ")");

				getMediaMetadata(track);
				track.fileLastModified = file.lastModified();

				mdb.insertTrack(track);
			}
		}
	}
	
	// ACTION_UPDATE_TRACK
	private void updateTrack(String path)
	{
		String[] whereArgs = { path };
		List<Track> tracks = MyMediaStoreManager.getTracks(MediaStore.Audio.Media.DATA + " = ?", whereArgs);

		if(tracks.size() == 0){
			return;
		}

		Track track = tracks.get(0);
		
		File file = new File(track.path);
		if(!file.exists()){
			return;
		}

		getMediaMetadata(track);
		track.fileLastModified = file.lastModified();

		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		MusicDB mdb = new MusicDB(db);
		
		db.beginTransactionNonExclusive();
		try{
			mdb.insertTrack(track);
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		
		updateAlbums(db);
		updateArtists(db);
		updateGenres(db);
		
	//	db.close();
	}

	private static void getMediaMetadata(Track t)
	{
        TagLibHelper tag = new TagLibHelper();
        tag.setFile(t.path);

        t.title = ifEmpty(tag.getTitle(), t.title);
        t.titleSort = ifEmpty(tag.getTitleSort(), t.title);

		t.artist = ifEmpty(tag.getArtist(), MusicDB._NULL);
		t.artistSort = ifEmpty(tag.getArtistSort(), t.artist);
        t.album = ifEmpty(tag.getAlbum(), MusicDB._NULL);
		t.albumSort = ifEmpty(tag.getAlbumSort(), t.album);
        t.albumArtist = ifEmpty(tag.getAlbumArtist(), t.artist);
		t.albumArtistSort = ifEmpty(tag.getAlbumArtistSort(), t.albumArtist.equals(t.artist)? t.artistSort : t.albumArtist);

        t.composer = ifEmpty(tag.getComposer(), MusicDB._NULL);
        t.genre = ifEmpty(tag.getGenre(), MusicDB._NULL);

        t.discNo = tag.getDiscNumber();
        t.trackNo = tag.getTrackNumber();

        t.compilation = tag.getCompilation();

        t.artworkHash = ArtworkCache.getHash(tag.getArtwork());

        tag.release();
    }

	private void updateAlbums(SQLiteDatabase db)
	{
		long time = System.currentTimeMillis();
		
		String stmt;
		
		db.beginTransactionNonExclusive();
		try{
			db.execSQL("DELETE FROM " + Albums.TABLE + ";");

			// 各アルバムで DISC_NO, TRACK_NO が最小のデータを INSERT
			stmt =
				"INSERT INTO " + Albums.TABLE + " (" +
					Albums.ALBUM + "," + Albums.ALBUM_SORT + "," + Albums.ARTIST + "," + Albums.ARTIST_SORT + "," + Albums.ARTWORK_HASH + "," + Albums.YEAR + "," + Albums.NUMBER_OF_SONGS +
				") " +
				"SELECT t3." + Tracks.ALBUM + ",t3." + Tracks.ALBUM_SORT + ",t3." + Tracks.ALBUMARTIST + ",t3." + Tracks.ALBUMARTIST_SORT + ",t3." + Tracks.ARTWORK_HASH + ",t4." + Tracks.YEAR + ",t4." + Albums.NUMBER_OF_SONGS +
				" FROM (" +
					"SELECT " + Tracks.ALBUM + "," + Tracks.ALBUM_SORT + "," + Tracks.ALBUMARTIST + "," + Tracks.ALBUMARTIST_SORT + "," + Tracks.ARTWORK_HASH +
					" FROM " + Tracks.TABLE + " t1 " +
					"WHERE t1." + Tracks._ID + " = (" +
						"SELECT " + Tracks._ID +
						" FROM " + Tracks.TABLE + " t2 WHERE t1." + Tracks.ALBUM + " = t2." + Tracks.ALBUM + " ORDER BY " + Tracks.DISC_NO + "," + Tracks.TRACK_NO + " LIMIT 1" +
					")" +
				") t3 " +
				"NATURAL INNER JOIN (" +
					"SELECT " + Tracks.ALBUM + ", MAX(" + Tracks.YEAR + ") AS " + Tracks.YEAR + ", COUNT(" + Tracks._ID + ") AS " + Albums.NUMBER_OF_SONGS +
					" FROM " + Tracks.TABLE + " GROUP BY " + Tracks.ALBUM +
				") t4;";
			//	"ON (t3." + Tracks.ALBUM + " = t4." + Tracks.ALBUM + ");";
				
			SQLiteStatement insertStmt = db.compileStatement(stmt);
			try{
				insertStmt.executeInsert();
			}finally{
				insertStmt.close();
			}
			
			stmt =
				"UPDATE " + Albums.TABLE + " SET " +
				Albums.COMPILATION + " = " + "(" +
					"CASE WHEN (" +
						"SELECT COUNT(DISTINCT " + Tracks.ALBUMARTIST + ") FROM " + Tracks.TABLE + " t " +
						"WHERE t." + Tracks.ALBUM + " = " + Albums.TABLE + "." + Albums.ALBUM +
					") > 1 THEN 1 ELSE 0 END" +
				");";
			
			SQLiteStatement updateStmt2 = db.compileStatement(stmt);
			try{
				updateStmt2.executeUpdateDelete();
			}finally{
				updateStmt2.close();
			}
			
			stmt =
				"UPDATE " + Tracks.TABLE + " SET " +
				Tracks.ALBUM_ID + " = (" +
					"SELECT " + Albums._ID + " FROM " + Albums.TABLE + " t WHERE t." + Albums.ALBUM + " = " + Tracks.TABLE + "." + Tracks.ALBUM +
				");";

			SQLiteStatement updateStmt = db.compileStatement(stmt);
			try{
				updateStmt.executeUpdateDelete();
			}finally{
				updateStmt.close();
			}
			
			stmt =
				"UPDATE " + Tracks.TABLE + " SET " +
				Tracks.COMPILATION + " = 1 " +
				"WHERE " + Tracks.ALBUM_ID + " IN (" +
					"SELECT " + Albums._ID + " FROM " + Albums.TABLE + " WHERE " + Albums.COMPILATION + " = 1" +
				");";
			
			SQLiteStatement updateStmt3 = db.compileStatement(stmt);
			try{
				updateStmt3.executeUpdateDelete();
			}finally{
				updateStmt3.close();
			}
				
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		
		App.logd("MDBS UAl " + (System.currentTimeMillis() - time));
	}

	private void updateArtists(SQLiteDatabase db)
	{
		long time = System.currentTimeMillis();
		
		String stmt;
		
		boolean gc = MyPreference.getBoolean(MyPreference.GROUP_COMPILATION);
		
		db.beginTransactionNonExclusive();
		try{
			db.execSQL("DELETE FROM " + Artists.TABLE + ";");

			if(!gc){
				stmt =
					"INSERT INTO " + Artists.TABLE + "(" +
						Artists.ARTIST + "," + Artists.ARTIST_SORT + "," + Artists.NUMBER_OF_SONGS + "," + Artists.NUMBER_OF_ALBUMS +
					") SELECT " + 
					"t." + Tracks.ARTIST + ",t." + Artists.ARTIST_SORT + ",t." + Artists.NUMBER_OF_SONGS + ",t." + Artists.NUMBER_OF_ALBUMS +
					" FROM (" +
						"SELECT " + Tracks.ARTIST + ", MAX(" + Tracks.ARTIST_SORT + ") AS " + Artists.ARTIST_SORT + ", COUNT(*) AS " + Artists.NUMBER_OF_SONGS + ", COUNT(DISTINCT " + Tracks.ALBUM + ") AS " + Artists.NUMBER_OF_ALBUMS +
						" FROM " + Tracks.TABLE + " GROUP BY " + Tracks.ARTIST +
					") t";
					
				SQLiteStatement insertStmt = db.compileStatement(stmt);
				try{
					insertStmt.executeInsert();
				}finally{
					insertStmt.close();
				}

				stmt =
					"UPDATE " + Tracks.TABLE + " SET " +
					Tracks.ARTIST_ID + " = (" +
						"SELECT " + Artists._ID + " FROM " + Artists.TABLE + " t WHERE t." + Artists.ARTIST + " = " + Tracks.TABLE + "." + Tracks.ARTIST +
					");";
				
				SQLiteStatement updateStmt = db.compileStatement(stmt);
				try{
					updateStmt.executeUpdateDelete();
				}finally{
					updateStmt.close();
				}
			}else{
				stmt =
					"INSERT INTO " + Artists.TABLE + "(" +
						Artists.ARTIST + "," + Artists.ARTIST_SORT + "," + Artists.NUMBER_OF_SONGS + "," + Artists.NUMBER_OF_ALBUMS +
					") SELECT " + 
					Albums.ARTIST + "," + Artists.ARTIST_SORT + "," + Artists.NUMBER_OF_SONGS + "," + Artists.NUMBER_OF_ALBUMS +
					" FROM (" +
						"SELECT " + Albums.ARTIST + ", MAX(" + Albums.ARTIST_SORT + ") AS " + Artists.ARTIST_SORT + ", SUM(" + Albums.NUMBER_OF_SONGS + ") AS " + Artists.NUMBER_OF_SONGS + ", COUNT(*) AS " + Artists.NUMBER_OF_ALBUMS +
						" FROM " + Albums.TABLE + " WHERE " + Albums.COMPILATION + " = 0 GROUP BY " + Albums.ARTIST +
					");";

				SQLiteStatement insertStmt = db.compileStatement(stmt);
				try{
					insertStmt.executeInsert();
				}finally{
					insertStmt.close();
				}

				stmt =
					"UPDATE " + Tracks.TABLE + " SET " +
					Tracks.ARTIST_ID + " = (" +
						"SELECT " + Artists._ID + " FROM " + Artists.TABLE + " t WHERE t." + Artists.ARTIST + " = " + Tracks.TABLE + "." + Tracks.ALBUMARTIST +
					");";
			
				SQLiteStatement updateStmt = db.compileStatement(stmt);
				try{
					updateStmt.executeUpdateDelete();
				}finally{
					updateStmt.close();
				}
			}

			stmt =
				"UPDATE " + Albums.TABLE + " SET " +
				Albums.ARTIST_ID + " = (" +
					"SELECT " + Artists._ID + " FROM " + Artists.TABLE + " t WHERE t." + Artists.ARTIST + " = " + Albums.TABLE + "." + Albums.ARTIST +
				");";

			SQLiteStatement updateStmt = db.compileStatement(stmt);
			try{
				updateStmt.executeUpdateDelete();
			}finally{
				updateStmt.close();
			}
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		
		App.logd("MDBS UAr " + (System.currentTimeMillis() - time));
	}
	
	private void updateGenres(SQLiteDatabase db)
	{
		long time = System.currentTimeMillis();

		String stmt;

		db.beginTransactionNonExclusive();
		try{
			db.execSQL("DELETE FROM " + Genres.TABLE + ";");

			stmt =
				"INSERT INTO " + Genres.TABLE + "(" +
					Genres.GENRE +
				") SELECT " + Genres.GENRE +
				" FROM " + Tracks.TABLE + " GROUP BY " + Genres.GENRE + ";";

			SQLiteStatement insertStmt = db.compileStatement(stmt);
			try{
				insertStmt.executeInsert();
			}finally{
				insertStmt.close();
			}

			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}

		App.logd("MDBS UG " + (System.currentTimeMillis() - time));
	}
	

	
	//
	// Playlists

	// ACTION_CREATE_PLAYLIST
	private long createPlaylist(Playlist playlist)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			mdb.insertPlaylist(playlist);

			String[] columns = { Playlists._ID };
			String[] selectionArgs = { playlist.title };

			Cursor cursor = db.query(Playlists.TABLE, columns, Playlists.TITLE + " = ?", selectionArgs, null, null, null);
			cursor.moveToFirst();
			playlist.id = cursor.getLong(0);
			cursor.close();

			MusicDBHelper.createPlaylistTrackTable(db, playlist.id);

			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}

		return playlist.id;
	}
	
	// ACTION_EDIT_PLAYLIST
	private void updatePlaylist(Playlist playlist)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			mdb.updatePlaylist(playlist);
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}
	
	// ACTION_DELETE_PLAYLIST
	private void deletePlaylist(long playlistId)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			mdb.deletePlaylist(playlistId);
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}

	private void updateNumSongsInPlaylist(SQLiteDatabase db, long playlistId)
	{
		String table = PlaylistTracks.TABLE + String.valueOf(playlistId);
		Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table + ";" , null);
		cursor.moveToFirst();
		int songs = cursor.getInt(0);
		cursor.close();

		ContentValues values = new ContentValues(1);
		values.put(Playlists.NUMBER_OF_SONGS, songs);

		String[] selectionArgs = { String.valueOf(playlistId) };
		db.update(Playlists.TABLE, values, Playlists._ID + " = ?", selectionArgs);
	}

	private void updateNumSongsInPlaylist(SQLiteDatabase db, long playlistId, int variation)
	{
		String stmt =
			"UPDATE " + Playlists.TABLE + " SET " +
			Playlists.NUMBER_OF_SONGS + " = " + Playlists.NUMBER_OF_SONGS + " + " + String.valueOf(variation) +
			" WHERE " + Playlists._ID + " = " + String.valueOf(playlistId) + ";";

		SQLiteStatement updateStmt = db.compileStatement(stmt);
		try{
			updateStmt.executeUpdateDelete();
		}finally{
			updateStmt.close();
		}
	}
	
	// PlaylistTrack

	// ACTION_ADD_TO_PLAYLIST
	private void addPlaylistTracks(Playlist playlist, List<Track> tracks)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			
			int i = mdb.getPlaylistTracks(playlist.id).size();
			for(Track track : tracks){
				PlaylistTrack playlistTrack = new PlaylistTrack(null);
				playlistTrack.setTrackInfo(track);
				playlistTrack.trackId = track.id;
				playlistTrack.playlistTrackNo = i;

				mdb.insertPlaylistTrack(playlist.id, playlistTrack);

				i++;
			}

			updateNumSongsInPlaylist(db, playlist.id, tracks.size());

			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		
		showToast(R.string.notify_added_to_playlist);
	}

	// ACTION_DELETE_PLAYLISTTRACKS
	private void deletePlaylistTracks(Playlist playlist, List<PlaylistTrack> tracks)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			String stmt =
				"DELETE FROM " + PlaylistTracks.TABLE + String.valueOf(playlist.id) +
				" WHERE " + PlaylistTracks._ID + " = ?;";

			SQLiteStatement deleteStmt = db.compileStatement(stmt);

			try{
				for(Track t : tracks){
					deleteStmt.bindLong(1, t.id);
					deleteStmt.executeUpdateDelete();
				}
			}finally{
				deleteStmt.close();
			}

			updateNumSongsInPlaylist(db, playlist.id, -tracks.size());

			// Playlist TrackNo を更新
			List<PlaylistTrack> ptracks = playlist.getPlaylistTracks();
			for(int i = 0; i < ptracks.size(); i++){
				PlaylistTrack ptrack = ptracks.get(i);
				if(ptrack.playlistTrackNo != i){
					ContentValues values = new ContentValues(1);
					values.put(PlaylistTracks.PLAYLIST_TRACK_NO, i);

					String[] selectionArgs = { String.valueOf(ptrack.id) };
					db.update(PlaylistTracks.TABLE + String.valueOf(playlist.id), values, PlaylistTracks._ID + " = ?", selectionArgs);
				}
			}
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}

	
	// Utils
	
	private static boolean isEmpty(String s)
	{
		return (s == null || s.isEmpty());
	}
	
	private static String ifEmpty(String s, String sub)
	{
		return (!isEmpty(s))? s : sub;
	}
	
	private void showToast(int stringId)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable(){
			int stringId;
			
			public Runnable setArgs(int stringId)
			{
				this.stringId = stringId;
				return this;
			}
			
			@Override
			public void run()
			{
				Toast.makeText(App.getContext(), stringId, Toast.LENGTH_SHORT).show();
			}
		}.setArgs(stringId));
	}
	
	// Broadcast
	private void notifyAction(String action)
	{
		sendBroadcast(new Intent(action));
	}
	
	
	// Content Provider Manager
	private static class MyMediaStoreManager
	{
		private static final String TRACK_WHERE = MediaStore.Audio.Media.IS_MUSIC + "= 1 AND " + MediaStore.Audio.Media.DURATION + ">= 1000";
		
		private static final String[] TRACK_FILLED_PROJECTION = {
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.TITLE,
		//	MediaStore.Audio.Media.TITLE_KEY,
			MediaStore.Audio.Media.ALBUM,
		//	MediaStore.Audio.Media.ALBUM_ID,
		//	MediaStore.Audio.Media.ALBUM_KEY,
			MediaStore.Audio.Media.ARTIST,
		//	MediaStore.Audio.Media.ARTIST_ID,
		//	MediaStore.Audio.Media.ARTIST_KEY,
			MediaStore.Audio.Media.COMPOSER,
			MediaStore.Audio.Media.TRACK,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.YEAR
		};

		public static List<Track> getTracks(String where, String[] whereArgs)
		{
			if(where == null){
				where = TRACK_WHERE;
			}else{
				where = TRACK_WHERE + " AND " + where;
			}
			
			ContentResolver resolver = App.getContext().getContentResolver();
			Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, TRACK_FILLED_PROJECTION,
										   where, whereArgs, null);

			List<Track> tracks = new LinkedList<>();

			while(cursor.moveToNext()){
				Track t = new Track(null);

				t.id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
				t.path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
				t.title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
			//	t.titleKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY));
				t.album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
			//	t.albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
				t.artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			//	t.artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
				t.composer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER));
				t.trackNo = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
				t.duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				t.year = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR));

			//	uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

				tracks.add(t);
			}

			cursor.close();
			return tracks;
		}
	}
	
}
