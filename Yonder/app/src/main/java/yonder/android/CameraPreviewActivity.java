package yonder.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
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
import org.json.JSONException;
import org.json.JSONObject;

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
	private Button capture, switchCamera, feedButton, addVideoButton;
	private Activity mActivity;
	private RelativeLayout cameraPreview;
	private boolean cameraFront = false;
	String userId;
	boolean recording;
	TextView counter;
//	private GestureDetectorCompat mDetector;
    CountDownTimer timer;
	String videoId;
	View switchBackground;
	int cameraId;

	// Camera Preview

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		userId = User.getId(this);
		Crashlytics.log(Log.INFO, TAG, "Creating Activity");

		setContentView(R.layout.activity_camera_preview);
		mActivity = this;
//		mDetector = new GestureDetectorCompat(this, new MyGestureListener());
		initialize();
		Video.setPaths(mActivity);
		VerifyUserTask verifyUserTask = new VerifyUserTask();
		verifyUserTask.execute();
		User.verify(mActivity);
		User.setLocation(mActivity);
		Video.cleanup(Video.loadedDir, false);
		Video.cleanup(Video.uploadDir, true);
		User.checkRoot(mActivity);
	}

	public void onResume() {
		super.onResume();
		Crashlytics.log(Log.INFO, TAG, "Resuming Activity");
		openCamera();
		mPreview.refreshCamera(cameraId, mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Crashlytics.log(Log.INFO, TAG, "Pausing Activity");
        if (recording) {
            stopRecording();
	        capture.setBackgroundResource(R.drawable.ic_record);
        }
		releaseCamera();
	}

	public void initialize() {
		cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);

		openCamera();
		mPreview = new CameraPreview(mActivity, cameraId, mCamera);
		cameraPreview.addView(mPreview);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnTouchListener(recordListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);

		addVideoButton = (Button) findViewById(R.id.button_add);
		addVideoButton.setOnClickListener(addVideoListener);

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
					switchCamera();
				} else {
					Toast.makeText(mActivity, "Sorry, your phone has only one camera", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(mActivity, "Sorry, cannot switch cameras while recording", Toast.LENGTH_LONG).show();
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

	OnClickListener addVideoListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!recording) {
				Crashlytics.log(Log.INFO, TAG, "Opening gallery");
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
					e.printStackTrace();
					Crashlytics.logException(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
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
					e.printStackTrace();
					Crashlytics.logException(e);
					Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
					mActivity.finish();
				}
				mPreview.refreshCamera(cameraId, mCamera);
				cameraFront = true;
			}
		}
	}

	private void openCamera() {
		if (!hasCamera(mActivity)) {
			Toast.makeText(mActivity, "Sorry, we could not find your camera", Toast.LENGTH_LONG).show();
			Crashlytics.logException(new Exception("No Camera found"));
			finish();
		}
		if (mCamera == null) {
			int frontFacingCamId = findFrontFacingCamera();
			int backFacingCamId = findBackFacingCamera();
			if (frontFacingCamId < 0) {
				switchCamera.setVisibility(View.GONE);
				switchBackground.setVisibility(View.GONE);
			}
			try {
				mCamera = Camera.open(backFacingCamId);
			}
			catch (RuntimeException e) {
				e.printStackTrace();
				Crashlytics.logException(e);
				Toast.makeText(mActivity, "Could not access your camera", Toast.LENGTH_LONG).show();
				mActivity.finish();
			}

			cameraId = backFacingCamId;
			cameraFront = false;
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

	View.OnTouchListener recordListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				Crashlytics.log(Log.INFO, TAG, "Start recording");
				if (!prepareMediaRecorder()) {
					Toast.makeText(CameraPreviewActivity.this, "Unexpected error while recording", Toast.LENGTH_LONG).show();
					mActivity.finish();
				}
				try {
					capture.setBackgroundResource(R.drawable.ic_stop);
					mediaRecorder.start();
					counter = (TextView)findViewById(R.id.recording_counter);
					switchBackground = findViewById(R.id.background_ChangeCamera);
					feedButton.setVisibility(View.INVISIBLE);
					addVideoButton.setVisibility(View.INVISIBLE);
					switchCamera.setVisibility(View.INVISIBLE);
					switchBackground.setVisibility(View.INVISIBLE);
					counter.setVisibility(View.VISIBLE);
					counter.setText("9");
					timer = new CountDownTimer(10000, 1000) {

						public void onTick(long millisUntilFinished) {
							counter.setText("" + millisUntilFinished / 1000);
						}
						public void onFinish() {
							if (recording) {
								stopRecording();
								capture.setBackgroundResource(R.drawable.ic_record);
							}
							counter.setVisibility(View.INVISIBLE);
							feedButton.setVisibility(View.VISIBLE);
							addVideoButton.setVisibility(View.VISIBLE);
							switchCamera.setVisibility(View.VISIBLE);
							switchBackground.setVisibility(View.VISIBLE);
							counter.setText("10");
						}
					}.start();
				} catch (Exception e) {
					Toast.makeText(CameraPreviewActivity.this, "Failed recording", Toast.LENGTH_LONG).show();
					Crashlytics.logException(new Exception("Failed Recording"));
					mActivity.finish();
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
						counter.setVisibility(View.INVISIBLE);
						feedButton.setVisibility(View.VISIBLE);
						addVideoButton.setVisibility(View.VISIBLE);
						switchCamera.setVisibility(View.VISIBLE);
						switchBackground.setVisibility(View.VISIBLE);
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
        Toast.makeText(CameraPreviewActivity.this, "Yondor Captured", Toast.LENGTH_LONG).show();
        recording = false;
	    Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
	    intent.putExtra("videoId", videoId);
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
			mediaRecorder.setProfile(CamcorderProfile.get(CameraInfo.CAMERA_FACING_FRONT, CamcorderProfile.QUALITY_HIGH));
			mediaRecorder.setOrientationHint(270);
		} else {
			mediaRecorder.setProfile(CamcorderProfile.get(CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_HIGH));
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
							"yonder.android", Context.MODE_PRIVATE);
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
				Toast.makeText(mActivity, "Could not connect to the Internet. Please try again later", Toast.LENGTH_LONG).show();
				finish();
			}

		}
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
					e.printStackTrace();
					Crashlytics.logException(e);
					Toast.makeText(mActivity, "Could not access the video", Toast.LENGTH_LONG).show();
					return;
				}
				Intent intent = new Intent(mActivity, CapturedVideoActivity.class);
				intent.putExtra("videoId", videoId);
				intent.putExtra("originalPath", in);
				startActivity(intent);

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