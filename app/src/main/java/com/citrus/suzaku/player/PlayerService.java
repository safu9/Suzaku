package com.citrus.suzaku.player;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.widget.*;

import com.citrus.suzaku.App;
import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.main.MainActivity;
import com.citrus.suzaku.R;
import com.citrus.suzaku.track.Track;
import com.citrus.suzaku.track.TrackActivity;
import com.citrus.suzaku.base.TrackGroup;

import java.io.*;
import java.lang.ref.WeakReference;


public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener
{
	private static final int NOTIFICATION_ID = 1;
	private static final String CHANNEL_ID = "suzaku";
	
	// From Activity, RemoteViews & RemoteControl
	public static final String ACTION_PLAY = "Citrus.suzaku.action.ACTION_PLAY";
	public static final String ACTION_ENQUEUE = "Citrus.suzaku.action.ACTION_ENQUEUE";
	public static final String ACTION_PLAY_PAUSE = "Citrus.suzaku.action.ACTION_PLAY_PAUSE";
	public static final String ACTION_NEXT = "Citrus.suzaku.action.ACTION_NEXT";
	public static final String ACTION_PREV = "Citrus.suzaku.action.ACTION_PREV";
	public static final String ACTION_STOP = "Citrus.suzaku.action.ACTION_STOP";
	
	public static final int MSG_REQUEST_INFO = 1;
	public static final int MSG_PLAY_PAUSE = 2;
	public static final int MSG_NEXT = 3;
	public static final int MSG_PREV = 4;
	public static final int MSG_STOP = 5;
	public static final int MSG_SEEK = 6;
	public static final int MSG_SWITCH_LOOPMODE = 7;
	public static final int MSG_SWITCH_SHUFFLEMODE = 8;
	public static final int MSG_SET_POSITION = 9;
	
	// To Activity
	public static final int MSG_NOTIFY_TRACK = 10;
	public static final int MSG_NOTIFY_PLAY_MODE = 11;
	public static final int MSG_NOTIFY_STATE = 12;
	public static final int MSG_NOTIFY_TIME = 13;
	public static final int MSG_STOPPED = 14;
	
	// Intent Extra Key
	public static final String KEY_TRACK = "TRACK";
	public static final String KEY_POSITION = "POSITION";
	public static final String KEY_COUNT = "COUNT";
	public static final String KEY_LOOPMODE = "LOOP";
	public static final String KEY_SHUFFLEMODE = "SHUFFLE";
	public static final String KEY_PLAYING = "PLAYING";
	public static final String KEY_STOPPED = "STOPPED";
	public static final String KEY_TIME = "TIME";
	
	private final Messenger mMessenger = new Messenger(new MyHandler(this));
	private Messenger mReplyTo;
	
	private PlaylistManager playlist;				// 保存される情報

	private MediaPlayer player;
	private boolean isPlaying = false;				// Audio Focus の一時中断も加味
	private boolean isStopped = true;				// MediaPlayer

	private AudioManager audioManager;
	private RemoteControlClient remoteControlClient;

	private PowerManager.WakeLock wakeLock;

	private int startId;

	
	@Override
	public void onCreate()
	{
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		ArtworkCache.initialize();
		
		playlist = new PlaylistManager();
		playlist.load();

		createNotificationChannel();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		this.startId = startId;
		String action = intent.getAction();
		
		App.logd("PS Received : " + action);

		if(action == null){
			action = "";
		}

		switch(action) {
			case ACTION_PLAY:
				int playRange = intent.getIntExtra("PLAY_RANGE", 0);
				TrackGroup item = (TrackGroup) intent.getSerializableExtra("ITEM");
				int position = intent.getIntExtra("POSITION", 0);
				boolean shuffleStart = intent.getBooleanExtra("SHUFFLE_START", false);

				playlist.setPlaylistInfo(playRange, item, position, shuffleStart);

				if(setupPlayer()){
					playSong();
				}else{
					playlist.save();
					notifyTrack();
					stopSong();
				}
				break;
			case ACTION_PLAY_PAUSE:
				playPauseSong();
				break;
			case ACTION_NEXT:
				nextSong();
				break;
			case ACTION_PREV:
				prevSong();
				break;
			case ACTION_STOP:
				stopSong();
				break;
			default:
				stopSelf(startId);
		}
		
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		mReplyTo = null;
		playlist.save();
		
		return true;
	}

	@Override
	public void onDestroy()
	{
		if(!isStopped){
			stopSong();
		}
	}

	@Override
	public void onAudioFocusChange(int focusChange)
	{
		switch(focusChange){
			case AudioManager.AUDIOFOCUS_GAIN:
				if(isPlaying){
					player.start();
				}
				player.setVolume(1f, 1f);
				break;
				
			case AudioManager.AUDIOFOCUS_LOSS:
				stopSong();
				break;
				
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				if(isPlaying){
					player.pause();
				}
				break;
				
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				player.setVolume(0.2f, 0.2f);
				break;
		}
	}

	private void handleMessage(Message msg)
	{
		switch(msg.what){
			case MSG_REQUEST_INFO:
				App.logd("PS Received : MSG_REQUEST_INFO");
				mReplyTo = msg.replyTo;
				notifyTrack();
				notifyPlayMode();
				notifyState();
				notifyTime();
				break;
			case MSG_PLAY_PAUSE:
				App.logd("PS Received : MSG_PLAY_PAUSE");
				playPauseSong();
				break;
			case MSG_NEXT:
				App.logd("PS Received : MSG_NEXT");
				nextSong();
				break;
			case MSG_PREV:
				App.logd("PS Received : MSG_PREV");
				prevSong();
				break;
			case MSG_STOP:
				App.logd("PS Received : MSG_STOP");
				stopSong();
				break;
			case MSG_SEEK:
				App.logd("PS Received : MSG_SEEK");
				int time = msg.getData().getInt(KEY_TIME, 0);
				if(!isStopped){
					player.seekTo(time);
				}else{
					playlist.setStartTime(time);
				}
				break;
			case MSG_SWITCH_LOOPMODE:
				App.logd("PS Received : MSG_SWITCH_LOOPMODE");
				playlist.switchLoopMode();
				playlist.save();
				notifyPlayMode();
				break;
			case MSG_SWITCH_SHUFFLEMODE:
				App.logd("PS Received : MSG_SWITCH_SHUFFLEMODE");
				playlist.switchShuffleMode();
				playlist.save();
				notifyPlayMode();
				break;
			case MSG_SET_POSITION:
				App.logd("PS Received : MSG_SET_POSITION");
				int position = msg.getData().getInt(KEY_POSITION, 0);
				setCurrentPosition(position);
				break;
		}
	}
	
	private void playSong()
	{
		player.start();
		player.seekTo(playlist.getStartTime());
		
		if(isStopped){
			isStopped = false;

			// オーディオフォーカス
			audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			registerRemoteControl();
		}
		
		if(!isPlaying){
			isPlaying = true;
		}

		playlist.save();
		notifyTrack();
		notifyState();
		notifyTime();
		
		updateArtwork();
		updateNotification();
		updateRemoteControl();
		updateWidget();
	}
	
	private boolean setupPlayer()
	{
		Track track = playlist.getCurrentTrack();
		
		if(track == null){
			return false;
		}
		
		App.logd("PS Setting : " + track.title);
		
		try{
			if(player == null){
				initPlayer();
			}

			player.reset();
			player.setDataSource(track.path);
			player.prepare();
	
		}catch(IOException e){
			e.printStackTrace();
			Log.e("Suzaku", "PS IOException : Can't open file \"" + track.path + "\"");
			Toast.makeText(App.getContext(), App.getContext().getString(R.string.err_cant_play, track.title), Toast.LENGTH_SHORT).show();
			return false;
			
		}catch(Exception e){
			e.printStackTrace();
			Log.e("Suzaku", "PS Exception");
			return false;
		}

		aquireWakeLock();

		return true;
	}

	private void initPlayer()
	{
		player = new MediaPlayer();

		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp)
			{
				App.logd("PS onCompletion called.");
				if(playlist.getLoopMode() != PlaylistManager.LOOPMODE_ONE){
					nextSong();
				}else{
					player.start();				// はじめから
					notifyTime();
				}
			}
		});

		// USE prepareAsync()
/*		player.setOnErrorListener(new MediaPlayer.OnErrorListener(){
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra)
			{
				App.loge("PS onError called.");
				
				if(what == MediaPlayer.MEDIA_ERROR_UNKNOWN){
					App.logd("PS MEDIA_ERROR_UNKNOWN");
					return false;
				}
				
				switch(extra){
					case MediaPlayer.MEDIA_ERROR_IO:
						App.logd("PS MEDIA_ERROR_IO");
						break;
					case MediaPlayer.MEDIA_ERROR_MALFORMED:
						App.logd("PS MEDIA_ERROR_MALFORMED");
						break;
					case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
						App.logd("PS MEDIA_ERROR_UNSUPPORTED");
						break;
					case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
						App.logd("PS MEDIA_ERROR_TIMED_OUT");
						break;
				}

				return false;			// call onCompletion()?
			}
		});
*/
	}
	
	private void nextSong()
	{
		if(playlist.forwardTrack() == null){
			playlist.save();
			notifyTrack();
			stopSong();
			return;
		}
		
		if(!isStopped){
			if(!setupPlayer()){
				nextSong();
				return;
			}
			
			if(isPlaying){
				player.start();
			}
		}

		playlist.save();
		notifyTrack();
		notifyTime();
		
		updateArtwork();
		updateWidget();
		
		if(!isStopped){
			updateNotification();
			updateRemoteControl();
		}else{
			stopSelf(startId);
		}
	}
	
	private void prevSong()
	{
		if(!isStopped && player.getCurrentPosition() > 3000){
			player.seekTo(0);
			notifyTime();
			return;
		}
		
		if(playlist.backTrack() == null){
			playlist.save();
			notifyTrack();
			stopSong();
			return;
		}
	
		if(!isStopped){
			if(!setupPlayer()){
				prevSong();
				return;
			}
			
			if(isPlaying){
				player.start();
			}
		}

		playlist.save();
		notifyTrack();
		notifyTime();

		updateArtwork();
		updateWidget();

		if(!isStopped){
			updateNotification();
			updateRemoteControl();
		}else{
			stopSelf(startId);
		}
	}
	
	private void playPauseSong()
	{
		if(isStopped){
			if(!setupPlayer()){
				stopSong();
			}else{
				playSong();
			}
			return;
		}
		
		if(isPlaying){
			player.pause();
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
			releaseWakeLock();
			
			isPlaying = false;
		}else{
			aquireWakeLock();
			player.start();
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			
			isPlaying = true;
		}
		
		notifyState();
		notifyTime();
		
		updateNotification();
		updateWidget();
	}
	
	private void stopSong()
	{
		if(player != null){
			playlist.setStartTime(player.getCurrentPosition());
			
			if(isPlaying){
				player.stop();
				isPlaying = false;
			}

			player.reset();
			player.release();
			player = null;
		}

		releaseWakeLock();
		
		if(!isStopped){
			stopForeground(true);

			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
			audioManager.unregisterRemoteControlClient(remoteControlClient);
			audioManager.abandonAudioFocus(this);
			
			isStopped = true;
		}
		
		playlist.save();
		
		replyMessage(MSG_STOPPED, null);
		updateWidget();
		
		stopSelf(startId);
	}

	private void setCurrentPosition(int position)
	{
		playlist.setCurrentPosition(position);

		if(!isStopped){
			if(!setupPlayer()){
				playlist.save();
				notifyTrack();
				stopSong();
				return;
			}

			if(isPlaying){
				player.start();
			}
		}

		playlist.save();
		notifyTrack();
		notifyTime();

		updateArtwork();
		updateWidget();

		if(!isStopped){
			updateNotification();
			updateRemoteControl();
		}else{
			stopSelf(startId);
		}
	}

	private void aquireWakeLock()
	{
		if(wakeLock == null){
			PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PlayerService.class.toString());
			wakeLock.acquire();
		}
	}

	private void releaseWakeLock()
	{
		if(wakeLock != null){
			wakeLock.release();
			wakeLock = null;
		}
	}
	
	private void updateArtwork()
	{
		Track track = playlist.getCurrentTrack();
		
		if(track == null || !ArtworkCache.isCorrectHash(track.artworkHash)){
			return;
		}
		
		if(ArtworkCache.Large.getArtworkCache(track) == null){
			new ImageGetTask(track).execute();
		}
		
	}
	
	// Reply to Activity or Fragment
	
	private void notifyTrack()
	{
		Bundle bundle = new Bundle();
		bundle.putSerializable(KEY_TRACK, playlist.getCurrentTrack());
		bundle.putInt(KEY_POSITION, playlist.getCurrentPosition());
		bundle.putInt(KEY_COUNT, playlist.getSize());
		bundle.putInt(KEY_LOOPMODE, playlist.getLoopMode());
		bundle.putBoolean(KEY_SHUFFLEMODE, playlist.getShuffleMode());
		replyMessage(MSG_NOTIFY_TRACK, bundle);
	}

	private void notifyPlayMode()
	{
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_LOOPMODE, playlist.getLoopMode());
		bundle.putBoolean(KEY_SHUFFLEMODE, playlist.getShuffleMode());
		replyMessage(MSG_NOTIFY_PLAY_MODE, bundle);
	}
	
	private void notifyState()
	{
		Bundle bundle = new Bundle();
		bundle.putBoolean(KEY_PLAYING, isPlaying);
		bundle.putBoolean(KEY_STOPPED, isStopped);
		replyMessage(MSG_NOTIFY_STATE, bundle);
	}
	
	private void notifyTime()
	{
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_TIME, (!isStopped)? player.getCurrentPosition() : playlist.getStartTime());
		replyMessage(MSG_NOTIFY_TIME, bundle);
	}
	
	private void replyMessage(int what, Bundle bundle)
	{
		if(mReplyTo == null){
			return;
		}
		
		Message msg = Message.obtain(null, what);
		msg.setData(bundle);
		
		try{
			mReplyTo.send(msg);
		}catch(RemoteException e){
			e.printStackTrace();
		}
	}

	private void createNotificationChannel()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.channel_name);
			String description = getString(R.string.channel_description);

			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(description);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(channel);
		}
	}
	
	// 通知
	private void updateNotification()
	{
		if(isStopped){
			return;
		}
		
		Track track = playlist.getCurrentTrack();
		
		RemoteViews ntfViews = new RemoteViews(getPackageName(), R.layout.notification);
		
		ntfViews.setTextViewText(R.id.track_view, track.title);
		ntfViews.setTextViewText(R.id.album_view, track.getAlbumString());
		ntfViews.setTextViewText(R.id.artist_view, track.getArtistString());
		
		if(isPlaying){
			ntfViews.setImageViewResource(R.id.play_button, R.drawable.ic_pause_white_32dp);
		}else{
			ntfViews.setImageViewResource(R.id.play_button, R.drawable.ic_play_white_32dp);
		}

		Bitmap artwork = ArtworkCache.Large.getArtworkCache(track);
		if(artwork != null){
			ntfViews.setImageViewBitmap(R.id.artwork_view, artwork);
		}else{
			ntfViews.setImageViewResource(R.id.artwork_view, R.drawable.img_blank_big);
		}
		
		Intent intent;
		
		intent = new Intent(ACTION_PREV);
		PendingIntent prevIntent = PendingIntent.getService(this, R.id.prev_button, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.prev_button, prevIntent);
		
		intent = new Intent(ACTION_PLAY_PAUSE);
		PendingIntent playIntent = PendingIntent.getService(this, R.id.play_button, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.play_button, playIntent);
		
		intent = new Intent(ACTION_NEXT);
		PendingIntent nextIntent = PendingIntent.getService(this, R.id.next_button, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.next_button, nextIntent);
		
		intent = new Intent(ACTION_STOP);
		PendingIntent stopIntent = PendingIntent.getService(this, R.id.close_button, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.close_button, stopIntent);

		Intent[] intents = new Intent[2];
		intents[0] = new Intent(this, MainActivity.class);
		intents[0].setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intents[1] = new Intent(this, TrackActivity.class);
		intents[1].setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivities(this, 0, intents, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder ntfBuilder = new NotificationCompat.Builder(App.getContext())
		.setContent(ntfViews)
		.setContentIntent(contentIntent)
		.setSmallIcon((isPlaying) ? R.drawable.ic_play_white_32dp : R.drawable.ic_pause_white_32dp)
		.setDefaults(Notification.FLAG_FOREGROUND_SERVICE)
		.setChannelId(CHANNEL_ID);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
			ntfBuilder.setPriority(Notification.PRIORITY_DEFAULT);
		}

		startForeground(NOTIFICATION_ID, ntfBuilder.build());
	}
	
	// ロック画面での表示
	private void registerRemoteControl()
	{
		ComponentName eventReceiver = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(eventReceiver);

		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(eventReceiver);

		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
		remoteControlClient = new RemoteControlClient(mediaPendingIntent);
		audioManager.registerRemoteControlClient(remoteControlClient);

		remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		remoteControlClient.setTransportControlFlags
			(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_NEXT 
			 | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_STOP);
	}
	
	private void updateRemoteControl()
	{
		if(isStopped){
			return;
		}
		
		Track track = playlist.getCurrentTrack();
		
		remoteControlClient.editMetadata(true)
		.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, track.title)
		.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, track.getArtistString())
		.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, track.getAlbumString())
		.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, ArtworkCache.Large.getArtworkCache(track))
		.apply();
	}
	
	private void updateWidget()
	{
		Intent intent = new Intent(PlayerWidgetProvider.ACTION_UPDATE);
		intent.putExtra(KEY_TRACK, playlist.getCurrentTrack());
		intent.putExtra(KEY_PLAYING, isPlaying);
		sendBroadcast(intent);
	}
	
	public static Intent newPlayIntent(int playRange, TrackGroup item, int position, boolean shuffleStart)
	{
		Intent intent = new Intent(ACTION_PLAY);
		intent.putExtra("PLAY_RANGE", playRange);
		intent.putExtra("ITEM", item);
		intent.putExtra("POSITION", position);
		intent.putExtra("SHUFFLE_START", shuffleStart);
		intent.setPackage(App.PACKAGE);
		return intent;
	}
	
	
	private static class MyHandler extends Handler
	{
		private final WeakReference<PlayerService> mServiceRef;

		public MyHandler(PlayerService service)
		{
			mServiceRef = new WeakReference<>(service);
		}

		@Override
		public void handleMessage(Message msg)
		{
			PlayerService service = mServiceRef.get();
			if(service != null){
				service.handleMessage(msg);
			}
		}
	}
	
	private class ImageGetTask extends ArtworkCache.Large.ImageGetTask
	{
		public ImageGetTask(Track track)
		{
			super(track, null, true);
		}

		@Override
		protected void onPostExecute(Bitmap result)
		{
			super.onPostExecute(result);
			
			updateNotification();
			updateRemoteControl();
			updateWidget();
		}
	}
	
}
