package com.citrus.suzaku.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.R;
import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.track.TrackActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;


public class DockFragment extends Fragment implements ServiceConnection
{
	private Messenger mService;
	private boolean isBound;
	
	private final Messenger mMessenger = new Messenger(new MyHandler(this));

	private boolean isPlaying = false;
	private boolean isStopped = true;
	
	private TextView trackTextView;
	private TextView albumTextView;
	private TextView artistTextView;
	private ImageView artworkImageView;
	private ImageButton playButton;
	private View view;
	private View progressView;

	private Track mTrack;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.fragment_dock, container, false);

		// UI

		LinearLayout dock = (LinearLayout)view.findViewById(R.id.dock);
		dock.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if(mTrack != null){
					getActivity().startActivity(new Intent(getActivity(), TrackActivity.class));
				}
			}
		});

		trackTextView = (TextView)view.findViewById(R.id.track_view);
		albumTextView = (TextView)view.findViewById(R.id.album_view);
		artistTextView = (TextView)view.findViewById(R.id.artist_view);

		artworkImageView = (ImageView)view.findViewById(R.id.artwork_view);

		playButton = (ImageButton)view.findViewById(R.id.play_button);
		ImageButton prevButton = (ImageButton)view.findViewById(R.id.prev_button);
		ImageButton nextButton = (ImageButton)view.findViewById(R.id.next_button);

		playButton.setImageResource(R.drawable.ic_play_white_32dp);
		playButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v){
				if(mTrack != null){
					if(isStopped){
						Intent intent = new Intent(PlayerService.ACTION_PLAY_PAUSE);
						intent.setPackage(App.PACKAGE);
						getActivity().startService(intent);
					}else{
						sendMessege(PlayerService.MSG_PLAY_PAUSE);
					}
				}
			}
		});

		nextButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v){
				if(mTrack != null){
					sendMessege(PlayerService.MSG_NEXT);
				}
			}
		});

		prevButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v){
				if(mTrack != null){
					sendMessege(PlayerService.MSG_PREV);
				}
			}
		});

		progressView = view.findViewById(R.id.progress_view);

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		
		Intent intent = new Intent(getActivity(), PlayerService.class);
		getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);

		EventBus.getDefault().register(this);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		if(isBound){
			sendMessege(PlayerService.MSG_REQUEST_INFO);
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();

		if(isBound){
			getActivity().unbindService(this);
			isBound = false;
		}

		EventBus.getDefault().unregister(this);
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service)
	{
		mService = new Messenger(service);
		isBound = true;

		sendMessege(PlayerService.MSG_REQUEST_INFO);
	}

	@Override
	public void onServiceDisconnected(ComponentName name)
	{
		mService = null;
		isBound = false;
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.MediaScanningEvent event)
	{
		trackTextView.setText(getString(R.string.msg_scanning_place, event.rootPath));
		albumTextView.setText(event.title);
		artistTextView.setText("");

			ViewGroup.LayoutParams params = progressView.getLayoutParams();
			params.width = view.getWidth() * event.percent / 100;
			progressView.setLayoutParams(params);

			progressView.setVisibility(View.VISIBLE);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.DatabaseChangedEvent event)
	{
		// 画面復帰
		updateView();
		progressView.setVisibility(View.GONE);
	}
	
	private void updateView()
	{
		if(mTrack != null){
			trackTextView.setText(mTrack.title);
			albumTextView.setText(mTrack.getAlbumString());
			artistTextView.setText(mTrack.getArtistString());
			
			if(ArtworkCache.isCorrectHash(mTrack.artworkHash)){
				ArtworkCache.Small.setArtworkView(artworkImageView, mTrack);
			}else{
				artworkImageView.setImageResource(R.drawable.img_blank);
			}
		}else{
			trackTextView.setText(R.string.app_name);
			albumTextView.setText("");
			artistTextView.setText("");
			artworkImageView.setImageResource(R.drawable.img_blank);
		}
	}
	
	private void updateState()
	{
		if(isPlaying){
			playButton.setImageResource(R.drawable.ic_pause_white_32dp);
		}else{
			playButton.setImageResource(R.drawable.ic_play_white_32dp);
		}
	}
	
	private void sendMessege(int what)
	{
		if(!isBound){
			return;
		}
		
		Message msg = Message.obtain(null, what);
		msg.replyTo = mMessenger;
		
		try{
			mService.send(msg);
		}catch(RemoteException e){
			e.printStackTrace();
		}
	}

	private void handleMessage(Message msg)
	{
		if(!isBound){
			return;
		}

		Bundle bundle = msg.getData();

		switch(msg.what){
			case PlayerService.MSG_NOTIFY_TRACK:
				mTrack = (Track)bundle.getSerializable(PlayerService.KEY_TRACK);
				updateView();
				break;
			case PlayerService.MSG_NOTIFY_STATE:
				isPlaying = bundle.getBoolean(PlayerService.KEY_PLAYING, false);
				isStopped = bundle.getBoolean(PlayerService.KEY_STOPPED, true);
				updateState();
				break;
			case PlayerService.MSG_STOPPED:
				isPlaying = false;
				updateState();
				break;
		}
	}
	
	
	private static class MyHandler extends Handler
	{
		private WeakReference<DockFragment> mFragmentRef;

		public MyHandler(DockFragment fragment)
		{
			mFragmentRef = new WeakReference<>(fragment);
		}

		@Override
		public void handleMessage(Message msg)
		{
			DockFragment fragment = mFragmentRef.get();
			if(fragment != null){
				fragment.handleMessage(msg);
			}
		}
	}
	
}
