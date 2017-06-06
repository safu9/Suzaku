package com.citrus.suzaku;

import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;


public class DockFragment extends Fragment implements ServiceConnection
{
	private Messenger mService;
	private boolean isBound;
	
	private final Messenger mMessenger = new Messenger(new MyHandler());

	private boolean isPlaying = false;
	private boolean isStopped = true;
	
	private TextView trackTextView;
	private TextView albumTextView;
	private TextView artistTextView;
	private ImageView artworkImageView;
	private ImageButton playButton;
	
	private boolean isReady = false;			// 画面遷移
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_dock, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		// UI
		
		LinearLayout dock = (LinearLayout)getActivity().findViewById(R.id.dock);
		dock.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if(isReady){
					getActivity().startActivity(new Intent(getActivity(), TrackActivity.class));
				}
			}
		});
		
		trackTextView = (TextView)getActivity().findViewById(R.id.dock_track);
		albumTextView = (TextView)getActivity().findViewById(R.id.dock_album);
		artistTextView = (TextView)getActivity().findViewById(R.id.dock_artist);
		
		artworkImageView = (ImageView)getActivity().findViewById(R.id.dock_artwork);
		
		playButton = (ImageButton)getActivity().findViewById(R.id.dock_playButton);
		ImageButton prevButton = (ImageButton)getActivity().findViewById(R.id.dock_prevButton);
		ImageButton nextButton = (ImageButton)getActivity().findViewById(R.id.dock_nextButton);

		playButton.setImageResource(R.drawable.ic_play);
		playButton.setOnClickListener(new ImageButton.OnClickListener(){
			public void onClick(View v){
				if(isReady){
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
			public void onClick(View v){
				if(isReady){
					sendMessege(PlayerService.MSG_NEXT);
				}
			}
		});

		prevButton.setOnClickListener(new ImageButton.OnClickListener(){
			public void onClick(View v){
				if(isReady){
					sendMessege(PlayerService.MSG_PREV);
				}
			}
		});
	}

	@Override
	public void onStart()
	{
		super.onStart();
		
		Intent intent = new Intent(getActivity(), PlayerService.class);
		getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
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
	
	private void updateView(Track track)
	{
		if(track != null){
			trackTextView.setText(track.title);
			albumTextView.setText(track.getAlbumString());
			artistTextView.setText(track.getArtistString());
			
			if(ArtworkCache.isCorrectHash(track.artworkHash)){
				ArtworkCache.Small.setArtworkView(artworkImageView, track);
			}else{
				artworkImageView.setImageResource(R.drawable.blank);
			}
			
			isReady = true;
		}else{
			trackTextView.setText(R.string.app_name);
			albumTextView.setText("");
			artistTextView.setText("");
			artworkImageView.setImageResource(R.drawable.blank);
			
			isReady = false;
		}
	}
	
	private void updateState()
	{
		if(isPlaying){
			playButton.setImageResource(R.drawable.ic_pause);
		}else{
			playButton.setImageResource(R.drawable.ic_play);
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
	
	
	private class MyHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			if(!isBound){
				return;
			}

			Bundle bundle = msg.getData();

			switch(msg.what){
				case PlayerService.MSG_NOTIFY_TRACK:
					Track track = (Track)bundle.getSerializable(PlayerService.KEY_TRACK);
					updateView(track);
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
				default:
					super.handleMessage(msg);
			}
		}
	}
	
}
