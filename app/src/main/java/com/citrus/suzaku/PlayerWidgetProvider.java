package com.citrus.suzaku;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.widget.*;


public class PlayerWidgetProvider extends AppWidgetProvider
{
	public static final String ACTION_UPDATE = "Citrus.suzaku.action.ACTION_UPDATE_WIDGET";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		updateWidget(context, null, false);
	}
	
	private void updateWidget(Context context, Track track, boolean isPlaying)
	{
		RemoteViews views = createView(track, isPlaying);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		
		ComponentName widget = new ComponentName(context, PlayerWidgetProvider.class);
		manager.updateAppWidget(widget, views);
		
	/*?	// なぜか更新されない
		if(appWidgetIds == null){
			return;
		}
		
		for(int i = 0; i < appWidgetIds.length; i++){
			manager.updateAppWidget(appWidgetIds[i], views);
		}
	*/
	}
	
	private RemoteViews createView(Track track, boolean isPlaying)
	{
		Context context = App.getContext();

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

		Intent intent;

		intent = new Intent(PlayerService.ACTION_PREV);
		PendingIntent prevIntent = PendingIntent.getService(context, R.id.prevButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.prevButton, prevIntent);

		intent = new Intent(PlayerService.ACTION_PLAY_PAUSE);
		PendingIntent playIntent = PendingIntent.getService(context, R.id.playButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.playButton, playIntent);

		intent = new Intent(PlayerService.ACTION_NEXT);
		PendingIntent nextIntent = PendingIntent.getService(context, R.id.nextButton, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.nextButton, nextIntent);
		
		// API LV 11
		Intent[] intents = new Intent[2];
		intents[0] = new Intent(context, MainActivity.class);
		intents[0].setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intents[1] = new Intent(context, TrackActivity.class);
		intents[1].setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivities(context, 0, intents, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.contentButton, contentIntent);

		if(track != null){
			views.setTextViewText(R.id.track, track.title);
			views.setTextViewText(R.id.album, track.getAlbumString());
			views.setTextViewText(R.id.artist, track.getArtistString());

			Bitmap artwork = ArtworkCache.Large.getArtworkCache(track);
			if(artwork != null){
				views.setImageViewBitmap(R.id.artwork, artwork);				// NPE 回避
			}else{
				views.setImageViewResource(R.id.artwork, R.drawable.big_blank);
			}
		
		}else{
			views.setTextViewText(R.id.track, "Suzaku");
			views.setTextViewText(R.id.album, "");
			views.setTextViewText(R.id.artist, "");

			views.setImageViewResource(R.id.artwork, R.drawable.big_blank);		// NPE 回避
		}
		
		if(isPlaying){
			views.setImageViewResource(R.id.playButton, R.drawable.ic_pause);
		}else{
			views.setImageViewResource(R.id.playButton, R.drawable.ic_play);
		}

		return views;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		switch(intent.getAction()){
			case ACTION_UPDATE:
				Track track = (Track)intent.getSerializableExtra(PlayerService.KEY_TRACK);
				boolean isPlaying = intent.getBooleanExtra(PlayerService.KEY_PLAYING, false);
				updateWidget(context, track, isPlaying);
				break;
				
			default:
				super.onReceive(context, intent);
				break;
		}
	}
	
}
