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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CapturedVideoActivity extends Activity {
    private VideoView vidView;
    private Button uploadButton;
    private Context myContext;
    String videoId;
    String uploadPath;

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
            uploadVideo();
        }
    };

    private void uploadVideo() {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;
        String responseFromServer = "";
        String urlString = "http://subtle-analyzer-90706.appspot.com/upload";
        try
        {
            //------------------ CLIENT REQUEST
            FileInputStream fileInputStream = new FileInputStream(new File(uploadPath+"/"+videoId) );
            // open a URL connection to the Servlet
            URL url = new URL(urlString);
            // Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();
            // Allow Inputs
            conn.setDoInput(true);
            // Allow Outputs
            conn.setDoOutput(true);
            // Don't use a cached copy.
            conn.setUseCaches(false);
            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data"); // Needed?
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
            dos = new DataOutputStream( conn.getOutputStream() );
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=uploadedfile;filename=" + videoId + lineEnd);
            dos.writeBytes(lineEnd);
            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0)
            {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            // close streams
            Log.e("Debug", "File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();
        }
        catch (MalformedURLException ex)
        {
            Log.e("Debug", "error: " + ex.getMessage(), ex);
        }
        catch (IOException ioe)
        {
            Log.e("Debug", "error: " + ioe.getMessage(), ioe);
        }
        //------------------ read the SERVER RESPONSE
        try {
            inStream = new DataInputStream ( conn.getInputStream() ); // getErrorStream
            String str;

            while (( str = inStream.readLine()) != null)
            {
                Log.e("Debug","Server Response "+str);
            }
            inStream.close();

        }
        catch (IOException ioex){
            Log.e("Debug", "error: " + ioex.getMessage(), ioex);
        }

    }

    private void compressVideo() {
        LoadJNI vk = new LoadJNI();
        try {
            String uploadPath = getApplicationContext().getExternalFilesDir("upload").getAbsolutePath();
            // ac audio channels ar audio frequency b bitrate
            String[] complexCommand = GeneralUtils.utilConvertToComplex("ffmpeg -y -i " + uploadPath + "/captured.mp4" +
                    " -strict experimental -s 1280x720 -r 24 -vcodec libx264 -b 1000k -ab 100k -ac 2 -ar 22050 " + uploadPath + "/" + videoId);
            vk.run(complexCommand, uploadPath, getApplicationContext());
            Log.i("test", "ffmpeg4android finished successfully");
        } catch (Throwable e) {
            Log.e("test", "vk run exception.", e);
        }
    }


}