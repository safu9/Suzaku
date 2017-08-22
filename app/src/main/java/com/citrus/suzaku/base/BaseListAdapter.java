package com.citrus.suzaku.base;

import android.view.*;
import android.widget.*;
import java.util.*;

public abstract class BaseListAdapter<T> extends BaseAdapter
{
	private List<T> list;
	
	
	public void setDataList(List<T> list)
	{
		this.list = list;
	}
	
	public List<T> getDataList()
	{
		return list;
	}
	
	@Override
	public int getCount()
	{
		return (list != null)? list.size() : 0;
	}

	@Override
	public T getItem(int position)
	{
		return (list != null)? list.get(position) : null;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent)
	{
		if(view == null){
			view = newView(parent);
		}
		
		bindView(position, view);
		
		return view;
	}
	
	public abstract void bindView(int position, View view);

	public abstract View newView(ViewGroup parent);
	
}
