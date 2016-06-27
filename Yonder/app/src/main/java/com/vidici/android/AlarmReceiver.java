package com.vidici.android;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

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
			GetNotificationsTask getNotificationsTask = new GetNotificationsTask();
			getNotificationsTask.execute(User.getId(context), "0");
		} else if (requestCode == 2) {
			PingTask pingTask = new PingTask();
			pingTask.execute();
		}
		wakeLock.release();
	}

	public void setNotificationAlarm(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		if (sharedPreferences.getString("push_notification", "on").equals("on")) {
			AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, AlarmReceiver.class);
			intent.putExtra("code", 1);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
			long initial = 1000 * 60 * 60; // Millisec * Second * Minute * Hour
			long delta = 1000 * 60 * 60 * 24; // Millisec * Second * Minute * Hour
			if (User.admin) {
				initial = 1000 * 60 * 15; // Millisec * Second * Minute * Hour
				delta = 1000 * 60 * 15; // Millisec * Second * Minute * Hour
			}
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + initial, delta, pendingIntent);
		}
	}

	public void setPingAlarm(Context context, boolean boot) { // ping and notification should be same task
		AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("code", 2);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, intent, 0);
		long delta = 1000 * 60 * 60 * 24; // Millisec * Second * Minute * Hour
		long triggerDelta;
		if (boot) {
			triggerDelta = 30000;
		} else {
			triggerDelta = delta;
		}
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + triggerDelta, delta, pendingIntent);
	}

	public void setAlarms(Context context, Boolean boot) {
			SharedPreferences sharedPreferences = context.getSharedPreferences(
					"com.vidici.android", Context.MODE_PRIVATE);
			int upgrade = sharedPreferences.getInt("upgrade", 0);
			int ban = sharedPreferences.getInt("ban", 0);
			AlarmReceiver alarmReceiver = new AlarmReceiver();
			if (ban == 0 && upgrade != 2) {
				alarmReceiver.setNotificationAlarm(context);
			} else {
				alarmReceiver.cancelNotificationAlarm(context);
			}
			alarmReceiver.setPingAlarm(context, boot);
	}

	public void cancelNotificationAlarm(Context context) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}


	class GetNotificationsTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				JSONObject response = gae.getNotifications(params[0], params[1]);
				return response;
			} catch (Exception e) {
				Logger.log(e);
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			try {
				Logger.log(Log.INFO, TAG, "Received push notification");
				SharedPreferences sharedPreferences = myContext.getSharedPreferences(
						"com.vidici.android", Context.MODE_PRIVATE);
				if (response != null && sharedPreferences.getString("push_notification", "on").equals("on")) {
					if (response.getString("success").equals("1")) {
						JSONArray notificationsArray = response.getJSONArray("notifications");
						int count = notificationsArray.length();
						if (count > 0) {
							NotificationManager notificationManager;
							notificationManager = (NotificationManager)myContext.getSystemService(Context.NOTIFICATION_SERVICE);

							// The PendingIntent to launch our activity if the user selects this notification
							Intent intent = new Intent(myContext, SplashActivity.class);
							intent.putExtra("notification", true);
							PendingIntent contentIntent = PendingIntent.getActivity(myContext, 0, intent, 0);

							String title;
							if (count == 1) {
								title = count + " New Notification";
							} else {
								title = count + " New Notifications";
							}

							Notification notification  = new Notification.Builder(myContext)
									.setContentTitle(title)
									.setContentText("Tap to read")
									.setSmallIcon(R.mipmap.ic_launcher)
									.setContentIntent(contentIntent)
									.setAutoCancel(true).build();

							// Send the notification.
							// We use a layout id because it is a unique number. We use it later to cancel.
							notificationManager.notify(R.mipmap.ic_launcher, notification);
							Logger.log(Log.INFO, TAG, "Showing push notification");
							Logger.trackEvent(myContext, "Notification", "Received Push Notification");
							cancelNotificationAlarm(myContext);
						}
					}
				}
			} catch (Exception e) {
				Logger.log(e);
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