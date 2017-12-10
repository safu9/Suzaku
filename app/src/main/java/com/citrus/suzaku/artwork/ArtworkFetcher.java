package com.citrus.suzaku.artwork;
//
// Created by safu9 on 2017/12/02

import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.citrus.suzaku.TagLibHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class ArtworkFetcher implements DataFetcher<ByteBuffer>
{
	private String mPath;


	public ArtworkFetcher(String path)
	{
		mPath = path;
	}

	@Override
	public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback)
	{
		File file = new File(mPath);
		if(!file.exists()){
			callback.onLoadFailed(new FileNotFoundException());
			return;
		}

		TagLibHelper tag = new TagLibHelper();
		tag.setFile(mPath);
		byte[] bytes = tag.getArtwork();
		tag.release();

		ByteBuffer buf = (bytes != null)? ByteBuffer.wrap(bytes) : null;
		callback.onDataReady(buf);
	}

	@Override
	public void cleanup()
	{

	}

	@Override
	public void cancel()
	{

	}

	@NonNull
	@Override
	public Class<ByteBuffer> getDataClass()
	{
		return ByteBuffer.class;
	}

	@NonNull
	@Override
	public DataSource getDataSource()
	{
		return DataSource.REMOTE;
	}
}
