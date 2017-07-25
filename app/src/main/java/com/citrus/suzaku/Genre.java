package com.citrus.suzaku;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.citrus.suzaku.MusicDB.Albums;
import com.citrus.suzaku.MusicDB.Genres;
import com.citrus.suzaku.MusicDB.Tracks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Genre extends TrackGroup implements Serializable
{
	private static final long serialVersionUID = 5L;

	public long id;
	public String genre;


	public Genre(Cursor cursor)
	{
		if(cursor == null)
			return;

		id = cursor.getLong(cursor.getColumnIndex(Genres._ID));
		genre = cursor.getString(cursor.getColumnIndex(Genres.GENRE));
	}
	
	@Override
	public List<Track> getTracks()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { genre };

		return mdb.getTracks(Tracks.GENRE + "= ?", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
	}

	@Override
	public List<Long> getTrackIds()
	{
		MusicDB mdb = new MusicDB();
		String[] selectionArgs = { genre };

		return mdb.getTrackIds(Tracks.GENRE + "= ?", selectionArgs, Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);
	}

	public List<Album> getAlbums()
	{
		SQLiteDatabase db = MusicDBHelper.getInstanceForReading().getReadableDatabase();

		String stmt =
			"SELECT " + 
			"t1." + Tracks.ALBUM_ID + " AS " + Albums._ID + ",t2." + Tracks.ALBUM + ",t2." + Albums.ALBUM_SORT + ",t2." + Albums.ARTIST +
			",t2." + Tracks.ARTIST_ID + ",t2." + Albums.ARTWORK_HASH + ",t2." + Albums.YEAR + ",t1." + Albums.NUMBER_OF_SONGS + ",t2." + Albums.COMPILATION +
			" FROM (" +
				"SELECT " + Tracks.ALBUM_ID + ", COUNT(*) AS " + Albums.NUMBER_OF_SONGS +
				" FROM " + Tracks.TABLE + " WHERE " + Tracks.GENRE + " = ? GROUP BY " + Tracks.ALBUM_ID +
			") t1" +
			" INNER JOIN " + Albums.TABLE + " t2 ON t1." + Tracks.ALBUM_ID + " = t2." + Albums._ID +
			" ORDER BY t2." + Albums.ALBUM_SORT + MusicDB.COLLATE_LOCALIZED + ";";

		Cursor cursor = db.rawQuery(stmt, new String[]{ genre });

		List<Album> albums = new ArrayList<>();

		while(cursor.moveToNext()){
			albums.add(new Album(cursor));
		}

		cursor.close();

		return albums;
	}
	
	public ContentValues getContentValues()
	{
		ContentValues values = new ContentValues(2);
		
		values.put(Genres._ID, id);
		values.put(Genres.GENRE, genre);
		
		return values;
	}

	// For Displaying String 

	public String getGenreString(){
		return (!genre.equals(MusicDB._NULL))? genre : App.getContext().getString(R.string.unknown_genre);
	}

}
