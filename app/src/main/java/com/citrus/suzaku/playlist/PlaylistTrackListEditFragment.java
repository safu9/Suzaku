package com.citrus.suzaku.playlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.track.Track;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//
// Created by safu9 on 2018/05/08

public class PlaylistTrackListEditFragment extends Fragment
{
	private static final String PLAYLIST = "PLAYLIST";

	private Playlist playlist;
	private List<PlaylistTrack> tracks;

	private int startIndexToChange;
	private List<PlaylistTrack> deletedTracks = new ArrayList<PlaylistTrack>();

	private RecyclerView recyclerView;
	private PlaylistTrackAdapter mAdapter;
	private ItemTouchHelper mTouchHelper;

	public static PlaylistTrackListEditFragment newInstance(Playlist playlist)
	{
		PlaylistTrackListEditFragment fragment = new PlaylistTrackListEditFragment();
		Bundle bundle = new Bundle();

		bundle.putSerializable(PLAYLIST, playlist);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		playlist = (Playlist)getArguments().getSerializable(PLAYLIST);

		View view = inflater.inflate(R.layout.fragmetnt_recyclerview, container, false);

		setHasOptionsMenu(true);

		recyclerView = view.findViewById(R.id.recycler_view);

		//! TENTATIVE
		tracks = playlist.getPlaylistTracks();
		startIndexToChange = tracks.size();

		mAdapter = new PlaylistTrackAdapter(tracks);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(mAdapter);

		mTouchHelper = new ItemTouchHelper(
			new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
				@Override
				public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
				{
					final int fromPos = viewHolder.getAdapterPosition();
					final int toPos = target.getAdapterPosition();
					PlaylistTrack track = tracks.remove(fromPos);
					tracks.add(toPos, track);
					mAdapter.notifyItemMoved(fromPos, toPos);

					if(startIndexToChange > fromPos || startIndexToChange > toPos){
						startIndexToChange = (fromPos < toPos)? fromPos : toPos;
					}
					return true;
				}

				@Override
				public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
				{
					final int fromPos = viewHolder.getAdapterPosition();
					PlaylistTrack track = tracks.remove(fromPos);
					deletedTracks.add(track);
					mAdapter.notifyItemRemoved(fromPos);

					if(startIndexToChange > fromPos){
						startIndexToChange = fromPos;
					}
				}
			});

		mTouchHelper.attachToRecyclerView(recyclerView);

		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
		recyclerView.addItemDecoration(dividerItemDecoration);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu,inflater);

		inflater.inflate(R.menu.menu_fragment_playlist_edit, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.menu_save:

				for(int i = 0; i < tracks.size(); i++){
					tracks.get(i).playlistTrackNo = i;
				}
				for(int i = 0; i < deletedTracks.size(); i++){
					deletedTracks.get(i).playlistTrackNo = -1;
				}

				List<PlaylistTrack> ptracks = new ArrayList<>();
				ptracks.addAll(tracks.subList(startIndexToChange, tracks.size()));
				ptracks.addAll(deletedTracks);

				Intent intent = new Intent(MusicDBService.ACTION_UPDATE_PLAYLISTTRACKS);
				intent.putExtra(MusicDBService.INTENT_KEY_PLAYLIST, playlist);
				intent.putExtra(MusicDBService.INTENT_KEY_PLAYLISTTRACK, (Serializable)ptracks);
				intent.setPackage(App.PACKAGE);
				getActivity().startService(intent);

				getFragmentManager().popBackStack();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	public class PlaylistTrackAdapter extends RecyclerView.Adapter<ViewHolder> {

		private List<PlaylistTrack> list;

		public PlaylistTrackAdapter(List<PlaylistTrack> list) {
			this.list = list;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleritem_playlist_track, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
			Track item = list.get(position);

			holder.trackTextView.setText(item.title);
			holder.artistTextView.setText(item.getArtistString());
			holder.durationTextView.setText(item.getDurationString());

			holder.handleView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
						mTouchHelper.startDrag(holder);
					}
					return false;
				}
			});
		}

		@Override
		public int getItemCount() {
			return list.size();
		}
	}

	private class ViewHolder extends RecyclerView.ViewHolder {
		TextView trackTextView;
		TextView artistTextView;
		TextView durationTextView;
		ImageView handleView;

		public ViewHolder(View itemView) {
			super(itemView);

			trackTextView = itemView.findViewById(R.id.title_view);
			artistTextView = itemView.findViewById(R.id.artist_view);
			durationTextView = itemView.findViewById(R.id.duration_view);
			handleView = itemView.findViewById(R.id.handle_view);
		}
	}
}
