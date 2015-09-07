package yonder.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class SplashActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());
		Crashlytics.setUserIdentifier(User.getId(this));
		Crashlytics.log(Log.INFO, TAG, "Creating Activity");
		setContentView(R.layout.activity_splash);

		Thread timerThread = new Thread(){
			public void run(){
				try{
					sleep(3000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}finally{
					SharedPreferences sharedPreferences = SplashActivity.this.getSharedPreferences(
							"yonder.android", Context.MODE_PRIVATE);
					String welcome = sharedPreferences.getString("welcome", null);
					if (welcome == null) {
						Intent intent = new Intent(SplashActivity.this,WelcomeActivity.class);
						startActivity(intent);
					} else {
						Intent intent = new Intent(SplashActivity.this,CameraPreviewActivity.class);
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
		Crashlytics.log(Log.INFO, TAG, "Pausing Activity");
		finish();
	}

}
