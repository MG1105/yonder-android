package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class WelcomeActivity extends Activity {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	private GestureDetectorCompat mDetector;
	Button collegeButton, joinWaitlist;
	TextView showRequest, showApp;
	EditText emailText, collegeText;
	Activity mActivity;
	static String welcome_seen = "seen_welcome_14";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.log(Log.INFO, TAG, "Creating Activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		mActivity = this;
		Logger.trackEvent(mActivity, "Welcome", "View");

		collegeButton = (Button) findViewById(R.id.welcome_button_college);
		joinWaitlist = (Button) findViewById(R.id.welcome_button_request);
		showRequest = (TextView) findViewById(R.id.welcome_textview_waitlist);
		showApp = (TextView) findViewById(R.id.welcome_textview_continue);
		emailText = (EditText) findViewById(R.id.welcome_edittext_email);
		collegeText = (EditText) findViewById(R.id.welcome_edittext_college);


		collegeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences sharedPreferences = getSharedPreferences(
						"com.vidici.android", Context.MODE_PRIVATE);
				sharedPreferences.edit().putBoolean(welcome_seen, true).apply();
				Spinner college = (Spinner) findViewById(R.id.activity_welcome_spinner_college);
				sharedPreferences.edit().putString("college", college.getSelectedItem().toString()).apply();
				Intent intent = new Intent(WelcomeActivity.this,MainActivity.class);
				startActivity(intent);
				finish();
				Logger.trackEvent(mActivity, "Welcome", "Picked College");
			}
		});

		showRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout out = (LinearLayout) findViewById(R.id.welcome_container_college);
				LinearLayout in = (LinearLayout) findViewById(R.id.welcome_container_request);
				out.setVisibility(View.GONE);
				in.setVisibility(View.VISIBLE);
				Logger.trackEvent(mActivity, "Welcome", "Show Request");
			}
		});

		joinWaitlist.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailText.getText().toString().trim();
				String college = collegeText.getText().toString().trim();

				if (email.length() > 0 && college.length() > 0) {
					Logger.trackEvent(mActivity, "Welcome", "Request College");
					JoinWaitlistTask joinWaitlistTask = new JoinWaitlistTask();
					joinWaitlistTask.execute(email, college);
					if (emailText != null && collegeText != null) {
						emailText.setText("");
						collegeText.setText("");
					}
					SharedPreferences sharedPreferences = getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putBoolean(welcome_seen, true).apply();
					sharedPreferences.edit().putString("college_request", college).apply();
					InputMethodManager inputManager = (InputMethodManager)
							getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
							getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				} else {
					Toast.makeText(mActivity, "Invalid input", Toast.LENGTH_LONG).show();
				}
			}
		});

		showApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.trackEvent(mActivity, "Welcome", "Show App");
				Intent intent = new Intent(WelcomeActivity.this,MainActivity.class);
				startActivity(intent);
				finish();
			}
		});
	}

	public void onResume() {
		super.onResume();
		Logger.log(Log.INFO, TAG, "Resuming Activity");
		Logger.fbActivate(this, true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		Logger.fbActivate(this, false);
	}


	class JoinWaitlistTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				String userId = User.getId(mActivity);
				Logger.log(Log.INFO, TAG, "Joining waiting list user id " + userId);
				JSONObject response = gae.joinWaitlist(userId, params[0], params[1]);
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
						LinearLayout in = (LinearLayout) findViewById(R.id.welcome_container_requested);
						LinearLayout out = (LinearLayout) findViewById(R.id.welcome_container_request);
						out.setVisibility(View.GONE);
						in.setVisibility(View.VISIBLE);
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
