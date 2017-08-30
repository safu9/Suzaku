package com.citrus.suzaku.base;

import android.content.Context;
import android.content.Intent;
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

import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.playlist.PlaylistSelectDialog;
import com.citrus.suzaku.track.TrackDetailActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public abstract class BaseListFragment<T> extends Fragment implements ActionMode.Callback, LoaderManager.LoaderCallbacks<List<T>>
{
	protected static final String LOADER_ID = "LOADER_ID";
	
	private int mLoaderId;
	private ActionMode mActionMode;

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
				if(mActionMode == null){
					onListItemClick((ListView)parent, view, position, id);
				}else{
					if(mListView.getCheckedItemCount() > 0){
						onItemCheckedStateChanged(mActionMode);
					}else{
						mActionMode.finish();
					}
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

				if(mListView.getCheckedItemCount() > 0){
					onItemCheckedStateChanged(mActionMode);
				}else{
					mActionMode.finish();
				}

				return true;
			}
		});

		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mEmptyView);
	}

	@Override
	public void onStart()
	{
		super.onStart();		
		startLoader(true);
		EventBus.getDefault().register(this);
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
		EventBus.getDefault().unregister(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.DatabaseChangedEvent event)
	{
		startLoader(true);
	}

	protected void onListItemClick(ListView l, View v, int position, long id)
	{
	}

	protected ListView getListView()
	{
		return mListView;
	}

	protected void setListAdapter(BaseAdapter adapter)
	{
		mAdapter = adapter;
		if(mListView != null){
			mListView.setAdapter(adapter);
		}
	}

	protected BaseAdapter getListAdapter()
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
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.menu_select_all:
				int count = getListAdapter().getCount();
				if(getListView().getCheckedItemCount() != count){
					for(int i = 0; i < count; i++){
						getListView().setItemChecked(i, true);
					}
					onItemCheckedStateChanged(mActionMode);
				}else{
					resetChoiceMode();
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

	private void onItemCheckedStateChanged(ActionMode mode)
	{
		if(mode != null){
			mode.setTitle(mListView.getCheckedItemCount() + " / " + mListView.getCount());
		}
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
	
}
