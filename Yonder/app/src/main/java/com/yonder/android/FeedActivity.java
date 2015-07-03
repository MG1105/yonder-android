package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.io.File;
import java.util.ArrayList;

public class FeedActivity extends Activity {

	private int tap = 0;
	private GestureDetectorCompat mDetector;
    VideoView currentVideo;
    ArrayList<Uri> uris;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        currentVideo = (VideoView) findViewById(R.id.currentVideo);
        uris = getContent();
        currentVideo.setVideoURI(uris.get(0));
        currentVideo.start();
        currentVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                currentVideo.start();
            }
        });

    }

	public ArrayList<Uri> getContent() {
		ArrayList<Uri> uris = new ArrayList<>();
        File videosFolder  = this.getExternalFilesDir("loaded_videos");
        File listFile[] = videosFolder.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                uris.add(Uri.parse(listFile[i].getAbsolutePath()));
            }
        }
		return uris;
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
		public boolean onSingleTapConfirmed(MotionEvent event) {
			if (tap < uris.size()-1) {
				tap++;
                currentVideo.stopPlayback();
                currentVideo.setVideoURI(uris.get(tap));
                currentVideo.start();
                currentVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        currentVideo.start();
                    }
                });
				return true;
			}
			return true;
		}
	}
}