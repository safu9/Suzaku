package com.citrus.suzaku.artwork;
//
// Created by safu9 on 2017/12/02

import com.bumptech.glide.load.Key;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class ArtworkKey implements Key
{
	private String mHash;

	public ArtworkKey(String hash)
	{
		mHash = hash;
	}

	@Override
	public void updateDiskCacheKey(MessageDigest messageDigest)
	{
		if(messageDigest != null){
			byte[] key = ByteBuffer.allocate(Integer.SIZE).putInt(hashCode()).array();
			messageDigest.update(key);
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ArtworkKey) {
			String hash2 = ((ArtworkKey)obj).mHash;
			return mHash != null && mHash.equals(hash2);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return (mHash != null)? mHash.hashCode() : 0;
	}
}
