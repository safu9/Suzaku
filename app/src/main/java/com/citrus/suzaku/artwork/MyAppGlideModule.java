package com.citrus.suzaku.artwork;
//
// Created by safu9 on 2017/12/02

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.citrus.suzaku.album.Album;
import com.citrus.suzaku.track.Track;

import java.nio.ByteBuffer;

@GlideModule
public class MyAppGlideModule extends AppGlideModule
{
	@Override
	public void applyOptions(Context context, GlideBuilder builder) {
		builder.setLogLevel(Log.VERBOSE);
	}

	@Override
	public void registerComponents(Context context, Glide glide, Registry registry)
	{
		registry.append(Track.class, ByteBuffer.class, new TrackArtworkLoader.Factory());
		registry.append(Album.class, ByteBuffer.class, new AlbumArtworkLoader.Factory());
	}
}
