package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class ProfileActivity extends AppCompatActivity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	static boolean signedUp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		SharedPreferences sharedPreferences = getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("profileId")) {
			Logger.log(Log.INFO, TAG, "Viewing Profile");
			ProfileFragment fragment = new ProfileFragment();
			Bundle bundle = new Bundle(1);
			bundle.putString("user_id", getIntent().getExtras().getString("profileId"));
			fragment.setArguments(bundle);
			getSupportFragmentManager().beginTransaction().add(R.id.activity_profile, fragment).commit();
		} else if (!sharedPreferences.getBoolean("logged_in", false)) {
			Logger.log(Log.INFO, TAG, "Viewing Login");
			LoginFragment fragment = new LoginFragment();
			Bundle bundle = new Bundle(1);
			bundle.putString("activity", "");
			fragment.setArguments(bundle);
			getSupportFragmentManager().beginTransaction().add(R.id.activity_profile, fragment).commit();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		if (signedUp) { // handles login screen is cached case
			try {
				MainActivity.mViewPager.setAdapter(MainActivity.mainPagerAdapter);
				MainActivity.mViewPager.setCurrentItem(1);
			} catch (RuntimeException e) {
				Logger.log(e);
			}
		}
	}
}
