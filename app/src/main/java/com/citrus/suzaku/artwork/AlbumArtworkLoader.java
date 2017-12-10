package com.citrus.suzaku.artwork;
//
// Created by safu9 on 2017/12/02

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.citrus.suzaku.album.Album;

import java.nio.ByteBuffer;

public final class AlbumArtworkLoader implements ModelLoader<Album, ByteBuffer>
{
	@Override
	public LoadData<ByteBuffer> buildLoadData(Album album, int width, int height, Options options)
	{
		return new LoadData<>(new ArtworkKey(album.artworkHash), new ArtworkFetcher(album.getTracks().get(0).path));
	}

	@Override
	public boolean handles(Album file)
	{
		return true;
	}

	static class Factory implements ModelLoaderFactory<Album, ByteBuffer>
	{
		@Override
		public ModelLoader<Album, ByteBuffer> build(MultiModelLoaderFactory multiFactory)
		{
			return new AlbumArtworkLoader();
		}

		@Override public void teardown() {
		}
	}
}
