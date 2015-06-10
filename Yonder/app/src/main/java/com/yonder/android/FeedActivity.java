package com.yonder.android;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.util.ArrayList;

public class FeedActivity extends Activity {

	private ArrayList<VideoView> videoViews;
	private RelativeLayout feedLayout;
	private int tap = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		setVideoViews();
		videoViews.get(0).start();
		videoViews.get(0).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				videoViews.get(0).start();
			}
		});

	}

	private void setVideoViews() {
		feedLayout = (RelativeLayout) findViewById(R.id.feedLayout);
		VideoView vidView;
		ArrayList<String> uris = getContent();
		videoViews = new ArrayList<>();

		for(String uri : uris) {
			vidView = new VideoView(this);
			Uri vidUri = Uri.parse(uri);
			vidView.setVideoURI(vidUri);
			videoViews.add(vidView);
			feedLayout.addView(vidView);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP && tap < videoViews.size()-1) {
			tap++;
			videoViews.get(tap-1).stopPlayback();
			feedLayout.removeView(videoViews.get(tap-1));
			videoViews.get(tap).start();
			videoViews.get(tap).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					videoViews.get(tap).start();
				}
			});
			return true;
		}
		return false;
	}

	public ArrayList<String> getContent() {
		ArrayList<String> uris = new ArrayList<>();
		uris.add("http://storage.googleapis.com/yander/20150306_235451_001.mp4");
		uris.add("http://storage.googleapis.com/yander/20130810_182659.mp4");
		uris.add("http://storage.googleapis.com/yander/20120809_015947.mp4");

		return uris;
	}
}