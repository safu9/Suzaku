package com.citrus.suzaku.artist;

import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.album.Album;

import java.util.*;


public class ArtistCompilation extends Artist
{
	private static final long serialVersionUID = 21L;

	public ArtistCompilation()
	{
		super(null);
	}

	@Override
	public List<Track> getTracks()
	{
		MusicDB mdb = new MusicDB();
		return mdb.getTracks(MusicDB.Tracks.COMPILATION + " = 1", null, MusicDB.Tracks.TITLE_SORT + MusicDB.COLLATE_LOCALIZED);

	}
	
	@Override
	public List<Album> getAlbums()
	{
		MusicDB mdb = new MusicDB();
		return mdb.getAlbums(MusicDB.Albums.COMPILATION + " = 1", null, MusicDB.Albums.ALBUM_SORT + MusicDB.COLLATE_LOCALIZED);
	}

}
