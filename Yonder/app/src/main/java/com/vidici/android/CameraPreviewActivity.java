package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class CameraPreviewActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mediaRecorder;
	private Button switchCamera, addVideoButton;
	private Activity mActivity;
	private RelativeLayout cameraPreview;
	private boolean cameraFront = false;
	String userId;
	boolean recording;
	TextView counter;
//	private GestureDetectorCompat mDetector;
    CountDownTimer timer;
	String videoId, channelId, channelName;
	int cameraId;

	// Camera Preview

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		userId = User.getId(this);
		Logger.log(Log.INFO, TAG, "Creating Activity");

		setContentView(R.layout.activity_camera_preview);
		mActivity = this;
//		mDetector = new GestureDetectorCompat(this, new MyGestureListener());
		initialize();
		channelId = getIntent().getExtras().getString("channelId");
		channelName = getIntent().getExtras().getString("channelName");
		TextView channel= (TextView)findViewById(R.id.textView_camera_channel);
		channel.setText("#"+channelName);
		View v = findViewById(R.id.camera_preview);
		v.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, final MotionEvent event){
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					Logger.log(Log.INFO, TAG, "Start recording");
					Logger.trackEvent(mActivity, "Upload", "Record Video");
					if (!prepareMediaRecorder()) {
						Toast.makeText(CameraPreviewActivity.this, "Unexpected error while recording", Toast.LENGTH_LONG).show();
						mActivity.finish();
						return true;
					}
					try {
						mediaRecorder.start();
						counter = (TextView)findViewById(R.id.recording_counter);
//						addVideoButton.setVisibility(View.INVISIBLE);
//						switchCamera.setVisibility(View.INVISIBLE);
						counter.setVisibility(View.VISIBLE);
						counter.setText("9");
						timer = new CountDownTimer(10000, 1000) {

							public void onTick(long millisUntilFinished) {
								counter.setText("" + millisUntilFinished / 1000);
							}
							public void onFinish() {
								if (recording) {
									stopRecording();
								}
								counter.setVisibility(View.INVISIBLE);
//								addVideoButton.setVisibility(View.VISIBLE);
//								switchCamera.setVisibility(View.VISIBLE);
								counter.setText("10");
							}
						}.start();
					} catch (Exception e) {
						Toast.makeText(CameraPreviewActivity.this, "Failed recording", Toast.LENGTH_LONG).show();
						Logger.log(new Exception("Failed Recording"));
						mActivity.finish();
					}
					return true;
				} else if(event.getAction() == MotionEvent.ACTION_UP){
					if (recording) {
						Logger.log(Log.INFO, TAG, "Stop recording");
						stopRecording();
						if (timer != null) {
							timer.cancel();
							TextView counter = (TextView)findViewById(R.id.recording_counter);
							counter.setVisibility(View.INVISIBLE);
//							addVideoButton.setVisibility(View.VISIBLE);
//							switchCamera.setVisibility(View.VISIBLE);
							counter.setText("10");
						}
					}
					return true;
				}
				return true;
			}
		});
	}

	public void onResume() {
		super.onResume();
		Logger.log(Log.INFO, TAG, "Resuming Activity");
		openCamera();
		mPreview.refreshCamera(cameraId, mCamera);
		Logger.fbActivate(this, true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
        if (recording) {
            stopRecording();
        }
		releaseCamera();
		Logger.fbActivate(this, false);
	}

	public void initialize() {
		cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);

		openCamera();
		mPreview = new CameraPreview(mActivity, cameraId, mCamera);
		cameraPreview.addView(mPreview);


//		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
//		switchCamera.setOnClickListener(switchCameraListener);

//		addVideoButton = (Button) findViewById(R.id.button_add);
//		addVideoButton.setOnClickListener(addVideoListener);

	}

	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// get the number of cameras
			if (!recording) {
				Logger.log(Log.INFO, TAG, "Request to switch cameras");
				int camerasNumber = Camera.getNumberOfCameras();
				if (camerasNumber > 1) {
					// release the old camera instance
					// switch camera, from the front and the back and vice versa
					releaseCamera();
					switchCamera();
				} else {
					Toast.makeText(mActivity, "Sorry, your phone has only one camera", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(mActivity, "Sorry, cannot switch cameras while recording", Toast.LENGTH_LONG).show();
			}
		}
	};

	OnClickListener addVideoListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!recording) {
				Logger.log(Log.INFO, TAG, "Opening gallery");
				Logger.trackEvent(mActivity, "Upload", "Open Gallery");
				Intent mediaChooser = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
				mediaChooser.setType("video/*");
				startActivityForResult(mediaChooser, 1);
			} else {
				Toast.makeText(mActivity, "Sorry, cannot open gallery while recording", Toast.LENGTH_LONG).show();
			}
		}
	};

	public void switchCamera() {
		// if the camera preview is the front
		if (cameraFront) {
			cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// refresh the preview
				try {
					mCamera = Camera.open(cameraId);
				}
				catch (RuntimeException e) {
					Logger.log(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
					return;
				}
				mPreview.refreshCamera(cameraId, mCamera);
				cameraFront = false;
			}
		} else {
			cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {
				// open the backFacingCamera
				// refresh the preview
				try {
					mCamera = Camera.open(cameraId);
				}
				catch (RuntimeException e) {
					Logger.log(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
					return;
				}
				mPreview.refreshCamera(cameraId, mCamera);
				cameraFront = true;
			}
		}
	}

	private void openCamera() {
		if (!hasCamera(mActivity)) {
			Toast.makeText(mActivity, "Sorry, we could not find your camera", Toast.LENGTH_LONG).show();
			Logger.log(new Exception("No Camera found"));
			finish();
			return;
		}
		if (mCamera == null) {
			int frontFacingCamId = findFrontFacingCamera();
			int backFacingCamId = findBackFacingCamera();
			if (frontFacingCamId < 0) {
				switchCamera.setVisibility(View.GONE);
				try {
					mCamera = Camera.open(backFacingCamId);
				}
				catch (RuntimeException e) {
					Logger.log(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
				}

				cameraId = backFacingCamId;
				cameraFront = false;
			} else {
				try {
					mCamera = Camera.open(frontFacingCamId);
				}
				catch (RuntimeException e) {
					Logger.log(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
				}

				cameraId = frontFacingCamId;
				cameraFront = true;

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
				break;
			}
		}
		return cameraId;
	}

	private boolean hasCamera(Context context) {
		// check if the device has rear camera, could also add or front
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

    protected void stopRecording() { // Exception
        // stop recording and release camera

	    try{
		    mediaRecorder.stop(); // stop the recording
	    }catch(RuntimeException stopException){
		    //handle cleanup here
		    releaseMediaRecorder(); // release the MediaRecorder object
		    Toast.makeText(CameraPreviewActivity.this, "Long press to record", Toast.LENGTH_LONG).show();
		    recording = false;
		    return;
	    }
	    releaseMediaRecorder(); // release the MediaRecorder object
        recording = false;
	    Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
	    intent.putExtra("videoId", videoId);
	    intent.putExtra("channelId", channelId);
	    intent.putExtra("cameraFront", cameraFront);
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

		videoId = Long.toString(System.currentTimeMillis()) + ".mp4";
        String uploadPath = Video.uploadDir.getAbsolutePath() + "/" + videoId;
		mediaRecorder.setOutputFile(uploadPath);
		mediaRecorder.setMaxDuration(15000); // Set max duration
		if (cameraFront) {
			mediaRecorder.setProfile(CamcorderProfile.get(CameraInfo.CAMERA_FACING_FRONT, CamcorderProfile.QUALITY_720P));
			mediaRecorder.setOrientationHint(270);
		} else {
			mediaRecorder.setProfile(CamcorderProfile.get(CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_720P));
			mediaRecorder.setOrientationHint(90);
		}

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			Logger.log(e);;
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			Logger.log(e);;
			return false;
		}
		recording = true;
		return true;

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == Activity.RESULT_OK) {

				videoId = Long.toString(System.currentTimeMillis()) + ".mp4";
				String uploadPath = Video.uploadDir.getAbsolutePath() + "/" + videoId;
				String in = "";
				try {
					Uri selectedUri = data.getData();
					String[] filePathColumn = {MediaStore.Video.Media.DATA};
					Cursor cursor = getContentResolver().query(selectedUri, filePathColumn, null, null, null);
					if(cursor.moveToFirst()){
						int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
						in = cursor.getString(columnIndex);
					} else {
						//boooo, cursor doesn't have rows ...
					}
					cursor.close();

					File inFile = new File(in);
					if (inFile.length() > 50000000) {
						Toast.makeText(mActivity, "Video cannot be larger than 50 MB", Toast.LENGTH_LONG).show();
						return;
					}

					Toast.makeText(mActivity, "Preparing the video", Toast.LENGTH_LONG).show();
					// source file channel
					// return the unique FileChannel object associated with this file input stream.
					FileChannel srcChannel = new FileInputStream(in).getChannel();

					// destination file channel
					// return the unique FileChannel object associated with this file output stream.
					FileChannel dstChannel = new FileOutputStream(uploadPath).getChannel();

					// transfer bytes into this channel's file from the given readable byte channel
					dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

					// close channels
					srcChannel.close();
					dstChannel.close();

				}
				catch (Exception e) {
					Logger.log(e);
					Toast.makeText(mActivity, "Could not access the video", Toast.LENGTH_LONG).show();
					return;
				}
				Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
				intent.putExtra("videoId", videoId);
				intent.putExtra("originalPath", in);
				String channelId = getIntent().getExtras().getString("channelId");
				intent.putExtra("channelId", channelId);
				startActivity(intent);

			}

		}
	}

	// Handle Touch

//	@Override
//	public boolean onTouchEvent(MotionEvent event){
//
//		this.mDetector.onTouchEvent(event);
//		return super.onTouchEvent(event);
//	}

//	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
//		private static final String DEBUG_TAG = "Gestures";
//
//		@Override
//		public boolean onDown(MotionEvent event) {
//			Log.d(DEBUG_TAG, "onDown: " + event.toString());
//			return true;
//		}

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