package com.citrus.suzaku;

import android.content.*;

public abstract class PlaylistBroadcastReceiver extends BroadcastReceiver
{
	@Override
	public abstract void onReceive(Context c, Intent i);

	public IntentFilter createIntentFilter()
	{
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MusicDBService.ACTION_PLAYLIST_CHANGED);

		return intentFilter;
	}
}

