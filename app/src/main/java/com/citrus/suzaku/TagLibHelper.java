package com.citrus.suzaku;
//
// Created by safu9 on 2017/05/17

public class TagLibHelper
{
    static{
        System.loadLibrary("tag");
        System.loadLibrary("taglib_wrapper");
    }

    @SuppressWarnings("unused")
    private long fileRefHandle;
    @SuppressWarnings("unused")
    private long tagHandle;
    @SuppressWarnings("unused")
    private long tagMapHandle;

    public native void setFile(String path);
    public native void dumpTags();

    // Read

    public static final int NUMBER_OF_KEY = 17;

    public static final int KEY_TITLE = 0;
    public static final int KEY_TITLESORT = 1;
    public static final int KEY_ARTIST = 2;
    public static final int KEY_ARTISTSORT = 3;
    public static final int KEY_ALBUM = 4;
    public static final int KEY_ALBUMSORT = 5;
    public static final int KEY_ALBUMARTIST = 6;
    public static final int KEY_ALBUMARTISTSORT = 7;
    public static final int KEY_GENRE = 8;
    public static final int KEY_COMPOSER = 9;
    public static final int KEY_YEAR = 10;
    public static final int KEY_LYRICS = 11;
    public static final int KEY_COMMENT = 12;
    public static final int KEY_GROUP = 13;
    public static final int KEY_TRACKNUMBER = 14;
    public static final int KEY_DISCNUMBER = 15;
    public static final int KEY_COMPILATION = 16;


    public native String getTag(int key);           // ちょっとだけ遅い

    public native String getTitle();
    public native String getTitleSort();
    public native String getArtist();
    public native String getArtistSort();
    public native String getAlbum();
    public native String getAlbumSort();
    public native String getAlbumArtist();
    public native String getAlbumArtistSort();
    public native String getGenre();
    public native String getComposer();
    public native int getYear();
    public native String getLyrics();
    public native String getComment();
    public native String getGroup();
    public native int getTrackNumber();
    public native int getDiscNumber();
    public native boolean getCompilation();

    public native byte[] getArtwork();

    public native int getLength();
    public native int getBitrate();
    public native int getSampleRate();
    public native int getChannels();

    // Write

    public native void setTag(int key, String value);
    public native boolean saveTag();

    public native void setTitle(String title);
    public native void setTitleSort(String titleSort);
    public native void setArtist(String artist);
    public native void setArtistSort(String artistSort);
    public native void setAlbum(String Album);
    public native void setAlbumSort(String albumSort);
    public native void setAlbumArtist(String albumArtist);
    public native void setAlbumArtistSort(String albumArtistSort);
    public native void setGenre(String genre);
    public native void setComposer(String composer);
    public native void setYear(int year);
    public native void setLyrics(String lyrics);
    public native void setComment(String comment);
    public native void setGroup(String group);
    public native void setTrackNumber(int trackNum);
    public native void setDiscNumber(int discNum, int discCount);
    public native void setCompilation(boolean compilation);

    public native void setArtwork(byte[] artwork, String mime);
    public native void deleteArtwork();

    public native void release();
}
