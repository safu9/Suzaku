package com.citrus.suzaku;

import android.content.*;
import android.view.*;

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
			
		switch(keyEvent.getKeyCode()){
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				context.startService(new Intent(PlayerService.ACTION_PLAY_PAUSE));
				break;
				
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				context.startService(new Intent(PlayerService.ACTION_NEXT));
				break;
				
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				context.startService(new Intent(PlayerService.ACTION_PREV));
				break;
				
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
			//	context.startService(new Intent(SuzakuPlayerService.ACTION_FAST_FORWARD));
				break;
				
			case KeyEvent.KEYCODE_MEDIA_REWIND:
			//	context.startService(new Intent(SuzakuPlayerService.ACTION_REWIND));
				break;
				
			case KeyEvent.KEYCODE_MEDIA_STOP:
				context.startService(new Intent(PlayerService.ACTION_STOP));
				break;
		}
	}
}
