package com.citrus.suzaku;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.content.*;
import android.view.*;
import android.widget.*;
import android.widget.Chronometer.*;
import android.widget.SeekBar.*;

import java.io.*;
import java.util.*;


public class TrackActivity extends Activity implements OnChronometerTickListener, ServiceConnection
{
	private Messenger mService;
	private boolean isBound;

	private final Messenger mMessenger = new Messenger(new MyHandler());
	
	private Track trackItem;
	private boolean isPlaying = false;
	private boolean isStopped = true;

	private String lyrics;
	
	private TextView titleTextView;
	private TextView albumTextView;
	private TextView artistTextView;
	private ImageButton artworkImageButton;
	private ImageButton playButton;
	private ImageButton loopButton;
	private ImageButton shuffleButton;
	private SeekBar durationBar;
	private Chronometer chronometer;
	private TextView numberTextView;
	
	private View panelView;
	private TextView lyricsTextView;
	private Button playlistButton;
	private Button detailButton;

	private ListView playlistView;
	PlaylistTrackListAdapter playlistAdapter;

	private boolean animInProgress;
	private int animDuration;
//	private Handler panelHandler;

	private PlaylistManager playlist;

	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_track);
		
		ArtworkCache.initialize();

		// Data

		playlist = new PlaylistManager();
		playlist.load();
	
		// UI

/*		// 透過ステータスバー関連
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		View content = findViewById(android.R.id.content);
		content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);
*/
		titleTextView = (TextView)findViewById(R.id.track);
		albumTextView = (TextView)findViewById(R.id.album);
		artistTextView = (TextView)findViewById(R.id.artist);
		artworkImageButton = (ImageButton)findViewById(R.id.artwork);

		artworkImageButton.setTag("");
		artworkImageButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				fadePanel();
			}
		});

		durationBar = (SeekBar)findViewById(R.id.durationBar);
		
		durationBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onStartTrackingTouch(SeekBar seekbar)
			{
			}
			
			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser)
			{
				if(fromUser){
					Bundle bundle = new Bundle();
					bundle.putInt(PlayerService.KEY_TIME, progress * 1000);
					sendMessage(PlayerService.MSG_SEEK, bundle);
				}
				chronometer.setBase(SystemClock.elapsedRealtime() - (progress * 1000));
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekbar)
			{
			}
		});
		
		chronometer = (Chronometer)findViewById(R.id.chronometer);
		chronometer.setOnChronometerTickListener(this);
		
		numberTextView = (TextView)findViewById(R.id.number);
		
		playButton = (ImageButton)findViewById(R.id.playButton);
		ImageButton prevButton = (ImageButton)findViewById(R.id.prevButton);
		ImageButton nextButton = (ImageButton)findViewById(R.id.nextButton);
		loopButton = (ImageButton)findViewById(R.id.loopButton);
		shuffleButton = (ImageButton)findViewById(R.id.shuffleButton);
		
		playButton.setImageResource(R.drawable.ic_pause);
		playButton.setOnClickListener(new ImageButton.OnClickListener(){
			public void onClick(View v)
			{
				if(isStopped){
					Intent intent = new Intent(PlayerService.ACTION_PLAY_PAUSE);
					intent.setPackage(App.PACKAGE);
					startService(intent);
				}else{
					sendMessage(PlayerService.MSG_PLAY_PAUSE, null);
				}
			}
		});
		
		nextButton.setOnClickListener(new ImageButton.OnClickListener(){
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_NEXT, null);
			}
		});
		
		prevButton.setOnClickListener(new ImageButton.OnClickListener(){
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_PREV, null);
			}
		});
		
		loopButton.setImageResource(R.drawable.ic_loop);
		loopButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_SWITCH_LOOPMODE, null);
			}
		});
		
		shuffleButton.setImageResource(R.drawable.ic_shuffle);
		shuffleButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_SWITCH_SHUFFLEMODE, null);
			}
		});
		
		panelView = findViewById(R.id.panel);
		panelView.setVisibility(View.GONE);

		lyricsTextView = (TextView)findViewById(R.id.lyrics);
		lyricsTextView.setClickable(true);
		lyricsTextView.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				fadePanel();
			}
		});

		detailButton = (Button)findViewById(R.id.detailButton);
		detailButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				List<Track> tracks = new ArrayList<>();
				tracks.add(trackItem);

				Intent intent = new Intent(TrackActivity.this, TrackDetailActivity.class);
				intent.putExtra("TRACKS", (Serializable)tracks);
				startActivity(intent);

				fadePanel();
			}
		});

		playlistButton = (Button)findViewById(R.id.playlistButton);
		playlistButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				fadePlaylistView();
			}
		});

		playlistView = (ListView)findViewById(R.id.playlist);
		playlistAdapter = new PlaylistTrackListAdapter();
		playlistAdapter.setDataList(playlist.getPlaylist());
		playlistView.setAdapter(playlistAdapter);

		
		animDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
		
	//	panelHandler = new Handler();
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		Intent intent = new Intent(this, PlayerService.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	
		if(isBound){
			sendMessage(PlayerService.MSG_REQUEST_INFO, null);
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		
		if(isBound){
			unbindService(this);
			isBound = false;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		artworkImageButton.setImageDrawable(null);
		chronometer.setOnChronometerTickListener(null);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service)
	{
		mService = new Messenger(service);
		isBound = true;
		
		sendMessage(PlayerService.MSG_REQUEST_INFO, null);
	}

	@Override
	public void onServiceDisconnected(ComponentName name)
	{
		mService = null;
		isBound = false;
	}
	
	@Override
	public void onChronometerTick(Chronometer meter)
	{
		durationBar.setProgress((int)((SystemClock.elapsedRealtime() - meter.getBase()) / 1000));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_activity_track, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;

		switch(item.getItemId()){
			case R.id.menu_detail:
				List<Track> tracks = new ArrayList<>();
				tracks.add(trackItem);
				
				intent = new Intent(this, TrackDetailActivity.class);
				intent.putExtra("TRACKS", (Serializable)tracks);
				startActivity(intent);
				return true;

			case R.id.menu_settings:
				intent = new Intent(this, SettingActivity.class);
				startActivity(intent);
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void fadePanel()
	{
		if(animInProgress){
			return;
		}

		if(panelView.getVisibility() == View.GONE){
			panelView.setAlpha(0f);
			panelView.setVisibility(View.VISIBLE);

			panelView.animate()
				.alpha(1f)
				.setDuration(animDuration)
				.setListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation)
					{
						animInProgress = false;
					}
				});
			
			// 遅延実行
/*			panelHandler.postDelayed(new Runnable(){
				@Override
				public void run(){
					fadePanel();
				}
			}, 10000);
*/
		}else{
			panelView.animate()
				.alpha(0f)
				.setDuration(animDuration)
				.setListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation)
					{
						panelView.setVisibility(View.GONE);
						animInProgress = false;
					}
				});
			
		//	panelHandler.removeCallbacksAndMessages(null);
		}
		
		animInProgress = true;
	}

	private void fadePlaylistView()
	{
		if(animInProgress){
			return;
		}

		if(playlistView.getVisibility() == View.GONE){
			playlistView.setAlpha(0f);
			playlistView.setVisibility(View.VISIBLE);

			playlistView.animate()
					.alpha(1f)
					.setDuration(animDuration)
					.setListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation)
						{
							animInProgress = false;
						}
					});

			fadePanel();

		}else{
			playlistView.animate()
					.alpha(0f)
					.setDuration(animDuration)
					.setListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation)
						{
							playlistView.setVisibility(View.GONE);
							animInProgress = false;
						}
					});
		}

		animInProgress = true;
	}
	
	private void updateView(Bundle bundle)
	{
		playlist.load();
		playlistAdapter.setDataList(playlist.getPlaylist());
		playlistAdapter.notifyDataSetChanged();

		trackItem = (Track)bundle.getSerializable(PlayerService.KEY_TRACK);
		int position = bundle.getInt(PlayerService.KEY_POSITION);
		int count = bundle.getInt(PlayerService.KEY_COUNT);
		int loopMode = bundle.getInt(PlayerService.KEY_LOOPMODE);
		boolean shuffleMode = bundle.getBoolean(PlayerService.KEY_SHUFFLEMODE);
		
		if(trackItem == null){
			finish();
			return;
		}
		
		titleTextView.setText(trackItem.title);
		albumTextView.setText(trackItem.getAlbumString());
		artistTextView.setText(trackItem.getArtistString());

		if(ArtworkCache.isCorrectHash(trackItem.artworkHash)){
			if(!trackItem.artworkHash.equals(artworkImageButton.getTag())){
				ArtworkCache.Large.setArtworkViewWithCache(artworkImageButton, trackItem);
			}
		}else{
			artworkImageButton.setImageResource(R.drawable.big_blank);
			artworkImageButton.setTag("");
		}
		
		durationBar.setMax((int)(trackItem.duration / 1000));
	
		chronometer.setFormat("%s / " + trackItem.getDurationString());
		numberTextView.setText(String.valueOf(position + 1) + " / " + String.valueOf(count));
		
		
		switch(loopMode){
			case PlaylistManager.LOOPMODE_OFF:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.white));
				loopButton.setImageResource(R.drawable.ic_loop);
				break;
			case PlaylistManager.LOOPMODE_ALL:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
				loopButton.setImageResource(R.drawable.ic_loop);
				break;
			case PlaylistManager.LOOPMODE_ONE:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
				loopButton.setImageResource(R.drawable.ic_loop_one);
				break;
		}
		
		if(!shuffleMode){
			shuffleButton.setColorFilter(ContextCompat.getColor(this, R.color.white));
		}else{
			shuffleButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
		}

		//! EXPERIMENTAL
		TagLibHelper tagHelper = new TagLibHelper();
		tagHelper.setFile(trackItem.path);
		lyrics = convertNewlineCode(tagHelper.getLyrics());
		tagHelper.release();

		lyricsTextView.setText(lyrics);
	}
	
	private void updateState()
	{
		if(isPlaying){
			playButton.setImageResource(R.drawable.ic_pause);
			chronometer.start();
		}else{
			playButton.setImageResource(R.drawable.ic_play);
			chronometer.stop();
		}
	}
	
	private void updateTime(int time)
	{
		durationBar.setProgress(time / 1000);

		chronometer.stop();
		chronometer.setBase(SystemClock.elapsedRealtime() - time);
		if(isPlaying){
			chronometer.start();
		}
	}
	
	private void sendMessage(int what, Bundle bundle)
	{
		if(!isBound){
			return;
		}
		
		Message msg = Message.obtain(null, what);
		msg.setData(bundle);
		msg.replyTo = mMessenger;
		
		try{
			mService.send(msg);
		}catch(RemoteException e){
			e.printStackTrace();
		}
	}

	private static String convertNewlineCode(String org)
	{
		String code = System.getProperty("line.separator");
		return (org != null)? org.replaceAll("\r\n|[\n\r\u2028\u2029\u0085]", code) : null;
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
				case PlayerService.MSG_NOTIFY_STATE:
					isPlaying = bundle.getBoolean(PlayerService.KEY_PLAYING, false);
					isStopped = bundle.getBoolean(PlayerService.KEY_STOPPED, true);
					updateState();
					break;
				case PlayerService.MSG_NOTIFY_TRACK:
					updateView(bundle);
					break;
				case PlayerService.MSG_STOPPED:
					finish();
					break;
				case PlayerService.MSG_NOTIFY_TIME:
					updateTime(bundle.getInt(PlayerService.KEY_TIME));
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}


	// Adapter
	private class PlaylistTrackListAdapter extends BaseListAdapter<Track>
	{
		private LayoutInflater inflater;

		public PlaylistTrackListAdapter()
		{
			Context context = App.getContext();
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(int position, View view)
		{
			ViewHolder holder = (ViewHolder)view.getTag();

			Track item = getItem(position);

			holder.trackTextView.setText(item.title);
			holder.artistTextView.setText(item.getArtistString());
			holder.durationTextView.setText(item.getDurationString());
		}

		@Override
		public View newView(ViewGroup parent)
		{
			View view = inflater.inflate(R.layout.listitem_track_dark, null, false);

			ViewHolder holder = new ViewHolder();

			holder.trackTextView = (TextView)view.findViewById(R.id.title);
			holder.artistTextView = (TextView)view.findViewById(R.id.artist);
			holder.durationTextView = (TextView)view.findViewById(R.id.duration);

			view.setTag(holder);

			return view;
		}
	}

	private static class ViewHolder
	{
		TextView trackTextView;
		TextView artistTextView;
		TextView durationTextView;
	}

}
