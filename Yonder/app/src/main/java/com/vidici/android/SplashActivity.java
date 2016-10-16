package com.vidici.android;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;

import org.json.JSONException;
import org.json.JSONObject;

import bolts.AppLinks;

public class SplashActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Logger.init(this);
		Logger.log(Log.INFO, TAG, "Creating Activity");
		setContentView(R.layout.activity_splash);
		mActivity = this;
		Video.setPaths(mActivity);
		VerifyUserTask verifyUserTask = new VerifyUserTask();
		verifyUserTask.execute();
//		User.setLocation(mActivity);
		Video.cleanup(Video.loadedDir);
		Video.cleanup(Video.uploadDir);

		AlarmReceiver alarmReceiver = new AlarmReceiver();
		alarmReceiver.setAlarms(this, false);

		Logger.startSession(this);

		// Facebook Invite
		FacebookSdk.sdkInitialize(this);
		AppLinkData.fetchDeferredAppLinkData(
				mActivity,
				new AppLinkData.CompletionHandler() {
					@Override
					public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
						if (appLinkData != null) {
							String promoCode = appLinkData.getPromotionCode();
							if (promoCode != null) {
//								SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
//										"com.vidici.android", Context.MODE_PRIVATE);
								Logger.trackEvent(mActivity, "Invite", "Opened app - FB");
//								sharedPreferences.edit().putBoolean("unlocked", true).apply();
//								Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
//								startActivity(intent);
								InvitationTask invitationTask = new InvitationTask();
								invitationTask.execute(promoCode);
							}
						}
					}
				});

		Thread timerThread = new Thread(){
			public void run(){
				try{
					sleep(500);
				}catch(InterruptedException e){
					e.printStackTrace();
				}finally{
					Bundle extras = getIntent().getExtras();
					SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
//					if (!sharedPreferences.getBoolean("unlocked", false)) {
//						Intent intent = new Intent(SplashActivity.this,LockActivity.class);
//						startActivity(intent);
//					} else
					if (extras != null && extras.getBoolean("notification", false)) {
						Logger.trackEvent(mActivity, "Notification", "Opened Push Notification");
						Intent intent = new Intent(SplashActivity.this,NotificationActivity.class);
						TaskStackBuilder.create(mActivity).addNextIntentWithParentStack(intent).startActivities();
					} else if (!sharedPreferences.getBoolean(WelcomeActivity.welcome_seen, false)) {
						Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
						startActivity(intent);
					} else {
						Intent intent = new Intent(SplashActivity.this,MainActivity.class);
						startActivity(intent);
					}
				}
			}
		};
		timerThread.start();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		finish();
	}

	// Verify User

	class VerifyUserTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(mActivity);
				Logger.log(Log.INFO, TAG, "Verifying user id " + userId);
				JSONObject response = gae.verifyUser(userId);

				if (response != null) {
					if (response.getString("success").equals("1")) {
						JSONObject userObject = response.getJSONObject("user");
						return userObject;
					} else { // server side failure
						Logger.log(new Exception("Server Side failure"));
						return null;
					}
				} else return null; // no internet
			} catch (Exception e) {
				Logger.log(e);;
				return null;
			}
		}

		protected void onPostExecute(JSONObject user) {
			if (user != null) {
				try {
					SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putInt("upgrade", user.getInt("upgrade")).apply();
					sharedPreferences.edit().putInt("ban", user.getInt("ban")).apply();
					sharedPreferences.edit().putInt("warn", user.getInt("warn")).apply();
				} catch (JSONException e) {
					Logger.log(e);
				}
			} else { // could also be server side failure

			}
		}
	}

	class InvitationTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(mActivity);
				Logger.log(Log.INFO, TAG, "Recording invitation open of user " + userId);
				JSONObject response = gae.invited(userId, params[0]);
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

					} else {
						Logger.log(new Exception("Server Side failure"));
					}
				} catch (JSONException e) {
					Logger.log(e);
				}
			}
		}
	}
}
