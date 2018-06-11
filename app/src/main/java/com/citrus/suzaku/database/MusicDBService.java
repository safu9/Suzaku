package com.citrus.suzaku.database;

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

import com.citrus.suzaku.App;
import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.R;
import com.citrus.suzaku.TagLibHelper;
import com.citrus.suzaku.database.MusicDB.Albums;
import com.citrus.suzaku.database.MusicDB.Artists;
import com.citrus.suzaku.database.MusicDB.Genres;
import com.citrus.suzaku.database.MusicDB.PlaylistTracks;
import com.citrus.suzaku.database.MusicDB.Playlists;
import com.citrus.suzaku.database.MusicDB.Tracks;
import com.citrus.suzaku.playlist.Playlist;
import com.citrus.suzaku.playlist.PlaylistTrack;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.track.Track;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


// For Updating MusicDB
public class MusicDBService extends IntentService
{
	// Actions From App
	public static final String ACTION_UPDATE_DATABASE = "com.citrus.suzaku.action.ACTION_UPDATE_DATABASE";
	public static final String ACTION_UPDATE_TRACKS = "com.citrus.suzaku.action.ACTION_UPDATE_TRACKS";
	public static final String ACTION_CREATE_PLAYLIST = "com.citrus.suzaku.action.ACTION_CREATE_PLAYLIST";
	public static final String ACTION_EDIT_PLAYLIST = "com.citrus.suzaku.action.ACTION_EDIT_PLAYLIST";
	public static final String ACTION_DELETE_PLAYLIST = "com.citrus.suzaku.action.ACTION_DELETE_PLAYLIST";
	public static final String ACTION_ADD_TO_PLAYLIST = "com.citrus.suzaku.action.ACTION_ADD_TO_PLAYLIST";
	public static final String ACTION_UPDATE_PLAYLISTTRACKS = "com.citrus.suzaku.action.ACTION_UPDATE_PLAYLISTTRACKS";
	public static final String ACTION_DELETE_PLAYLISTTRACKS = "com.citrus.suzaku.action.ACTION_DELETE_PLAYLISTTRACKS";

	// Intent Extra Key
	public static final String INTENT_KEY_PATHS = "PATHS";
	public static final String INTENT_KEY_PLAYLIST = "PLAYLIST";
	public static final String INTENT_KEY_PLAYLIST_ID = "PLAYLIST_ID";
	public static final String INTENT_KEY_TRACK_IDS = "TRACK_IDS";
	public static final String INTENT_KEY_PLAYLISTTRACK = "PLAYLISTTRACK";
	public static final String INTENT_KEY_PLAYLISTTRACK_IDS = "PLAYLISTTRACK_IDS";
	
	
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
		}
		switch(action){
			case ACTION_UPDATE_DATABASE:
				updateDB();

				postEvent(new DatabaseChangedEvent());
				break;
		
			case ACTION_UPDATE_TRACKS:
				List<String> paths = (List<String>)intent.getSerializableExtra(INTENT_KEY_PATHS);
				updateTracks(paths);
			
				postEvent(new DatabaseChangedEvent());
				break;
		
			case ACTION_CREATE_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				createPlaylist(playlist);

				postEvent(new PlaylistChangedEvent());
				break;
			}
			case ACTION_EDIT_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				updatePlaylist(playlist);

				postEvent(new PlaylistChangedEvent());
				break;
			}
			case ACTION_DELETE_PLAYLIST:
				long playlistId = intent.getLongExtra(INTENT_KEY_PLAYLIST_ID, -1);
				deletePlaylist(playlistId);

				postEvent(new PlaylistChangedEvent());
				break;

			case ACTION_ADD_TO_PLAYLIST:{
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				List<Long> trackIds = (List<Long>) intent.getSerializableExtra(INTENT_KEY_TRACK_IDS);
				addPlaylistTracks(playlist, trackIds);

				postEvent(new PlaylistChangedEvent());
				break;
			}
			case ACTION_UPDATE_PLAYLISTTRACKS: {
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				List<PlaylistTrack> ptracks = (List<PlaylistTrack>) intent.getSerializableExtra(INTENT_KEY_PLAYLISTTRACK);
				updatePlaylistTracks(playlist, ptracks);

				postEvent(new PlaylistChangedEvent());
				break;
			}
			case ACTION_DELETE_PLAYLISTTRACKS: {
				Playlist playlist = (Playlist) intent.getSerializableExtra(INTENT_KEY_PLAYLIST);
				List<Long> ptrackIds = (List<Long>) intent.getSerializableExtra(INTENT_KEY_PLAYLISTTRACK_IDS);
				deletePlaylistTracks(playlist, ptrackIds);

				postEvent(new PlaylistChangedEvent());
				break;
			}
		}
	}
	
	//
	// Update Database
	
	// ACTION_UPDATE_DATABASE
	private void updateDB()
	{
		long startTime = System.currentTimeMillis();
		
		showToast(R.string.msg_scanning);

		Log.d("Suzaku", "MDBS Updating DB");
		//! DEBUG
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

		showToast(R.string.msg_scan_finished);
		
		Log.d("Suzaku", "MDBS Updated DB  time:" + (System.currentTimeMillis() - startTime) / 1000.0 + "s memory:" + (newLinuxFreeHeap - linuxFreeHeap) + "MB");
	}

	private void updateAllTracks(SQLiteDatabase db)
	{
		long time = System.currentTimeMillis();

		ArrayList<String> rootPaths = PreferenceUtils.getStringList(PreferenceUtils.MUSIC_FOLDER);

		//! TENTATIVE
		if(rootPaths.size() == 0){
			rootPaths.add(Environment.getExternalStorageDirectory().getAbsolutePath());
			rootPaths.addAll(App.getSdCardFilesDirPathList());
		}

		deleteTracksOutOfLibrary(db, rootPaths);
		deleteNonexistentTracks(db);

		for(String path : rootPaths){
			App.logd("MDBS UT : " + path);
			File rootDir = new File(path);
			if(!rootDir.exists()){
				continue;
			}

			updateAllTracksInStorage(db, path);
		}

		App.logd("MDBS UT " + (System.currentTimeMillis() - time));
	}

	// 指定フォルダ以外のトラックを削除
	private void deleteTracksOutOfLibrary(SQLiteDatabase db, ArrayList<String> rootPaths)
	{
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
	}

	// 存在しないトラックを削除
	private void deleteNonexistentTracks(SQLiteDatabase db)
	{
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			for(Track track : mdb.getAllTracks()){
				File file = new File(track.path);
				if(!file.exists()){
					String[] whereArgs = { String.valueOf(track.id) };
					mdb.deleteTracks(Tracks._ID + "= ?", whereArgs);
				}
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}

	// 各ストレージ内のトラックを更新
	private void updateAllTracksInStorage(SQLiteDatabase db, String rootPath)
	{
		String[] whereArgs = { rootPath + "/" };
		List<Track> tracks = MyMediaStoreManager.getTracks(MediaStore.Audio.Media.DATA + " LIKE ? || '%'", whereArgs);		// "||" は文字列連結
		
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);

			int i = 1;
			MediaScanningEvent event = new MediaScanningEvent();
			event.rootPath = rootPath;

			for(Track track : tracks){

				File file = new File(track.path);

				String[] columns = { Tracks.FILE_LAST_MODIFIED };
				String[] selectionArgs = { track.path };
				Cursor cursor = db.query(Tracks.TABLE, columns, Tracks.PATH + "= ?", selectionArgs, null, null, null);

				try{
					if(cursor.moveToFirst()){
						long lastModified = cursor.getLong(cursor.getColumnIndex(Tracks.FILE_LAST_MODIFIED));

						if(file.lastModified() == lastModified){
							i++;
							continue;
						}
					}
				}finally{
					cursor.close();
				}

				event.title = track.title;
				event.percent = ++i * 100 / tracks.size();
				EventBus.getDefault().post(event);

//				App.logd("MDBS Updating : " + track.title + " (" + track.path + ")");

				getMediaMetadata(track);
				track.fileLastModified = file.lastModified();

				mdb.insertTrack(track);
			}
		
			db.setTransactionSuccessful();
			PreferenceUtils.putLong(PreferenceUtils.DB_LAST_UPDATED, System.currentTimeMillis());
		}finally{
			db.endTransaction();
		}
	}

	// ACTION_UPDATE_TRACKS
	private void updateTracks(List<String> paths)
	{
		List<Track> tracks = new ArrayList<>();

		for(String path : paths){
			String[] whereArgs = { path };
			List<Track> results = MyMediaStoreManager.getTracks(MediaStore.Audio.Media.DATA + " = ?", whereArgs);

			if(results.size() == 0){
				continue;
			}

			Track track = results.get(0);

			File file = new File(track.path);
			if(!file.exists()){
				continue;
			}

			getMediaMetadata(track);
			track.fileLastModified = file.lastModified();

			tracks.add(track);
		}

		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		MusicDB mdb = new MusicDB(db);
		
		db.beginTransactionNonExclusive();
		try{
			for(Track track : tracks){
				mdb.insertTrack(track);
			}
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
			// 曲数を設定
			stmt =
				"UPDATE " + Albums.TABLE + " SET " +
				Albums.NUMBER_OF_SONGS + " = " + "(" +
					"SELECT COUNT(*) FROM " + Tracks.TABLE + " t " +
					"WHERE t." + Tracks.ALBUM_ID + " = " + Albums.TABLE + "." + Albums._ID +
				");";

			SQLiteStatement updateStmt = db.compileStatement(stmt);
			try{
				updateStmt.executeUpdateDelete();
			}finally{
				updateStmt.close();
			}

			// トラックにコンピレーションを設定
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
		
		boolean gc = PreferenceUtils.getBoolean(PreferenceUtils.GROUP_COMPILATION);
		
		db.beginTransactionNonExclusive();
		try{
			if(!gc){
				// 曲数を設定
				stmt =
					"UPDATE " + Artists.TABLE + " SET " +
					Artists.NUMBER_OF_SONGS + " = (" +
						"SELECT COUNT(*) AS " + Artists.NUMBER_OF_SONGS + " FROM " + Tracks.TABLE + " t " +
						"WHERE t." + Tracks.ARTIST_ID + " = " + Artists.TABLE + "." + Artists._ID +
					");";

				SQLiteStatement updateStmt = db.compileStatement(stmt);
				try{
					updateStmt.executeInsert();
				}finally{
					updateStmt.close();
				}

				// アルバム数を設定
				stmt =
					"UPDATE " + Artists.TABLE + " SET " +
					Artists.NUMBER_OF_ALBUMS + " = (" +
						"SELECT COUNT(DISTINCT " + Tracks.ALBUM_ID + ") AS " + Artists.NUMBER_OF_ALBUMS + " FROM " + Tracks.TABLE + " t " +
						"WHERE t." + Tracks.ARTIST_ID + " = " + Artists.TABLE + "." + Artists._ID +
					");";

				SQLiteStatement updateStmt2 = db.compileStatement(stmt);
				try{
					updateStmt2.executeInsert();
				}finally{
					updateStmt2.close();
				}
			}else{
				// 曲数を設定
				stmt =
					"UPDATE " + Artists.TABLE + " SET " +
					Artists.NUMBER_OF_SONGS + " = (" +
						"SELECT SUM(" + Albums.NUMBER_OF_SONGS + ") AS " + Artists.NUMBER_OF_SONGS + " FROM " + Albums.TABLE + " t " +
						"WHERE t." + Albums.ARTIST_ID + " = " + Artists.TABLE + "." + Artists._ID + " AND t." + Albums.COMPILATION + "=0" +
					");";

				SQLiteStatement updateStmt = db.compileStatement(stmt);
				try{
					updateStmt.executeInsert();
				}finally{
					updateStmt.close();
				}

				// アルバム数を設定
				stmt =
					"UPDATE " + Artists.TABLE + " SET " +
					Artists.NUMBER_OF_ALBUMS + " = (" +
						"SELECT COUNT(*) AS " + Artists.NUMBER_OF_ALBUMS + " FROM " + Albums.TABLE + " t " +
						"WHERE t." + Albums.ARTIST_ID + " = " + Artists.TABLE + "." + Artists._ID + " AND t." + Albums.COMPILATION + "=0" +
					");";

				SQLiteStatement updateStmt2 = db.compileStatement(stmt);
				try{
					updateStmt2.executeInsert();
				}finally{
					updateStmt2.close();
				}
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
		String[] selectionArgs = {String.valueOf(playlistId)};
		Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + PlaylistTracks.TABLE + " WHERE " + PlaylistTracks.PLAYLIST_ID + "=?;" , selectionArgs);
		cursor.moveToFirst();
		int songs = cursor.getInt(0);
		cursor.close();

		ContentValues values = new ContentValues(1);
		values.put(Playlists.NUMBER_OF_SONGS, songs);

		db.update(Playlists.TABLE, values, Playlists._ID + " = ?", selectionArgs);
	}

	// PlaylistTrack

	// ACTION_ADD_TO_PLAYLIST
	private void addPlaylistTracks(Playlist playlist, List<Long> trackIds)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			MusicDB mdb = new MusicDB(db);
			
			int i = mdb.getPlaylistTracks(playlist.id).size();
			for(Long id : trackIds){
				Track track = mdb.getTrack(id);
				PlaylistTrack playlistTrack = new PlaylistTrack(null);
				playlistTrack.setTrackInfo(track);
				playlistTrack.trackId = track.id;
				playlistTrack.playlistId = playlist.id;
				playlistTrack.playlistTrackNo = i;

				mdb.insertPlaylistTrack(playlistTrack);

				i++;
			}

			updateNumSongsInPlaylist(db, playlist.id);

			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		
		showToast(R.string.msg_added_to_playlist);
	}

	// ACTION_UPDATE_PLAYLISTTRACKS
	private void updatePlaylistTracks(Playlist playlist, List<PlaylistTrack> ptracks){

		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			String stmt =
				"UPDATE " + PlaylistTracks.TABLE + " SET " +
					PlaylistTracks.PLAYLIST_TRACK_NO + " = ?" +
					" WHERE " + PlaylistTracks._ID + " = ? AND " + PlaylistTracks.PLAYLIST_ID + " = ?;";

			SQLiteStatement updateStmt = db.compileStatement(stmt);

			stmt =
				"DELETE FROM " + PlaylistTracks.TABLE +
					" WHERE " + PlaylistTracks._ID + " = ? AND " + PlaylistTracks.PLAYLIST_ID + " = ?;";

			SQLiteStatement deleteStmt = db.compileStatement(stmt);

			try{
				for(PlaylistTrack ptrack : ptracks){
					if(ptrack.playlistTrackNo >= 0){		// 更新
						updateStmt.bindLong(1, ptrack.playlistTrackNo);
						updateStmt.bindLong(2, ptrack.id);
						updateStmt.bindLong(3, playlist.id);
						updateStmt.executeUpdateDelete();
					}else{									// 削除
						deleteStmt.bindLong(1, ptrack.id);
						deleteStmt.bindLong(2, playlist.id);
						deleteStmt.executeUpdateDelete();
					}
				}
			}finally{
				updateStmt.close();
				deleteStmt.close();
			}

			updateNumSongsInPlaylist(db, playlist.id);

			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}

	// ACTION_DELETE_PLAYLISTTRACKS
	private void deletePlaylistTracks(Playlist playlist, List<Long> ptrackIds)
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForWriting().getWritableDatabase();
		db.beginTransactionNonExclusive();
		try{
			String stmt =
				"DELETE FROM " + PlaylistTracks.TABLE +
				" WHERE " + PlaylistTracks._ID + " = ? AND " + PlaylistTracks.PLAYLIST_ID + " = ?;";

			SQLiteStatement deleteStmt = db.compileStatement(stmt);

			try{
				for(Long id : ptrackIds){
					deleteStmt.bindLong(1, id);
					deleteStmt.bindLong(2, playlist.id);
					deleteStmt.executeUpdateDelete();
				}
			}finally{
				deleteStmt.close();
			}

			updateNumSongsInPlaylist(db, playlist.id);

			// Playlist TrackNo を更新
			List<PlaylistTrack> ptracks = playlist.getPlaylistTracks();
			for(int i = 0; i < ptracks.size(); i++){
				PlaylistTrack ptrack = ptracks.get(i);
				if(ptrack.playlistTrackNo != i){
					ContentValues values = new ContentValues(1);
					values.put(PlaylistTracks.PLAYLIST_TRACK_NO, i);

					String[] selectionArgs = { String.valueOf(ptrack.id) };
					db.update(PlaylistTracks.TABLE, values, PlaylistTracks._ID + " = ?", selectionArgs);
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

	// 通知
	private void postEvent(Object event)
	{
		EventBus.getDefault().post(event);
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

			if(cursor != null){
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
			}
			return tracks;
		}
	}

	public static class MediaScanningEvent
	{
		public String title;
		public String rootPath;
		public int percent;
	}

	public static class DatabaseChangedEvent
	{
	}

	public static class PlaylistChangedEvent
	{
	}
	
}
