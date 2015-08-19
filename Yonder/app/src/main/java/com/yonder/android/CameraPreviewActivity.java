package com.yonder.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CameraPreviewActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mediaRecorder;
	private Button capture, switchCamera, feedButton;
	private Activity mActivity;
	private RelativeLayout cameraPreview;
	private boolean cameraFront = false;
	String userId;
	boolean recording;
	boolean clickOnly;
//	private GestureDetectorCompat mDetector;
    CountDownTimer timer;

	// Camera Preview

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Fabric.with(this, new Crashlytics());
		CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
		Fabric.with(this, new Crashlytics.Builder().core(core).build());
		userId = User.getId(this);
		Crashlytics.setUserIdentifier(userId);

		setContentView(R.layout.activity_camera_preview);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mActivity = this;
//		mDetector = new GestureDetectorCompat(this, new MyGestureListener());
		initialize();
		Video.setPaths(mActivity);
		VerifyUserTask verifyUserTask = new VerifyUserTask();
		verifyUserTask.execute();
		User.verify(mActivity);
		User.setLocation(mActivity);
		Video.cleanup(Video.loadedDir);
		Video.cleanup(Video.uploadDir);
	}

	public void onResume() {
		super.onResume();
		if (!hasCamera(mActivity)) {
			Toast.makeText(mActivity, "Sorry, we could not find your camera. Exiting...", Toast.LENGTH_LONG).show();
			Crashlytics.logException(new Exception("No Camera found"));
			finish();
		}
		if (mCamera == null) {
			// if the front facing camera does not exist
			if (findFrontFacingCamera() < 0) {
				//Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
				switchCamera.setVisibility(View.GONE);
				mCamera = Camera.open(findBackFacingCamera());
			} else {
				mCamera = Camera.open(findFrontFacingCamera());
			}
			mPreview.refreshCamera(mCamera);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
        if (recording) {
            stopRecording();
	        capture.setBackgroundResource(R.drawable.ic_record);
        }
		releaseCamera();
	}

	public void initialize() {
		cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);

		mPreview = new CameraPreview(mActivity, mCamera);
		cameraPreview.addView(mPreview);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnTouchListener(recordListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);

		feedButton = (Button) findViewById(R.id.button_feed);
		feedButton.setOnClickListener(feedButtonListener);
	}

	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// get the number of cameras
			if (!recording) {
				Crashlytics.log(Log.INFO, TAG, "Request to switch cameras");
				int camerasNumber = Camera.getNumberOfCameras();
				if (camerasNumber > 1) {
					// release the old camera instance
					// switch camera, from the front and the back and vice versa
					releaseCamera();
					chooseCamera();
				} else {
					Toast.makeText(mActivity, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(mActivity, "Sorry, cannot switch cameras while recording!", Toast.LENGTH_LONG).show();
			}
		}
	};

	OnClickListener feedButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(mActivity, LoadFeedActivity.class);
			startActivity(intent);
		}
	};

	public void chooseCamera() {
		// if the camera preview is the front
		if (cameraFront) {
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// refresh the preview
				mCamera = Camera.open(cameraId);
				mPreview.refreshCamera(mCamera);
			}
		} else {
			int cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// refresh the preview
				mCamera = Camera.open(cameraId);
				mPreview.refreshCamera(mCamera);
			}
		}
	}

	private int findFrontFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
				cameraFront = true;
				break;
			}
		}
		return cameraId;
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		// Search for the back facing camera
		// get the number of cameras
		int numberOfCameras = Camera.getNumberOfCameras();
		// for every camera check
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				cameraFront = false;
				break;
			}
		}
		return cameraId;
	}

	private boolean hasCamera(Context context) {
		// check if the device has camera
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	// Video Recording

	OnClickListener clickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (clickOnly)
			clickOnly = true;
		}
	};

	View.OnTouchListener recordListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN && !clickOnly){
				Crashlytics.log(Log.INFO, TAG, "Start recording");
				if (!prepareMediaRecorder()) {
					Toast.makeText(CameraPreviewActivity.this, "Failed Recording!", Toast.LENGTH_LONG).show();
					mActivity.recreate();
				}
				try {
					capture.setBackgroundResource(R.drawable.ic_stop);
					mediaRecorder.start();
					timer = new CountDownTimer(10000, 1000) {
						TextView counter = (TextView)findViewById(R.id.recording_counter);
						public void onTick(long millisUntilFinished) {
							counter.setText(""+millisUntilFinished / 1000);
						}
						public void onFinish() {
							if (recording) {
								stopRecording();
								capture.setBackgroundResource(R.drawable.ic_record);
							}
							counter.setText("10");
						}
					}.start();
				} catch (Exception e) {
					Toast.makeText(CameraPreviewActivity.this, "Failed Recording!", Toast.LENGTH_LONG).show();
					Crashlytics.logException(new Exception("Failed Recording"));
					mActivity.recreate();
				}
				return true;
			}
			else if(event.getAction() == MotionEvent.ACTION_UP){
				if (recording) {
					Crashlytics.log(Log.INFO, TAG, "Stop recording");
					stopRecording();
					capture.setBackgroundResource(R.drawable.ic_record);
					if (timer != null) {
						timer.cancel();
						TextView counter = (TextView)findViewById(R.id.recording_counter);
						counter.setText("10");
					}
				}
				return true;
			}
			else
				return false;
		}
	};

    protected void stopRecording() { // Exception
        // stop recording and release camera
        mediaRecorder.stop(); // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        Toast.makeText(CameraPreviewActivity.this, "Yonder Captured!", Toast.LENGTH_LONG).show();
        recording = false;
        Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
        startActivity(intent);
    }

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			mCamera.lock(); // lock camera for later use
		}
	}

	private boolean prepareMediaRecorder() {

		mediaRecorder = new MediaRecorder();

		mCamera.unlock();
		mediaRecorder.setCamera(mCamera);

		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        String uploadPath = Video.uploadDir.getAbsolutePath() + "/captured.mp4";
		mediaRecorder.setOutputFile(uploadPath);
		mediaRecorder.setMaxDuration(15000); // Set max duration
		if (cameraFront) {
			mediaRecorder.setOrientationHint(270);
		} else {
			mediaRecorder.setOrientationHint(90);
		}

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			e.printStackTrace();
			Crashlytics.logException(e);;
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			e.printStackTrace();
			Crashlytics.logException(e);;
			return false;
		}
		recording = true;
		return true;

	}

	// Verify User

	class VerifyUserTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				Crashlytics.log(Log.INFO, TAG, "Verifying user id " + userId);
				JSONObject response = gae.verifyUser(userId);
				if (response != null) {
					if (response.getString("success").equals("1")) {
						JSONObject userObject = response.getJSONObject("user");
						return userObject;
					} else { // server side failure
						Crashlytics.logException(new Exception("Server Side failure"));
						return null;
					}
				} else return null; // no internet
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
				return null;
			}
		}

		protected void onPostExecute(JSONObject user) {
			if (user != null) {
				try {
					SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
							"com.yonder.android", Context.MODE_PRIVATE);
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
					e.printStackTrace();
					Crashlytics.logException(e);;
				}
			} else { // could also be server side failure
				Toast.makeText(mActivity, "Could not connect to the Internet. Please try again later. Exiting...", Toast.LENGTH_LONG).show();
				finish();
			}

		}
	}

	// Handle Touch

//	@Override
//	public boolean onTouchEvent(MotionEvent event){
//		this.mDetector.onTouchEvent(event);
//		return super.onTouchEvent(event);
//	}
//
//	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
//		private static final String DEBUG_TAG = "Gestures";
//
//		@Override
//		public boolean onDown(MotionEvent event) {
//			Log.d(DEBUG_TAG, "onDown: " + event.toString());
//			return true;
//		}
//
//		@Override
//		public boolean onFling(MotionEvent event1, MotionEvent event2,
//							   float velocityX, float velocityY) {
//			Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
//			Intent intent = new Intent(mActivity, LoadFeedActivity.class);
//            startActivity(intent);
//			return true;
//		}
//	}

}