package com.vidici.android;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private int cameraId;
	private Activity activity;
	static int displayOrientation;

	public CameraPreview(Activity activity, int cameraId, Camera camera) {
		super(activity);
		this.activity = activity;
		mCamera = camera;
		this.cameraId = cameraId;
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (mCamera != null) {
			refreshCamera(cameraId, mCamera);
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	}

	public void setAutoFocus() {
		Camera.Parameters params = mCamera.getParameters();
		List<String> modes = params.getSupportedFocusModes();
		for (String m : modes) {
			if (m.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				mCamera.setParameters(params);
				break;
			}
		}
	}

	public void refreshCamera(int cameraId, Camera camera) {
		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}
		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings
		mCamera = camera;
		try {
			mCamera.setPreviewDisplay(mHolder);
			setCameraDisplayOrientation(activity, cameraId, camera);
			mCamera.startPreview();
			setAutoFocus();
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public void setCameraDisplayOrientation(Activity activity,
	                                               int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		displayOrientation = result;
		Logger.log(Log.INFO, TAG, "Set display orientation " + displayOrientation);
		camera.setDisplayOrientation(displayOrientation);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// mCamera.release();

	}
}