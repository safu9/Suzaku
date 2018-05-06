package com.citrus.suzaku.genre;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.album.Album;
import com.citrus.suzaku.base.TrackGroup;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDB.Albums;
import com.citrus.suzaku.database.MusicDB.Genres;
import com.citrus.suzaku.database.MusicDB.Tracks;
import com.citrus.suzaku.database.MusicDBHelper;
import com.citrus.suzaku.track.Track;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Genre extends TrackGroup implements Serializable
{
	private static final long serialVersionUID = 5000L;

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
			"t1." + Tracks.ALBUM_ID + " AS " + Albums._ID + "," + Albums.ALBUM + "," + Albums.ALBUM_SORT + "," + Albums.ARTIST_ID + "," + Albums.ARTIST +
			"," + Albums.ARTIST_SORT + "," + Albums.ARTWORK_HASH + "," + Albums.YEAR + ",t1." + Albums.NUMBER_OF_SONGS + " AS " + Albums.NUMBER_OF_SONGS + "," + Albums.COMPILATION +
			" FROM (" +
				"SELECT " + Tracks.ALBUM_ID + ", COUNT(*) AS " + Albums.NUMBER_OF_SONGS +
				" FROM " + Tracks.TABLE + " WHERE " + Tracks.GENRE + " = ? GROUP BY " + Tracks.ALBUM_ID +
			") t1" +
			" INNER JOIN " + Albums.VIEW + " t2 ON t1." + Tracks.ALBUM_ID + " = t2." + Albums._ID +
			" ORDER BY t2." + Albums.ALBUM_SORT + MusicDB.COLLATE_LOCALIZED + ";";

		Cursor cursor = db.rawQuery(stmt, new String[]{ genre });

		List<Album> albums = new ArrayList<>();

		while(cursor.moveToNext()){
			albums.add(new Album(cursor));
		}

		cursor.close();

		return albums;
	}

	// For Displaying String 

	public String getGenreString(){
		return (!genre.equals(MusicDB._NULL))? genre : App.getContext().getString(R.string.unknown_genre);
	}

}
