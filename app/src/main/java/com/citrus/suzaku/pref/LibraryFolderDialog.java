package com.citrus.suzaku.pref;
//
// Created by safu9 on 2017/06/12

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.base.BaseListAdapter;

import java.util.List;

// ライブラリフォルダの一覧を表示
public class LibraryFolderDialog extends PreferenceDialogFragmentCompat implements ListView.OnItemClickListener, View.OnClickListener
{
    private List<String> paths;
    private FolderListAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LibraryFolderPreference pref = (LibraryFolderPreference)getPreference();
        paths = pref.getStringListValue();

        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_library_folder, null, false);

        Button okButton = view.findViewById(R.id.ok_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        adapter = new FolderListAdapter();
        adapter.setDataList(paths);

        View footer = inflater.inflate(R.layout.listitem_text, null, false);
        TextView textView = footer.findViewById(R.id.title_view);
        textView.setText(R.string.add_folder);

        ListView listView = view.findViewById(R.id.list);
        listView.addFooterView(footer);
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_folder);
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void onDialogClosed(boolean positiveResult)
    {
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id)
    {
        if(paths == null || position == paths.size()){					// Footer
            LibraryFolderSelectDialog dialog = new LibraryFolderSelectDialog();
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), "LibraryFolderSelectDialog");
        }
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId()){
            case R.id.ok_button:
                LibraryFolderPreference pref = (LibraryFolderPreference)getPreference();
            //    if (pref.callChangeListener(value)){
                    pref.persistStringListValue(paths);
            //    }

                dismiss();
                break;

            case R.id.cancel_button:
                dismiss();
                break;
        }
    }

    // Called by LibraryFolderSelectDialog
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_OK){
            String path = data.getStringExtra("PATH");
            paths.add(path);

            adapter.notifyDataSetChanged();
        }
    }


    // Adapter
    private class FolderListAdapter extends BaseListAdapter<String>
    {
        private final LayoutInflater inflater;


        public FolderListAdapter()
        {
            Context context = App.getContext();
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public void bindView(int position, View view)
        {
            ViewHolder holder = (ViewHolder)view.getTag();

            String path = getItem(position);

            holder.dirNameTextView.setText(path);
            holder.clearButton.setTag(position);
        }

        @Override
        public View newView(ViewGroup parent)
        {
            View view = inflater.inflate(R.layout.dialog_listitem_lib_folder, parent, false);

            ViewHolder holder = new ViewHolder();

            holder.dirNameTextView = view.findViewById(R.id.name_view);
            holder.clearButton = view.findViewById(R.id.clear_button);

            holder.clearButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int position = (int)v.getTag();
                    paths.remove(position);

                    notifyDataSetChanged();
                }
            });

            view.setTag(holder);

            return view;
        }
    }

    private static class ViewHolder
    {
        TextView dirNameTextView;
        ImageButton clearButton;
    }

}
