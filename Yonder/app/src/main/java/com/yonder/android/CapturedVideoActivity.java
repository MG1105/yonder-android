package com.yonder.android;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

public class CapturedVideoActivity extends Activity {
    private VideoView vidView;
    private Button uploadButton;
    private Context myContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = this;
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

        uploadButton = (Button) findViewById(R.id.button_upload);
        uploadButton.setOnClickListener(uploadListener);
    }

    View.OnClickListener uploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(myContext, "Uploading...", Toast.LENGTH_LONG);
            toast.show();
        }
    };

}