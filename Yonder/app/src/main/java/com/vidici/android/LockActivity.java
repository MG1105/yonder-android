package com.vidici.android;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class LockActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;
	EditText codeText, emailText;
	TextView showWaitlist, showCode;
	Button unlock, joinWaitlist;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Logger.log(Log.INFO, TAG, "Creating Activity");
		setContentView(R.layout.activity_lock);
		mActivity = this;
		codeText = (EditText) findViewById(R.id.lock_textview_code);
		emailText = (EditText) findViewById(R.id.lock_textview_email);
		showWaitlist = (TextView) findViewById(R.id.textview_waitlist);
		showCode = (TextView) findViewById(R.id.textview_code);
		unlock = (Button) findViewById(R.id.lock_button_code);
		joinWaitlist = (Button) findViewById(R.id.lock_button_email);
		User.verify(mActivity);
		Logger.trackEvent(mActivity, "Lock", "View");

		unlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String code = codeText.getText().toString().trim();
				if (code.length()>0) {
					SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					int count = sharedPreferences.getInt("unlock_count", 0);
					if (count < 5) {
						UnlockTask unlockTask = new UnlockTask();
						unlockTask.execute(code);
					} else {
						Toast.makeText(mActivity, "Too many attempts", Toast.LENGTH_LONG).show();
					}
				}
				if (codeText != null) {
					codeText.setText("");
				}
				InputMethodManager inputManager = (InputMethodManager)
						getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
						getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		joinWaitlist.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailText.getText().toString().trim();
				if (email.length()>0) {
					JoinWaitlistTask joinWaitlistTask = new JoinWaitlistTask();
					joinWaitlistTask.execute(email);
				}
				if (emailText != null) {
					emailText.setText("");
				}
				InputMethodManager inputManager = (InputMethodManager)
						getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
						getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		showWaitlist.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout codeContainer = (LinearLayout) findViewById(R.id.lock_container_code);
				LinearLayout emailContainer = (LinearLayout) findViewById(R.id.lock_container_email);
				codeContainer.setVisibility(View.GONE);
				emailContainer.setVisibility(View.VISIBLE);
				Logger.trackEvent(mActivity, "Lock", "Click Waitlist");
			}
		});

		showCode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout codeContainer = (LinearLayout) findViewById(R.id.lock_container_code);
				LinearLayout emailContainer = (LinearLayout) findViewById(R.id.lock_container_email);
				codeContainer.setVisibility(View.VISIBLE);
				emailContainer.setVisibility(View.GONE);
				Logger.trackEvent(mActivity, "Lock", "Click Code");
			}
		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		finish();
	}

	class UnlockTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(mActivity);
				Logger.log(Log.INFO, TAG, "Unlocking app for user id " + userId);
				JSONObject response = gae.unlock(userId, params[0]);
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
						SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
								"com.vidici.android", Context.MODE_PRIVATE);
						int count = sharedPreferences.getInt("unlock_count", 0);
						sharedPreferences.edit().putInt("unlock_count", ++count).apply();
						if (response.getString("unlocked").equals("1")) {
							Logger.trackEvent(mActivity, "Lock", "Unlocked");
							sharedPreferences.edit().putBoolean("unlocked", true).apply();
							Intent intent = new Intent(LockActivity.this, WelcomeActivity.class);
							startActivity(intent);
						} else {
							Logger.trackEvent(mActivity, "Lock", "Failed Attempt");
							if (count == 4) {
								Toast.makeText(mActivity, 5-count+" attempt left", Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(mActivity, 5-count+" attempts left", Toast.LENGTH_LONG).show();
							}
							if (response.getString("unlocked").equals("0")) {

							} else if (response.getString("unlocked").equals("-1")) {
								Toast.makeText(mActivity, "This invite code has recently expired", Toast.LENGTH_LONG).show();
							} else if (response.getString("unlocked").equals("-2")) {
								Toast.makeText(mActivity, "This invite code has reached its cap and is no longer valid", Toast.LENGTH_LONG).show();
							}
						}
					} else { // server side failure
						Logger.log(new Exception("Server Side failure"));
					}
				} catch (JSONException e) {
					Logger.log(e);
				}
			}
		}
	}


	class JoinWaitlistTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(mActivity);
				Logger.log(Log.INFO, TAG, "Joining waiting list user id " + userId);
				JSONObject response = gae.joinWaitlist(userId, params[0]);
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
						Toast.makeText(mActivity, "You were successfully added to the waiting list :)", Toast.LENGTH_LONG).show();
						Logger.trackEvent(mActivity, "Lock", "Joined Waitlist");
					} else { // server side failure
						Logger.log(new Exception("Server Side failure"));
					}
				} catch (JSONException e) {
					Logger.log(e);
				}
			}
		}
	}

}
