package com.citrus.suzaku;
//
// Created by safu9 on 2017/06/16

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibraryFolderSelectDialog extends DialogFragment implements AdapterView.OnItemClickListener, View.OnClickListener
{
	private View view;

	private ListView listView;
	private FileArrayAdapter adapter;

	private List<String> storagePaths;
	private List<File> storageNames;
	private File dir;


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.dialog_library_folder, null);

		Button okButton = (Button)view.findViewById(R.id.okButton);
		Button cancelButton = (Button)view.findViewById(R.id.cancelButton);

		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		listView = (ListView)view.findViewById(R.id.list);
		listView.setOnItemClickListener(this);

		adapter = new FileArrayAdapter();
		listView.setAdapter(adapter);

		storagePaths = getStoragePaths();
		storageNames = new ArrayList<>();
		storageNames.add(new File(getString(R.string.inner_storage)));
		for(int i = 1; i < storagePaths.size(); i++){
			storageNames.add(new File(getString(R.string.sd_card) + " " + i));
		}

		dir = null;
		if(storagePaths.size() == 1){
			dir = new File(storagePaths.get(0));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_folder);
		builder.setView(view);

		return builder.create();
	}

	@Override
	public void onStart()
	{
		super.onStart();
/*
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		lp.height = (int)(getContext().getResources().getDisplayMetrics().heightPixels * 0.9);
		view.setLayoutParams(lp);
*/
		updateList();
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(dir == null){				// root
			dir = new File(storagePaths.get(position));
		}else if(position == 0){		// up
			if(storagePaths.contains(dir.getAbsolutePath())){
				dir = null;
			}else{
				dir = dir.getParentFile();
			}
		}else{
			dir = adapter.getItem(position);
		}
		updateList();
	}

	@Override
	public void onClick(View v)
	{
		switch(v.getId()){
			case R.id.okButton:
				Intent result = new Intent();
				result.putExtra("PATH", dir.getAbsolutePath());

				if (getTargetFragment() != null) {
					// 呼び出し元がFragmentの場合
					getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, result);
				} else {
					// 呼び出し元がActivityの場合
					PendingIntent pi = getActivity().createPendingResult(getTargetRequestCode(), result,
							PendingIntent.FLAG_ONE_SHOT);
					try {
						pi.send(Activity.RESULT_OK);
					} catch (PendingIntent.CanceledException ex) {
						// send failed
					}
				}

				dismiss();

				break;

			case R.id.cancelButton:
				dismiss();
				break;
		}
	}

	private void updateList()
	{
		List<File> list;

		if(dir == null){        // root
			getDialog().setTitle(R.string.select_folder);                // onCreateDialogView で呼ぶとヌルポ
			list = storageNames;
		}else{
			getDialog().setTitle(dir.getAbsolutePath());                // onCreateDialogView で呼ぶとヌルポ

			File[] fileList = dir.listFiles(new FileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					return (file.isDirectory() && file.getName().charAt(0) != '.');
				}
			});

			list = new ArrayList<>();
			list.addAll(Arrays.asList(fileList));
			Collections.sort(list);

			list.add(0, new File(".."));
		}

		adapter.setDataList(list);
		adapter.notifyDataSetChanged();
	}

	private static List<String> getStoragePaths()
	{
		List<String> storagePaths = new ArrayList<>();

		// Inner Storage
		storagePaths.add(Environment.getExternalStorageDirectory().getAbsolutePath());

		// SD cards
		storagePaths.addAll(App.getSdCardFilesDirPathList());

		return storagePaths;
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
			holder.dirNameView.setText(file.getName());
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.dialog_listitem_dir, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.iconView = (ImageView)view.findViewById(R.id.icon);
			holder.dirNameView = (TextView)view.findViewById(R.id.dirName);

			view.setTag(holder);

			return view;
		}

		private static class ViewHolder
		{
			ImageView iconView;
			TextView dirNameView;
		}
	}

}
