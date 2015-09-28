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

import com.crashlytics.android.Crashlytics;
import org.json.JSONObject;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

public class AlarmReceiver extends BroadcastReceiver
{
	private Context myContext;
	private final String TAG = "Log." + this.getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		myContext = context.getApplicationContext();
		Fabric.with(myContext, new Crashlytics());
		Crashlytics.setUserIdentifier(User.getId(myContext));
		Crashlytics.log(Log.INFO, TAG, "Received alarm");
		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmLock");
		wakeLock.acquire();
		GetFeedTask getFeed = new GetFeedTask();
		getFeed.execute();
		wakeLock.release();
	}

	public void setAlarm(Context context) {
		AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		long delta = 1000 * 60 * 60 * 24; // Millisec * Second * Minute * Hour
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delta, delta, pendingIntent);
	}

	public void cancelAlarm(Context context) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
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
				Crashlytics.log(Log.INFO, TAG, String.format("Getting new videos count for userId %s longitude %s latitude %s myVideosOnly %s",
						userId, longitude, latitude, false));
				JSONObject response = gae.getFeed(userId, longitude, latitude, false, true);
				return response;
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
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
									.setSmallIcon(R.mipmap.ic_launcher)
									.setContentIntent(contentIntent)
									.setAutoCancel(true).build();

							// Send the notification.
							// We use a layout id because it is a unique number. We use it later to cancel.
							notificationManager.notify(R.mipmap.ic_launcher, notification);
							cancelAlarm(myContext);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					Crashlytics.logException(e);;
				}
			}
		}
	}
}
