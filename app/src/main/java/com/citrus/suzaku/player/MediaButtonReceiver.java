package com.citrus.suzaku.player;

import android.content.*;
import android.support.v4.content.ContextCompat;
import android.view.*;

import com.citrus.suzaku.App;

public class MediaButtonReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		
		if(keyEvent.getAction() != KeyEvent.ACTION_DOWN){
			return;
		}
		
		App.logd("MBR Received Key Code : " + keyEvent.getKeyCode());

		Intent service;
		switch(keyEvent.getKeyCode()){
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				service = new Intent(PlayerService.ACTION_PLAY_PAUSE);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
				break;
				
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				service = new Intent(PlayerService.ACTION_NEXT);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
				break;
				
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				service = new Intent(PlayerService.ACTION_PREV);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
				break;
				
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
/*				service = new Intent(PlayerService.ACTION_FAST_FORWARD);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
*/				break;
				
			case KeyEvent.KEYCODE_MEDIA_REWIND:
/*				service = new Intent(PlayerService.ACTION_REWIND);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
*/				break;
				
			case KeyEvent.KEYCODE_MEDIA_STOP:
				service = new Intent(PlayerService.ACTION_STOP);
				service.setPackage(App.PACKAGE);
				ContextCompat.startForegroundService(context, service);
				break;
		}
	}
}
