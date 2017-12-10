package com.citrus.suzaku.artwork;
//
// Created by safu9 on 2017/12/02

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.citrus.suzaku.track.Track;

import java.nio.ByteBuffer;

public final class TrackArtworkLoader implements ModelLoader<Track, ByteBuffer>
{
	@Override
	public LoadData<ByteBuffer> buildLoadData(Track track, int width, int height, Options options)
	{
		return new LoadData<>(new ArtworkKey(track.artworkHash), new ArtworkFetcher(track.path));
	}

	@Override
	public boolean handles(Track track)
	{
		return true;
	}

	static class Factory implements ModelLoaderFactory<Track, ByteBuffer>
	{
		@Override
		public ModelLoader<Track, ByteBuffer> build(MultiModelLoaderFactory multiFactory)
		{
			return new TrackArtworkLoader();
		}

		@Override public void teardown() {
		}
	}
}
