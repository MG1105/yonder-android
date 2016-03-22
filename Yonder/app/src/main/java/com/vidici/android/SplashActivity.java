package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

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
		User.verify(mActivity);
		User.setLocation(mActivity);
		Video.cleanup(Video.loadedDir);
		Video.cleanup(Video.uploadDir);

		AlarmReceiver alarmReceiver = new AlarmReceiver();
		alarmReceiver.setAlarms(this, false);

		Logger.startSession(this);

		Thread timerThread = new Thread(){
			public void run(){
				try{
					sleep(250);
				}catch(InterruptedException e){
					e.printStackTrace();
				}finally{
					SharedPreferences sharedPreferences = SplashActivity.this.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					String welcome = sharedPreferences.getString("welcome_8", null);
					if (welcome == null) {
						Intent intent = new Intent(SplashActivity.this,WelcomeActivity.class);
						startActivity(intent);
					} else {
						Intent intent = new Intent(SplashActivity.this,ChannelActivity.class);
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
					if (user.getInt("warn") != 0 ) {
						Alert.showWarning(mActivity);
					} else if (user.getInt("upgrade") != 0) {
						Alert.forceUpgrade(mActivity, user.getInt("upgrade"));
					} else if (user.getInt("ban") != 0) {
						Alert.ban(mActivity, user.getInt("ban"));
					}
				} catch (JSONException e) {
					Logger.log(e);;
				}
			} else { // could also be server side failure
				Toast.makeText(mActivity, "Could not connect to the Internet. Please try again later", Toast.LENGTH_LONG).show();
				finish();
			}

		}
	}

}
