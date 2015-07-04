package com.yonder.android;

import java.io.IOException;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraPreviewActivity extends Activity {
	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mediaRecorder;
	private Button capture, switchCamera;
	private Activity mActivity;
	private RelativeLayout cameraPreview;
	private boolean cameraFront = false;
	private GestureDetectorCompat mDetector;

	// Camera Preview

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_preview);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mActivity = this;
		mDetector = new GestureDetectorCompat(this, new MyGestureListener());
		initialize();
	}

	public void onResume() {
		super.onResume();
		if (!hasCamera(mActivity)) {
			Toast toast = Toast.makeText(mActivity, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
			toast.show();
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
	protected void onStop() {
		super.onStop();
		// when on Pause, release camera in order to be used from other
		// applications
		releaseCamera();
	}

	public void initialize() {
		cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);

		mPreview = new CameraPreview(mActivity, mCamera);
		cameraPreview.addView(mPreview);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnClickListener(captureListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);
	}

	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// get the number of cameras
			if (!recording) {
				int camerasNumber = Camera.getNumberOfCameras();
				if (camerasNumber > 1) {
					// release the old camera instance
					// switch camera, from the front and the back and vice versa

					releaseCamera();
					chooseCamera();
				} else {
					Toast toast = Toast.makeText(mActivity, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
					toast.show();
				}
			}
		}
	};

	public void chooseCamera() {
		// if the camera preview is the front
		if (cameraFront) {
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// set a picture callback
				// refresh the preview

				mCamera = Camera.open(cameraId);
				// mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera);
			}
		} else {
			int cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// set a picture callback
				// refresh the preview

				mCamera = Camera.open(cameraId);
				// mPicture = getPictureCallback();
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
	boolean recording = false;
	OnClickListener captureListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (recording) {
				// stop recording and release camera
				mediaRecorder.stop(); // stop the recording
				releaseMediaRecorder(); // release the MediaRecorder object
				Toast.makeText(CameraPreviewActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
				recording = false;
				Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
				startActivity(intent);
			} else {
				if (!prepareMediaRecorder()) {
					Toast.makeText(CameraPreviewActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
					finish();
				}
				// work on UiThread for better performance
				runOnUiThread(new Runnable() {
					public void run() {
						// If there are stories, add them to the table

						try {
							mediaRecorder.start();
						} catch (final Exception ex) {
							// Log.i("---","Exception in thread");
						}
					}
				});

				recording = true;
			}
		}
	};

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

		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P ));

		mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");
		mediaRecorder.setMaxDuration(7000); // Set max duration 60 sec.
		if (cameraFront) {
			mediaRecorder.setOrientationHint(270);
		} else {
			mediaRecorder.setOrientationHint(90);
		}

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	// Handle Touch

	@Override
	public boolean onTouchEvent(MotionEvent event){
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		private static final String DEBUG_TAG = "Gestures";

		@Override
		public boolean onDown(MotionEvent event) {
			Log.d(DEBUG_TAG, "onDown: " + event.toString());
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
							   float velocityX, float velocityY) {
			Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
			Intent intent = new Intent(mActivity, LoadFeedActivity.class);
            startActivity(intent);
			return true;
		}
	}

}