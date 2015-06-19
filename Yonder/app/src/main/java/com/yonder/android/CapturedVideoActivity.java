package com.yonder.android;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import java.util.ArrayList;

public class CapturedVideoActivity extends Activity {
    private VideoView vidView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_video);
        vidView = (VideoView) findViewById(R.id.capturedVideo);
        Uri vidUri = Uri.parse("/sdcard/myvideo.mp4");
        vidView.setVideoURI(vidUri);
        vidView.start();
        vidView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                vidView.start();
            }
        });
    }

}