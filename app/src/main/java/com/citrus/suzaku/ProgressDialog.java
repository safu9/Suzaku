package com.citrus.suzaku;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


// Select Playlist and Add Tracks
public class ProgressDialog extends DialogFragment
{
//	private ProgressBar mProgressBar;
	private TextView mMessageView;

	public static ProgressDialog newInstance(String title)
	{
		ProgressDialog dialog = new ProgressDialog();
		Bundle args = new Bundle();
		args.putString("TITLE", title);
		dialog.setArguments(args);

		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_progress, null, false);

//		mProgressBar = (ProgressBar)view.findViewById(R.id.progress);
		mMessageView = (TextView)view.findViewById(R.id.message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getArguments().getString("TITLE"));
		builder.setView(view);

		return builder.create();
	}

	public void updateMessage(String message)
	{
		if(mMessageView != null){
			mMessageView.setText(message);
		}
	}
}
