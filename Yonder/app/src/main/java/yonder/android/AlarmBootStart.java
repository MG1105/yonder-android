package yonder.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmBootStart extends BroadcastReceiver
{
	AlarmReceiver alarmReceiver = new AlarmReceiver();
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) && !User.admin)
		{
			alarmReceiver.setAlarm(context);
		}
	}
}
