package com.citrus.suzaku;
//
// Created by safu9 on 2017/07/26

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class BaseAsyncTaskLoader<T> extends AsyncTaskLoader<T>
{
	private T result;

	public BaseAsyncTaskLoader(Context context)
	{
		super(context);
	}

	// Called at the last of loadInBackground
	public void setResult(T result)
	{
		this.result = result;
	}

	@Override
	protected void onStartLoading()
	{
		if(result != null){
			deliverResult(result);
		}else if(takeContentChanged() || result == null){
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading()
	{
		cancelLoad();
	}

	@Override
	public void deliverResult(T result)
	{
		this.result = result;

		if(isStarted()){
			super.deliverResult(result);
		}
	}

	@Override
	protected void onReset()
	{
		super.onReset();
		cancelLoad();
	}
}

