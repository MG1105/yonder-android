package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class WelcomeActivity extends Activity {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	private GestureDetectorCompat mDetector;
	private int tap = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.log(Log.INFO, TAG, "Creating Activity");
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_welcome_1);
		mDetector = new GestureDetectorCompat(this, new MyGestureListener());

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

	// Handle touch
	@Override
	public boolean onTouchEvent(MotionEvent event){
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			if (tap == 0) {
//				setContentView(R.layout.activity_welcome_2);
				tap = 1;
			} else if (tap == 1) {
				SharedPreferences sharedPreferences = WelcomeActivity.this.getSharedPreferences(
						"com.vidici.android", Context.MODE_PRIVATE);
				sharedPreferences.edit().putString("welcome_8", "yes").apply();
				Intent intent = new Intent(WelcomeActivity.this,CameraPreviewActivity.class);
				startActivity(intent);
				finish();
			}

			return true;
		}
	}
}
