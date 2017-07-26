package com.citrus.suzaku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public abstract class BaseListFragment<T> extends Fragment implements ActionMode.Callback, LoaderManager.LoaderCallbacks<List<T>>
{
	protected static final String LOADER_ID = "LOADER_ID";
	
	private int mLoaderId;
	private ActionMode mActionMode;

	private DatabaseBroadcastReceiver receiver;

	private ListView mListView;
	private BaseAdapter mAdapter;
	private TextView mEmptyView;


	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		mLoaderId = getArguments().getInt(LOADER_ID, 0);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_listview, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		View view = getView();
		mListView = (ListView)view.findViewById(android.R.id.list);
		mEmptyView = (TextView)view.findViewById(android.R.id.empty);

//		listView.setMultiChoiceModeListener(this);

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if(mActionMode != null){
					if(mListView.getCheckedItemCount() == 0){
						mActionMode.finish();
					}else{
						onItemCheckedStateChanged(mActionMode, position, id, mListView.isItemChecked(position));
					}
				}else{
					onListItemClick((ListView)parent, view, position, id);
				}
			}
		});

		mListView.setLongClickable(true);
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				if(mActionMode == null){
					((MainActivity)getActivity()).startSupportActionMode(BaseListFragment.this);
				}

				boolean checked = mListView.isItemChecked(position);
				((ListView)parent).setItemChecked(position, !checked);

				if(mListView.getCheckedItemCount() == 0){
					mActionMode.finish();
				}else{
					onItemCheckedStateChanged(mActionMode, position, id, !checked);
				}

				return true;
			}
		});

		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mEmptyView);

		receiver = new DatabaseBroadcastReceiver();
	}

	@Override
	public void onStart()
	{
		super.onStart();		
		startLoader(true);
		getActivity().registerReceiver(receiver, receiver.getIntentFilter());
	}

	@Override
	public void onPause()
	{
		super.onPause();
		resetChoiceMode();
	}

	@Override
	public void onStop()
	{
		super.onStop();
		getActivity().unregisterReceiver(receiver);
	}

	public void onListItemClick(ListView l, View v, int position, long id)
	{
	}

	public ListView getListView()
	{
		return mListView;
	}

	public void setListAdapter(BaseAdapter adapter)
	{
		mAdapter = adapter;
		if(mListView != null){
			mListView.setAdapter(adapter);
		}
	}

	public BaseAdapter getListAdapter()
	{
		return mAdapter;
	}

	// ActionMode

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		mActionMode = mode;
		mode.getMenuInflater().inflate(R.menu.menu_fragment_choicemode, menu);

		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		return false;
	}


	private void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		if(mode != null){
			mode.setTitle(getListView().getCheckedItemCount() + " / " + getListView().getCount());
		}
		
		View v = getListView().getChildAt(position);
		if(v != null){
			v.setActivated(checked);
		}
	}
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.menu_select_all:
				int count = getListAdapter().getCount();
				for(int i = 0; i < count; i++){
					getListView().setItemChecked(i, true);			// called onItemCheckedStateChanged()
				}
				
				break;
				
			case R.id.menu_add_to_playlist:
				PlaylistSelectDialog dialog = PlaylistSelectDialog.newInstance(getCheckedTrackIds());
				dialog.setTargetFragment(this, 0);
				dialog.show(getChildFragmentManager(), "PlaylistSelectDialog");
				
				resetChoiceMode();
				break;

			case R.id.menu_add_to_queue:
				break;
				
			case R.id.menu_detail:
				Intent intent = new Intent(getActivity(), TrackDetailActivity.class);
				intent.putExtra("IDS", (Serializable)getCheckedTrackIds());
				startActivity(intent);
				break;
				
			default:
				return false;
		}
		
		return true;
	}
	
	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		mActionMode = null;

		SparseBooleanArray positions = mListView.getCheckedItemPositions();
		int count = mAdapter.getCount();

		for(int i = 0; i < count; i++){
			if(positions.get(i)){
				View v = getListView().getChildAt(i);
				if(v != null){
					v.setActivated(false);
				}
			}
		}

		mListView.clearChoices();
		mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);

		mAdapter.notifyDataSetChanged();
	}
	
	public void resetChoiceMode()
	{
		if(mActionMode != null){
			mActionMode.finish();
		}
	}
	
	protected List<T> getCheckedItems()
	{
		SparseBooleanArray positions = getListView().getCheckedItemPositions();
		List<T> items = new ArrayList<>();
		List<T> allItems = ((BaseListAdapter<T>)getListAdapter()).getDataList();

		int count = getListAdapter().getCount();
		for(int i = 0; i < count; i++){
			if(positions.get(i)){
				items.add(allItems.get(i));
			}
		}
		
		return items;
	}

	
	// Convert List<T> (Track or TrackGroup) to List<Long> (ID)
	protected abstract List<Long> getCheckedTrackIds();

	// Run in AsyncTaskLoader
	protected abstract List<T> getDataList();
	
	protected void startLoader(boolean restart)
	{
		mEmptyView.setVisibility(View.GONE);
		
		String status = Environment.getExternalStorageState();
		if(!status.equals(Environment.MEDIA_MOUNTED)){
			BaseListAdapter<T> adapter = (BaseListAdapter<T>)getListAdapter();

			if(adapter == null){
				return;
			}
			
			adapter.setDataList(null);
			adapter.notifyDataSetChanged();
			
			mEmptyView.setText(R.string.sd_unmounted);
			mEmptyView.setVisibility(View.VISIBLE);
			return;
		}
		
		if(!restart){
			getActivity().getSupportLoaderManager().initLoader(mLoaderId, null, this);
		}else{
			getActivity().getSupportLoaderManager().restartLoader(mLoaderId, null, this);
		}
	}

	@Override
	public Loader<List<T>> onCreateLoader(int id, Bundle args)
	{
		return new DataListLoader<>(getActivity(), this);
	}

	@Override
	public void onLoadFinished(Loader<List<T>> loader, List<T> list)
	{
		BaseListAdapter<T> adapter = (BaseListAdapter<T>)getListAdapter();
		
		if(adapter == null){
			return;
		}
		
		adapter.setDataList(list);
		adapter.notifyDataSetChanged();
		
		if(list == null || list.size() == 0){
			mEmptyView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<T>> loader)
	{
		//
	}
	
	// AsyncTaskLoader
	private static class DataListLoader<T> extends BaseAsyncTaskLoader<List<T>>
	{
		private BaseListFragment<T> fragment;
		
		public DataListLoader(Context context, BaseListFragment<T> fragment)
		{
			super(context);
			
			this.fragment = fragment;
		}
		
		@Override
		public List<T> loadInBackground()
		{
			if(fragment == null){
				return null;
			}

			List<T> list = fragment.getDataList();

			setResult(list);
			return list;
		}
		
		@Override
		protected void onReset()
		{
			super.onReset();
			fragment = null;
		}
	}

	
	// BroadcastReceiver
	private class DatabaseBroadcastReceiver extends BroadcastReceiver
	{
		private IntentFilter filter;

		public DatabaseBroadcastReceiver()
		{
			filter = new IntentFilter();
			filter.addAction(MusicDBService.ACTION_DATABASE_CHANGED);
		}

		@Override
		public void onReceive(Context c, Intent i)
		{
			startLoader(true);
		}

		public IntentFilter getIntentFilter()
		{
			return filter;
		}
	}
	
}
