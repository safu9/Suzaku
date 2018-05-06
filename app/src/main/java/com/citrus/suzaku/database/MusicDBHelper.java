package com.citrus.suzaku.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.citrus.suzaku.App;
import com.citrus.suzaku.database.MusicDB.Albums;
import com.citrus.suzaku.database.MusicDB.Artists;
import com.citrus.suzaku.database.MusicDB.Genres;
import com.citrus.suzaku.database.MusicDB.PlaylistTracks;
import com.citrus.suzaku.database.MusicDB.Playlists;
import com.citrus.suzaku.database.MusicDB.Tracks;


// SQLiteOpenHelper
public class MusicDBHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "music.db";
	private static final int DATABASE_VERSION = 6;

	// Singleton ...

	private static MusicDBHelper readInstance = null;

	public static synchronized MusicDBHelper getInstanceForReading()
	{
		if(readInstance == null){
			readInstance = new MusicDBHelper(App.getContext());
		}
		return readInstance;
	}
	
	private static MusicDBHelper writeInstance = null;

	public static synchronized MusicDBHelper getInstanceForWriting()
	{
		if(writeInstance == null){
			writeInstance = new MusicDBHelper(App.getContext());
		}
		return writeInstance;
	}
	
	
	private MusicDBHelper(Context context)
	{
		super(context, context.getExternalFilesDir(null)  + "/" + DATABASE_NAME, null, DATABASE_VERSION);
	}

/*	// 初回設定
	@Override
	public void onConfigure(SQLiteDatabase db)
	{
		super.onConfigure(db);
		db.setLocale(Locale.getDefault());
	}
*/
	// DB が無いとき Table 作成
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		createTrackTable(db);
		createAlbumTable(db);
		createArtistTable(db);
		createGenreTable(db);
		createPlaylistTable(db);

		createAlbumView(db);
		createTrackView(db);
	}

	// 開くときにアップグレード
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Cursor cursor = db.rawQuery("SELECT tbl_name FROM sqlite_master WHERE type='table'", null);
		
		while(cursor.moveToNext()){
			db.execSQL("DROP TABLE " + cursor.getString(cursor.getColumnIndex("tbl_name")) + ";");
		}
		cursor.close();
		
		onCreate(db);
	}
	
	
	private static void createTrackTable(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + Tracks.TABLE + " (" + 
				   Tracks._ID + " INTEGER PRIMARY KEY," +
				   Tracks.PATH + " TEXT UNIQUE NOT NULL," +
				   Tracks.TITLE + " TEXT NOT NULL," +
				   Tracks.TITLE_SORT + " TEXT," +
				   Tracks.ARTIST_ID + " INTEGER," +
				   Tracks.ALBUM_ID + " INTEGER," +
				   Tracks.ARTWORK_HASH + " TEXT," +
				   Tracks.COMPOSER + " TEXT," +
				   Tracks.GENRE + " TEXT," +
				   Tracks.TRACK_NO + " INTEGER," +
				   Tracks.DISC_NO + " INTEGER," +
				   Tracks.DURATION + " INTEGER," +
				   Tracks.YEAR + " INTEGER," +
				   Tracks.COMPILATION + " INTEGER," +
				   Tracks.FILE_LAST_MODIFIED + " INTEGER NOT NULL DEFAULT 0," +
				   Tracks.FLAG + " INTEGER);");
	}
	
	private static void createAlbumTable(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + Albums.TABLE + " (" + 
				   Albums._ID + " INTEGER PRIMARY KEY," +
				   Albums.ALBUM + " TEXT UNIQUE," +
				   Albums.ALBUM_SORT + " TEXT," +
				   Albums.ARTWORK_HASH + " TEXT," +
				   Albums.ARTIST_ID + " INTEGER," +
				   Albums.NUMBER_OF_SONGS + " INTEGER," +
				   Albums.YEAR + " INTEGER," +
				   Albums.COMPILATION + " INTEGER NOT NULL DEFAULT 0);");
	}
	
	private static void createArtistTable(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + Artists.TABLE + " (" + 
				   Artists._ID + " INTEGER PRIMARY KEY," +
				   Artists.ARTIST + " TEXT UNIQUE," +
				   Artists.ARTIST_SORT + " TEXT," +
				   Artists.NUMBER_OF_ALBUMS + " INTEGER," +
				   Artists.NUMBER_OF_SONGS + " INTEGER);");
	}
	
	private static void createGenreTable(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + Genres.TABLE + " (" + 
				   Genres._ID + " INTEGER PRIMARY KEY," +
				   Genres.GENRE + " TEXT UNIQUE);");
	}
	
	private static void createPlaylistTable(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + Playlists.TABLE + " (" + 
				   Playlists._ID + " INTEGER PRIMARY KEY," +
				   Playlists.TITLE + " TEXT," +
				   Playlists.NUMBER_OF_SONGS + " INTEGER);");
	}
	
	
	public static void createPlaylistTrackTable(SQLiteDatabase db, long playlistId)
	{
		db.execSQL("CREATE TABLE IF NOT EXISTS " + PlaylistTracks.TABLE + String.valueOf(playlistId) + " (" + 
				   PlaylistTracks._ID + " INTEGER PRIMARY KEY," +
				   PlaylistTracks.TRACK_ID + " INTEGER," +
				   PlaylistTracks.PATH + " TEXT NOT NULL," +
				   PlaylistTracks.TITLE + " TEXT NOT NULL," +
				   PlaylistTracks.TITLE_SORT + " TEXT," +
				   PlaylistTracks.ARTIST + " TEXT," +
				   PlaylistTracks.ARTIST_ID + " INTEGER," +
				   PlaylistTracks.ARTIST_SORT + " TEXT," +
				   PlaylistTracks.ALBUM + " TEXT," +
				   PlaylistTracks.ALBUM_ID + " INTEGER," +
				   PlaylistTracks.ALBUM_SORT + " TEXT," +
				   PlaylistTracks.ALBUMARTIST_ID + " INTEGER," +
				   PlaylistTracks.ALBUMARTIST + " TEXT," +
				   PlaylistTracks.ALBUMARTIST_SORT + " TEXT," +
				   PlaylistTracks.ARTWORK_HASH + " TEXT," +
				   PlaylistTracks.COMPOSER + " TEXT," +
				   PlaylistTracks.GENRE + " TEXT," +
				   PlaylistTracks.TRACK_NO + " INTEGER," +
				   PlaylistTracks.DISC_NO + " INTEGER," +
				   PlaylistTracks.DURATION + " INTEGER," +
				   PlaylistTracks.YEAR + " INTEGER," +
				   PlaylistTracks.COMPILATION + " INTEGER," +
				   PlaylistTracks.FILE_LAST_MODIFIED + " INTEGER NOT NULL DEFAULT 0," +
				   PlaylistTracks.PLAYLIST_TRACK_NO + " INTEGER);");
	}

	private static void createAlbumView(SQLiteDatabase db)
	{
		db.execSQL("CREATE VIEW " + Albums.VIEW +
				" AS SELECT " +
				"al." + Albums._ID + "," +
				"al." + Albums.ALBUM + "," +
				"al." + Albums.ALBUM_SORT + "," +
				"al." + Albums.ARTWORK_HASH + "," +

				"ar." + Artists._ID + " AS " + Albums.ARTIST_ID + "," +
				"ar." + Artists.ARTIST + "," +
				"ar." + Artists.ARTIST_SORT + "," +

				"al." + Albums.NUMBER_OF_SONGS + "," +
				"al." + Albums.YEAR + "," +
				"al." + Albums.COMPILATION +

				" FROM " + Albums.TABLE + " al " +
				" LEFT JOIN " + Artists.TABLE + " ar " +
				" ON al." + Albums.ARTIST_ID + " = ar." + Artists._ID
		);
	}

	private static void createTrackView(SQLiteDatabase db)
	{
		db.execSQL("CREATE VIEW " + Tracks.VIEW +
				" AS SELECT " +
				"t." + Tracks._ID + "," +
				"t." + Tracks.PATH + "," +
				"t." + Tracks.TITLE + "," +
				"t." + Tracks.TITLE_SORT + "," +

				"al." + Albums._ID + " AS " + Tracks.ALBUM_ID + "," +
				"al." + Albums.ALBUM + "," +
				"al." + Albums.ALBUM_SORT + "," +
				"al." + Albums.ARTIST_ID + " AS " + Tracks.ALBUMARTIST_ID + "," +
				"al." + Albums.ARTIST + " AS " + Tracks.ALBUMARTIST + "," +
				"al." + Albums.ARTIST_SORT + " AS " + Tracks.ALBUMARTIST_SORT + "," +

				"ar." + Artists._ID + " AS " + Tracks.ARTIST_ID + "," +
				"ar." + Tracks.ARTIST + "," +
				"ar." + Tracks.ARTIST_SORT + "," +

				"t." + Tracks.ARTWORK_HASH + "," +
				"t." + Tracks.COMPOSER + "," +
				"t." + Tracks.GENRE + "," +
				"t." + Tracks.TRACK_NO + "," +
				"t." + Tracks.DISC_NO + "," +
				"t." + Tracks.DURATION + "," +
				"t." + Tracks.YEAR + "," +
				"t." + Tracks.COMPILATION + "," +
				"t." + Tracks.FILE_LAST_MODIFIED + "," +
				"t." + Tracks.FLAG +

				" FROM " + Tracks.TABLE + " t " +
				" LEFT JOIN " + Albums.VIEW + " al " +
				" ON t." + Tracks.ALBUM_ID + " = al." + Albums._ID +
				" LEFT JOIN " + Artists.TABLE + " ar " +
				" ON t." + Tracks.ARTIST_ID + " = ar." + Artists._ID
		);
	}
}
	
	
