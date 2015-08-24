package yonder.android;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.crashlytics.android.Crashlytics;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

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
    String caption;
    String userId;
    String longitude;
    String latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = this;
        setContentView(R.layout.activity_captured_video);
        Crashlytics.log(Log.INFO, TAG, "Creating Activity");
        vidView = (VideoView) findViewById(R.id.capturedVideo);
        spinner = (ProgressBar)findViewById(R.id.uploading_in_progress);
        spinner.setVisibility(View.GONE);
        uploadPath = Video.uploadDir.getAbsolutePath();
        videoId = getIntent().getExtras().getString("videoId");
        final Uri vidUri = Uri.parse(uploadPath + "/" + videoId);
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
        Crashlytics.log(Log.INFO, TAG, "Resuming Activity");
        Alert.showVideoRule(this);
        vidView.start();
    }

    View.OnClickListener uploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            uploadButton.setEnabled(false);
            EditText captionText = (EditText) findViewById(R.id.editText_caption);
            if (captionText.getText().length() == 0) {
                Toast toast = Toast.makeText(myContext, "Please add a caption first", Toast.LENGTH_LONG);
                toast.show();
                uploadButton.setEnabled(true);
            } else {
                Toast toast = Toast.makeText(myContext, "Uploading...", Toast.LENGTH_LONG);
                toast.show();
                vidView.pause();
                spinner.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.GONE);
                captionText.setKeyListener(null);
                captionText.setEnabled(false);
                UploadVideoTask upload = new UploadVideoTask();
                upload.execute();
                finish();
            }
        }
    };


    class UploadVideoTask extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            compressVideo();
            ArrayList<String> location = User.getLocation(myContext);
            if (location != null) {
                longitude = location.get(0);
                latitude = location.get(1);
            } else { // Default SJSU
                longitude = "-121.881072222222";
                latitude = "37.335187777777";
            }
            userId = User.getId(myContext);
            EditText captionText = (EditText) findViewById(R.id.editText_caption);
            caption = captionText.getText().toString();
            AppEngine gae = new AppEngine();
            Crashlytics.log(Log.INFO, TAG, String.format("Uploading uploadPath %s videoId %s caption %s userId %s longitude %s latitude %s",
                    uploadPath, videoId, caption, userId, longitude, latitude));
            JSONObject response = gae.uploadVideo(uploadPath, videoId, caption, userId, longitude, latitude);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        Toast.makeText(myContext, "Yonder uploaded", Toast.LENGTH_LONG).show();
                        spinner.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(myContext, "Failed to upload", Toast.LENGTH_LONG).show();
                        spinner.setVisibility(View.GONE);
                        Crashlytics.logException(new Exception("Server Side failure"));
                    }
                } else {
                    Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
            }
            new File(uploadPath + "/"+ videoId).delete();
            File[] listFile = Video.uploadDir.listFiles();
            if (listFile != null) {
                for (int i = 0; i < listFile.length; i++) {
                    if (listFile[i].getAbsolutePath().endsWith(".mp4")) {
                        continue; // can we safely delete all files if other compression going on?
                    }
                    listFile[i].delete();
                }
            }
        }
    }

    private void compressVideo() {
        LoadJNI vk = new LoadJNI(); // reduce library size
        try {
            File tmp = new File(uploadPath + "/tmp" + videoId);
            new File(uploadPath + "/" + videoId).renameTo(tmp);
            Crashlytics.log(Log.INFO, TAG, "Pre compression size " + tmp.length());
            // ac audio channels ar audio frequency b bitrate
            String command = "ffmpeg -y -i " + uploadPath + "/tmp"+ videoId +
                    " -strict experimental -s 1280x720 -r 24 -vcodec mpeg4 -b 1500k -ab 100k -ac 2 -ar 22050 " + uploadPath + "/" + videoId;
            Crashlytics.log(Log.INFO, TAG, command);
            String[] complexCommand = GeneralUtils.utilConvertToComplex(command);
            vk.run(complexCommand, uploadPath, getApplicationContext());
            Crashlytics.log(Log.INFO, TAG, "Post compression size " + new File(uploadPath + "/" + videoId).length());
            tmp.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Crashlytics.logException(e);;
        }
    }

}