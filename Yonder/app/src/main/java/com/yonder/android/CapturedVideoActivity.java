package com.yonder.android;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import org.json.JSONException;
import org.json.JSONObject;

public class CapturedVideoActivity extends Activity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    private VideoView vidView;
    private Button uploadButton;
    private Context myContext;
    String videoId;
    String uploadPath;
    String caption = "default caption";
    String userId = "12345677";
    String longitude = "-121.886329";
    String latitude = "37.338208";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = this;
        setContentView(R.layout.activity_captured_video);
        vidView = (VideoView) findViewById(R.id.capturedVideo);
        uploadPath = this.getExternalFilesDir("upload").getAbsolutePath();
        videoId = Long.toString(System.currentTimeMillis()) + ".mp4";
        Uri vidUri = Uri.parse(uploadPath + "/captured.mp4");
        vidView.setVideoURI(vidUri);
        vidView.start();
        vidView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                vidView.start();
            }
        });
        compressVideo();
        uploadButton = (Button) findViewById(R.id.button_upload);
        uploadButton.setOnClickListener(uploadListener);
    }

    View.OnClickListener uploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(myContext, "Uploading...", Toast.LENGTH_LONG);
            toast.show();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            AppEngine gae = new AppEngine();
            JSONObject response = gae.uploadVideo(uploadPath, videoId, caption, userId, longitude, latitude);
            try {
                Log.i(TAG, response.getString("success"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };



    private void compressVideo() {
        LoadJNI vk = new LoadJNI();
        try {
            String uploadPath = getApplicationContext().getExternalFilesDir("upload").getAbsolutePath();
            // ac audio channels ar audio frequency b bitrate
            String[] complexCommand = GeneralUtils.utilConvertToComplex("ffmpeg -y -i " + uploadPath + "/captured.mp4" +
                    " -strict experimental -s 1280x720 -r 24 -vcodec libx264 -b 1000k -ab 100k -ac 2 -ar 22050 " + uploadPath + "/" + videoId);
            vk.run(complexCommand, uploadPath, getApplicationContext());
            Log.i(TAG, "ffmpeg4android finished successfully");
        } catch (Throwable e) {
            Log.e(TAG, "vk run exception.", e);
        }
    }

    private void getLocation() {

    }

    private void getUserId() {

    }


}