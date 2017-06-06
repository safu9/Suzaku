package com.citrus.suzaku;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.widget.*;
import java.io.*;


public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener
{
	private static final int NOTIFICATION_ID = 1;
	
	// From Activity, RemoteViews & RemoteControll
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
	
	// To Activity
	public static final int MSG_NOTIFY_TRACK = 10;
	public static final int MSG_NOTIFY_STATE = 11;
	public static final int MSG_NOTIFY_TIME = 12;
	public static final int MSG_STOPPED = 13;
	
	// Intent Extra Key
	public static final String KEY_TRACK = "TRACK";
	public static final String KEY_POSITION = "POSITION";
	public static final String KEY_COUNT = "COUNT";
	public static final String KEY_LOOPMODE = "LOOP";
	public static final String KEY_SHUFFLEMODE = "SHUFFLE";
	public static final String KEY_PLAYING = "PLAYING";
	public static final String KEY_STOPPED = "STOPPED";
	public static final String KEY_TIME = "TIME";
	
	private final Messenger mMessenger = new Messenger(new MyHandler());
	private Messenger mReplyTo;
	
	private PlaylistManager playlist;				// 保存される情報

	private MediaPlayer player;
	private boolean isPlaying = false;				// Audio Focus の一時中断も加味
	private boolean isStopped = true;				// MediaPlayer

	private NotificationManager manager;
	private AudioManager audioManager;
	private RemoteControlClient remoteControlClient;
	
	private int startId;

	
	@Override
	public void onCreate()
	{
		manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		ArtworkCache.initialize();
		
		playlist = new PlaylistManager();
		playlist.load();
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

				if (!setupPlayer()) {
					notifyTrack();
					stopSong();
				} else {
					playSong();
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

		player.setWakeMode(App.getContext(), PowerManager.PARTIAL_WAKE_LOCK);
	}
	
	private void nextSong()
	{
		if(playlist.forwardTrack() == null){
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
			
			isPlaying = false;
		}else{
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
	
	private void updateArtwork()
	{
		Track track = playlist.getCurrentTrack();
		
		if(track == null || !ArtworkCache.isCorrectHash(track.artworkHash)){
			return;
		}
		
		if(!track.artworkHash.equals(ArtworkCache.Large.getArtworkHash())){
			new ImageGetTask(track).execute();
		}
		
	}
	
	// Reply to Activity or Fragment
	
	private void notifyTrack()
	{
		playlist.save();

		Bundle bundle = new Bundle();
		bundle.putSerializable(KEY_TRACK, playlist.getCurrentTrack());
		bundle.putInt(KEY_POSITION, playlist.getCurrentPosition());
		bundle.putInt(KEY_COUNT, playlist.getSize());
		bundle.putInt(KEY_LOOPMODE, playlist.getLoopMode());
		bundle.putBoolean(KEY_SHUFFLEMODE, playlist.getShuffleMode());
		replyMessage(MSG_NOTIFY_TRACK, bundle);
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
	
	// 通知
	private void updateNotification()
	{
		if(isStopped){
			return;
		}
		
		Track track = playlist.getCurrentTrack();
		
		RemoteViews ntfViews = new RemoteViews(getPackageName(), R.layout.notification);
		
		ntfViews.setTextViewText(R.id.track, track.title);
		ntfViews.setTextViewText(R.id.album, track.getAlbumString());
		ntfViews.setTextViewText(R.id.artist, track.getArtistString());
		
		if(isPlaying){
			ntfViews.setImageViewResource(R.id.playButton, R.drawable.ic_pause);
		}else{
			ntfViews.setImageViewResource(R.id.playButton, R.drawable.ic_play);
		}

		Bitmap artwork = ArtworkCache.Large.getArtworkCache(track);
		if(artwork != null){
			ntfViews.setImageViewBitmap(R.id.artwork, artwork);
		}else{
			ntfViews.setImageViewResource(R.id.artwork, R.drawable.big_blank);
		}
		
		Intent intent;
		
		intent = new Intent(ACTION_PREV);
		PendingIntent prevIntent = PendingIntent.getService(this, R.id.prevButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.prevButton, prevIntent);
		
		intent = new Intent(ACTION_PLAY_PAUSE);
		PendingIntent playIntent = PendingIntent.getService(this, R.id.playButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.playButton, playIntent);
		
		intent = new Intent(ACTION_NEXT);
		PendingIntent nextIntent = PendingIntent.getService(this, R.id.nextButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.nextButton, nextIntent);
		
		intent = new Intent(ACTION_STOP);
		PendingIntent stopIntent = PendingIntent.getService(this, R.id.closeButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ntfViews.setOnClickPendingIntent(R.id.closeButton, stopIntent);
		
/*		// REcreate MainActivity!!! (without saving instantState)
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(TrackActivity.class);
		stackBuilder.addNextIntent(new Intent(this, TrackActivity.class));
		PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
*/
		// API LV 11
		Intent[] intents = new Intent[2];
		intents[0] = new Intent(this, MainActivity.class);
		intents[0].setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intents[1] = new Intent(this, TrackActivity.class);
		intents[1].setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivities(this, 0, intents, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder ntfBuilder = new NotificationCompat.Builder(App.getContext())
		.setContent(ntfViews)
		.setContentIntent(contentIntent)
		.setSmallIcon((isPlaying) ? R.drawable.ic_play : R.drawable.ic_pause)
		.setDefaults(Notification.FLAG_FOREGROUND_SERVICE)
		.setPriority(Notification.PRIORITY_HIGH);
		
		startForeground(NOTIFICATION_ID, ntfBuilder.getNotification());
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
	
	
	private class MyHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what){
				case MSG_REQUEST_INFO:
					App.logd("PS Received : MSG_REQUEST_INFO");
					mReplyTo = msg.replyTo;
					notifyTrack();
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
					notifyTrack();
					break;
				case MSG_SWITCH_SHUFFLEMODE:
					App.logd("PS Received : MSG_SWITCH_SHUFFLEMODE");
					playlist.switchShuffleMode();
					notifyTrack();
					break;
				default:
					super.handleMessage(msg);
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