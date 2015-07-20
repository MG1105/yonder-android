package com.yonder.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class CapturedVideoActivity extends Activity { // Test phone screen off/lock
    private final String TAG = "Log." + this.getClass().getSimpleName();
    private VideoView vidView;
    private Button uploadButton;
    private Context myContext;
    private ProgressBar spinner;
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
        spinner = (ProgressBar)findViewById(R.id.uploading_in_progress);
        spinner.setVisibility(View.GONE);
        uploadPath = this.getExternalFilesDir("upload").getAbsolutePath();
        videoId = Long.toString(System.currentTimeMillis()) + ".mp4";
        final Uri vidUri = Uri.parse(uploadPath + "/captured.mp4");
        vidView.setVideoURI(vidUri);
	    vidView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
		    @Override
		    public void onPrepared(MediaPlayer mp) {
			    mp.setLooping(true);
		    }
	    });
        uploadButton = (Button) findViewById(R.id.button_upload);
        uploadButton.setOnClickListener(uploadListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vidView.start();
    }

    View.OnClickListener uploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(myContext, "Uploading...", Toast.LENGTH_LONG);
            toast.show();
	        vidView.pause();
            spinner.setVisibility(View.VISIBLE);
            uploadButton.setVisibility(View.GONE);
            UploadVideoTask upload = new UploadVideoTask();
            upload.execute();
        }
    };


    class UploadVideoTask extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            compressVideo();
            AppEngine gae = new AppEngine();
            JSONObject response = gae.uploadVideo(uploadPath, videoId, caption, userId, longitude, latitude);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response.getString("success").equals("1")) {
                    Toast toast = Toast.makeText(myContext, "Video Uploaded!", Toast.LENGTH_LONG);
                    toast.show();
                    spinner.setVisibility(View.GONE);
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            File[] listFile = getApplicationContext().getExternalFilesDir("upload").listFiles();
            if (listFile != null) {
                for (int i = 0; i < listFile.length; i++) {
                    listFile[i].delete();  // deletes new video we just captured as this just finished
                }
            }
        }
    }

    private void compressVideo() { // A/libc? Fatal signal 11 (SIGSEGV) at 0x836b8a94 (code=2), thread 10051 (AsyncTask #4): unless you change from h264 to mpeg4
        LoadJNI vk = new LoadJNI(); // reduce library size
        try {
            String uploadPath = getApplicationContext().getExternalFilesDir("upload").getAbsolutePath();
            Log.i(TAG, "Compressing video");
            // ac audio channels ar audio frequency b bitrate
            String[] complexCommand = GeneralUtils.utilConvertToComplex("ffmpeg -y -i " + uploadPath + "/captured.mp4" +
                    " -strict experimental -s 1280x720 -r 24 -vcodec mpeg4 -b 1000k -ab 100k -ac 2 -ar 22050 " + uploadPath + "/" + videoId);
            vk.run(complexCommand, uploadPath, getApplicationContext());
            Log.i(TAG, "Video compressed"); // Cannot view on GAE
        } catch (Throwable e) {
            Log.e(TAG, "vk run exception.", e);
        }
    }

    private void getLocation() {

    }

    private void getUserId() {

    }


}