package com.citrus.suzaku;
//
// Created by $USER_NAME on 2017/05/17

public class TagLibHelper
{
    static{
        System.loadLibrary("taglib_wrapper");
    }

    private long fileRefHandle;

    private long tagHandle;
    private long tagMapHandle;

    public native void setFile(String path_);
    public native void dumpTags();

    // Read

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
    public native void setDiscNumber(int discNum);
    public native void setCompilation(boolean compilation);

    public native void release();
}
