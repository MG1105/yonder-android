package yonder.android;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver
{
	private Context myContext;
	private final String TAG = "Log." + this.getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		myContext = context.getApplicationContext();
		Logger.init(context);
		Logger.log(Log.INFO, TAG, "Received alarm");
		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmLock");
		wakeLock.acquire();
		int requestCode = intent.getExtras().getInt("code");
		if (requestCode == 1) {
			GetFeedTask getFeed = new GetFeedTask();
			getFeed.execute();
		} else if (requestCode == 2) {
			PingTask pingTask = new PingTask();
			pingTask.execute();
		}
		wakeLock.release();
	}

	public void setNotificationAlarm(Context context) {
		AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("code", 1);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
		long delta = 1000 * 60 * 60 * 72; // Millisec * Second * Minute * Hour
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delta, delta, pendingIntent);
	}

	public void setPingAlarm(Context context, boolean boot) {
		AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("code", 2);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, intent, 0);
		long delta = 1000 * 60 * 60 * 12; // Millisec * Second * Minute * Hour
		long triggerDelta;
		if (boot) {
			triggerDelta = 30000;
		} else {
			triggerDelta = delta;
		}
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + triggerDelta, delta, pendingIntent);
	}

	public void setBootAlarm(Context context) {
		setNotificationAlarm(context);
		setPingAlarm(context, true);
	}

	public void cancelNotificationAlarm(Context context) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}


	class GetFeedTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				ArrayList<String> location = User.getLocation(myContext);
				String longitude;
				String latitude;
				if (location != null) {
					longitude = location.get(0);
					latitude = location.get(1);
				} else { // Default SJSU
					longitude = "-121.881072222222";
					latitude = "37.335187777777";
				}
				AppEngine gae = new AppEngine();
				String userId = User.getId(myContext);
				Logger.log(Log.INFO, TAG, String.format("Getting new videos count for userId %s longitude %s latitude %s myVideosOnly %s",
						userId, longitude, latitude, false));
				JSONObject response = gae.getFeed(userId, longitude, latitude, false, true);
				return response;
			} catch (Exception e) {
				Logger.log(e);
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			if (response != null) {
				try {
					if (response.getString("success").equals("1")) {
						String count = response.getString("videos");
						if (Integer.parseInt(count) > 0) {
							NotificationManager notificationManager;
							notificationManager = (NotificationManager)myContext.getSystemService(Context.NOTIFICATION_SERVICE);

							// The PendingIntent to launch our activity if the user selects this notification
							PendingIntent contentIntent = PendingIntent.getActivity(myContext, 0, new Intent(myContext, SplashActivity.class), 0);

							Notification notification  = new Notification.Builder(myContext)
									.setContentTitle("New Yondors near you")
									.setContentText("Tap to open Yondor")
									.setSmallIcon(R.mipmap.ic_launcher)
									.setContentIntent(contentIntent)
									.setAutoCancel(true).build();

							// Send the notification.
							// We use a layout id because it is a unique number. We use it later to cancel.
							notificationManager.notify(R.mipmap.ic_launcher, notification);
							cancelNotificationAlarm(myContext);
						}
					}
				} catch (Exception e) {
					Logger.log(e);
				}
			}
		}
	}

	class PingTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(myContext);
				Logger.log(Log.INFO, TAG, String.format("Pinging home userId %s",
						userId));
				JSONObject response = gae.pingHome(userId);
				return response;
			} catch (Exception e) {
				Logger.log(e);
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			if (response != null) {
				try {
					if (response.getString("success").equals("1")) {
						Logger.log(Log.INFO, TAG, "Pinged home");
					}
				} catch (Exception e) {
					Logger.log(e);
				}
			}
		}
	}
}