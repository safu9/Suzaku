package com.citrus.suzaku.track;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
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
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.citrus.suzaku.App;
import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.R;
import com.citrus.suzaku.SettingActivity;
import com.citrus.suzaku.TagLibHelper;
import com.citrus.suzaku.base.BaseListAdapter;
import com.citrus.suzaku.player.PlayerService;
import com.citrus.suzaku.player.PlaylistManager;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class TrackActivity extends Activity implements OnChronometerTickListener, PopupMenu.OnMenuItemClickListener, ServiceConnection
{
	private Messenger mService;
	private boolean isBound;

	private final Messenger mMessenger = new Messenger(new MyHandler(this));
	
	private Track trackItem;
	private boolean isPlaying = false;
	private boolean isStopped = true;
	
	private TextView titleTextView;
	private TextView albumTextView;
	private TextView artistTextView;

	private ImageButton popupButton;
	private ImageButton closeButton;

	private ImageButton artworkImageButton;
	private ImageButton playButton;
	private ImageButton loopButton;
	private ImageButton shuffleButton;
	private SeekBar durationBar;
	private Chronometer chronometer;
	private TextView numberTextView;
	
	private View panelView;
	private Button switchPanelButton;

	private ScrollView lyricsScrollView;
	private TextView lyricsTextView;

	private ListView playlistView;
	private PlaylistTrackListAdapter playlistAdapter;

	private int animDuration;

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
		titleTextView = (TextView)findViewById(R.id.track_view);
		albumTextView = (TextView)findViewById(R.id.album_view);
		artistTextView = (TextView)findViewById(R.id.artist_view);
		artworkImageButton = (ImageButton)findViewById(R.id.artwork_view);

		ImageButton navBeforeButton = (ImageButton)findViewById(R.id.nav_before_button);
		navBeforeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		popupButton = (ImageButton)findViewById(R.id.popup_button);
		popupButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				PopupMenu menu = new PopupMenu(TrackActivity.this, v);
				menu.getMenuInflater().inflate(R.menu.menu_activity_track, menu.getMenu());
				menu.setOnMenuItemClickListener(TrackActivity.this);
				menu.show();
			}
		});

		closeButton = (ImageButton)findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				fadePanel();
			}
		});

		artworkImageButton.setTag("");
		artworkImageButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				fadePanel();
			}
		});

		durationBar = (SeekBar)findViewById(R.id.duration_bar);
		
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
		
		numberTextView = (TextView)findViewById(R.id.number_view);
		
		playButton = (ImageButton)findViewById(R.id.play_button);
		ImageButton prevButton = (ImageButton)findViewById(R.id.prev_button);
		ImageButton nextButton = (ImageButton)findViewById(R.id.next_button);
		loopButton = (ImageButton)findViewById(R.id.loop_button);
		shuffleButton = (ImageButton)findViewById(R.id.shuffle_button);
		
		playButton.setImageResource(R.drawable.ic_pause_white_32dp);
		playButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
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
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_NEXT, null);
			}
		});
		
		prevButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_PREV, null);
			}
		});
		
		loopButton.setImageResource(R.drawable.ic_loop_white_32dp);
		loopButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_SWITCH_LOOPMODE, null);
			}
		});
		
		shuffleButton.setImageResource(R.drawable.ic_shuffle_white_32dp);
		shuffleButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				sendMessage(PlayerService.MSG_SWITCH_SHUFFLEMODE, null);
			}
		});
		
		panelView = findViewById(R.id.panel);
		panelView.setVisibility(View.GONE);

		lyricsScrollView = (ScrollView)findViewById(R.id.lyrics_scroll);

		lyricsTextView = (TextView)findViewById(R.id.lyrics_view);
		lyricsTextView.setClickable(true);
		lyricsTextView.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				fadePanel();
			}
		});

		Button detailButton = (Button)findViewById(R.id.detail_button);
		detailButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				List<Long> trackIds = new ArrayList<>();
				trackIds.add(trackItem.id);

				Intent intent = new Intent(TrackActivity.this, TrackDetailActivity.class);
				intent.putExtra("IDS", (Serializable)trackIds);
				startActivity(intent);

				fadePanel();
			}
		});

		switchPanelButton = (Button)findViewById(R.id.switch_panel_button);
		switchPanelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switchPanel();
			}
		});

		playlistView = (ListView)findViewById(R.id.playlist);
		playlistAdapter = new PlaylistTrackListAdapter();
		playlistAdapter.setDataList(playlist.getPlaylist());
		playlistView.setAdapter(playlistAdapter);
		playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Bundle bundle = new Bundle();
				bundle.putInt(PlayerService.KEY_POSITION, position);
				sendMessage(PlayerService.MSG_SET_POSITION, bundle);
			}
		});

		animDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
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
		return handleMenuItem(item) || super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item)
	{
		return handleMenuItem(item);
	}

	private boolean handleMenuItem(MenuItem item)
	{
		Intent intent;

		switch(item.getItemId()){
			case R.id.menu_lyrics:
				if(panelView.getVisibility() != View.VISIBLE){
					fadePanel();
				}
				if(lyricsScrollView.getVisibility() != View.VISIBLE){
					switchPanel();
				}
				break;

			case R.id.menu_playing_list:
				if(panelView.getVisibility() != View.VISIBLE){
					fadePanel();
				}
				if(playlistView.getVisibility() != View.VISIBLE){
					switchPanel();
				}
				break;

			case R.id.menu_detail:
				List<Long> trackIds = new ArrayList<>();
				trackIds.add(trackItem.id);

				intent = new Intent(this, TrackDetailActivity.class);
				intent.putExtra("IDS", (Serializable)trackIds);
				startActivity(intent);
				break;

			case R.id.menu_settings:
				intent = new Intent(this, SettingActivity.class);
				startActivity(intent);
				break;

			default:
				return false;
		}
		return true;
	}

	// パネルの表示/非表示の切り替え
	private void fadePanel()
	{
		fadeView(panelView);

		fadeView(popupButton);
		fadeView(closeButton);
	}

	// パネルの歌詞/プレイリストの切り替え
	private void switchPanel()
	{
		switchPanelButton.setText((lyricsScrollView.getVisibility() == View.VISIBLE)? R.string.lyrics : R.string.playlist);

		fadeView(lyricsScrollView);
		fadeView(playlistView);
	}

	private void fadeView(final View view)
	{
		Object tag = view.getTag();
		if(tag != null && (boolean)tag){
			return;
		}
		view.setTag(true);

		if(view.getVisibility() == View.GONE){
			view.setAlpha(0f);
			view.setVisibility(View.VISIBLE);

			view.animate()
					.alpha(1f)
					.setDuration(animDuration)
					.setListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation)
						{
							view.setTag(false);
						}
					});
		}else{
			view.animate()
					.alpha(0f)
					.setDuration(animDuration)
					.setListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation)
						{
							view.setVisibility(View.GONE);
							view.setTag(false);
						}
					});
		}
	}
	
	private void updateView(Bundle bundle)
	{
		trackItem = (Track)bundle.getSerializable(PlayerService.KEY_TRACK);
		int position = bundle.getInt(PlayerService.KEY_POSITION);
		int count = bundle.getInt(PlayerService.KEY_COUNT);
		
		if(trackItem == null){
			finish();
			return;
		}

		// TODO : avoid resetting Marquee
		titleTextView.setText(trackItem.title);
		albumTextView.setText(trackItem.getAlbumString());
		artistTextView.setText(trackItem.getArtistString());

		if(ArtworkCache.isCorrectHash(trackItem.artworkHash)){
			if(!trackItem.artworkHash.equals(artworkImageButton.getTag())){
				ArtworkCache.Large.setArtworkViewWithCache(artworkImageButton, trackItem);
			}
		}else{
			artworkImageButton.setImageResource(R.drawable.img_blank_big);
			artworkImageButton.setTag("");
		}
		
		durationBar.setMax((int)(trackItem.duration / 1000));
	
		chronometer.setFormat("%s / " + trackItem.getDurationString());
		numberTextView.setText(String.valueOf(position + 1) + " / " + String.valueOf(count));

		// Panel

		TagLibHelper tagHelper = new TagLibHelper();
		tagHelper.setFile(trackItem.path);
		String lyrics = convertNewlineCode(tagHelper.getLyrics());
		tagHelper.release();

		lyricsTextView.setText(lyrics);

		playlist.load();
		playlistAdapter.setDataList(playlist.getPlaylist());
		playlistAdapter.notifyDataSetChanged();
	}

	private void updatePlayMode(int loopMode, boolean shuffleMode)
	{
		switch(loopMode){
			case PlaylistManager.LOOPMODE_OFF:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.white));
				loopButton.setImageResource(R.drawable.ic_loop_white_32dp);
				break;
			case PlaylistManager.LOOPMODE_ALL:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
				loopButton.setImageResource(R.drawable.ic_loop_white_32dp);
				break;
			case PlaylistManager.LOOPMODE_ONE:
				loopButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
				loopButton.setImageResource(R.drawable.ic_loop_one_white_32dp);
				break;
		}

		if(!shuffleMode){
			shuffleButton.setColorFilter(ContextCompat.getColor(this, R.color.white));
		}else{
			shuffleButton.setColorFilter(ContextCompat.getColor(this, R.color.orange_light));
		}
	}
	
	private void updateState()
	{
		if(isPlaying){
			playButton.setImageResource(R.drawable.ic_pause_white_32dp);
			chronometer.start();
		}else{
			playButton.setImageResource(R.drawable.ic_play_white_32dp);
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

	private void handleMessage(Message msg)
	{
		if(!isBound){
			return;
		}

		Bundle bundle = msg.getData();

		switch(msg.what){
			case PlayerService.MSG_NOTIFY_TRACK:
				updateView(bundle);
				break;
			case PlayerService.MSG_NOTIFY_PLAY_MODE:
				int loopMode = bundle.getInt(PlayerService.KEY_LOOPMODE);
				boolean shuffleMode = bundle.getBoolean(PlayerService.KEY_SHUFFLEMODE);
				updatePlayMode(loopMode, shuffleMode);
				break;
			case PlayerService.MSG_NOTIFY_STATE:
				isPlaying = bundle.getBoolean(PlayerService.KEY_PLAYING, false);
				isStopped = bundle.getBoolean(PlayerService.KEY_STOPPED, true);
				updateState();
				break;
			case PlayerService.MSG_STOPPED:
				finish();
				break;
			case PlayerService.MSG_NOTIFY_TIME:
				updateTime(bundle.getInt(PlayerService.KEY_TIME));
				break;
		}
	}

	private static String convertNewlineCode(String org)
	{
		String code = System.getProperty("line.separator");
		return (org != null)? org.replaceAll("\r\n|[\n\r\u2028\u2029\u0085]", code) : null;
	}

	private static class MyHandler extends Handler
	{
		private WeakReference<TrackActivity> mActivityRef;

		public MyHandler(TrackActivity activity)
		{
			mActivityRef = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			TrackActivity activity = mActivityRef.get();
			if(activity != null){
				activity.handleMessage(msg);
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
			View view = inflater.inflate(R.layout.listitem_track_dark, parent, false);

			ViewHolder holder = new ViewHolder();

			holder.trackTextView = (TextView)view.findViewById(R.id.title_view);
			holder.artistTextView = (TextView)view.findViewById(R.id.artist_view);
			holder.durationTextView = (TextView)view.findViewById(R.id.duration_view);

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
