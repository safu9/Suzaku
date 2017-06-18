package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

//! DISABLED
public class FolderSelectPreference extends DialogPreference implements AdapterView.OnItemClickListener
{
	private static final String SD_PATH;
	static{
		SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	}
	
	private View view;
	
	private ListView listView;
	private FileArrayAdapter adapter;
	
	private File dir;
	
	
	public FolderSelectPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected View onCreateDialogView()
	{
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.dialog_select_folder, null);

		listView = (ListView)view.findViewById(R.id.list);
		listView.setOnItemClickListener(this);
		
		adapter = new FileArrayAdapter();
		listView.setAdapter(adapter);

		String path = getPersistedString(SD_PATH);
		dir = new File(path);
		
		// 存在しないとき
		if(!dir.exists()){
			persistString(SD_PATH);
			dir = new File(SD_PATH);
		}
		
		return view;
	}

	@Override
	protected void showDialog(Bundle state)
	{
		super.showDialog(state);
		
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		lp.height = (int)(getContext().getResources().getDisplayMetrics().heightPixels * 0.9);
		view.setLayoutParams(lp);
		
		updateList();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult){
			persistString(dir.getAbsolutePath());
		}
		
		super.onDialogClosed(positiveResult);
	}
	

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(position == 0 && !dir.getPath().equals(SD_PATH)){
			dir = dir.getParentFile();
			updateList();
		}else{
			dir = adapter.getItem(position);
			updateList();
		}
	}
	
	private void updateList()
	{
		getDialog().setTitle(dir.getAbsolutePath());				// onCreateDialogView で呼ぶとヌルポ
		
		File[] fileList = dir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file)
			{
				return (file.isDirectory() && file.getName().charAt(0) != '.');
			}
		});

		ArrayList<File> list = new ArrayList<>();
		list.addAll(Arrays.asList(fileList));
		Collections.sort(list);

		if(!dir.getPath().equals(SD_PATH)){
			list.add(0, new File(".."));
		}

		adapter.setDataList(list);
		adapter.notifyDataSetChanged();
	}
	

	private static class FileArrayAdapter extends BaseListAdapter<File>
	{
		private LayoutInflater inflater;
		

		public FileArrayAdapter()
		{
			inflater = (LayoutInflater)App.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();

			File file = getItem(position);

			int iconId;
			if(file.getName().equals("..")){		//一つ上へ
				iconId = R.drawable.ic_up;
			}else{									//フォルダ
				iconId = R.drawable.ic_dir;
			}

			holder.iconView.setImageResource(iconId);
			holder.folderNameView.setText(file.getName());
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.dialog_listitem_dir, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.iconView = (ImageView)view.findViewById(R.id.icon);
			holder.folderNameView = (TextView)view.findViewById(R.id.dirName);

			view.setTag(holder);
			
			return view;
		}
		
		private static class ViewHolder
		{
			ImageView iconView;
			TextView folderNameView;
		}
	}
	
}
