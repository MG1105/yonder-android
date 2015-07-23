package com.yonder.android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AppEngine {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    String response;
    HttpURLConnection conn = null;

    protected JSONObject uploadVideo(String uploadPath, String videoId, String caption, String userId, String longitude, String latitude) {
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024*1024;
        String query = "";
        try
        {
            String encodedCaption = URLEncoder.encode(caption, "UTF-8");
            query = "?caption=" + encodedCaption + "&user=" + userId + "&long=" + longitude + "&lat=" + latitude;

            String urlString = "http://subtle-analyzer-90706.appspot.com/videos" + query;

            FileInputStream fileInputStream = new FileInputStream(new File(uploadPath+"/"+videoId) );
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive"); // Needed?
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
            Log.i(TAG, "File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (IOException ioe)
        {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject getFeed(String userId, String longitude, String latitude, boolean myVideosOnly) {

        String query = "";
        try
        {
            query = "?user=" + userId + "&long=" + longitude + "&lat=" + latitude;
            if (myVideosOnly) {
                query += "&search=mine";
            } else {
                query += "&search=near";
            }
            String urlString = "http://subtle-analyzer-90706.appspot.com/videos" + query;

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive"); // Needed?
        } catch (IOException ioe)
        {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject getFeedInfo(String ids) {

        String query = "";
        try
        {
            String encodedIds = URLEncoder.encode(ids, "UTF-8");
            query = "?ids=" + encodedIds;
            String urlString = "http://subtle-analyzer-90706.appspot.com/videos/info" + query;

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive"); // Needed?
        } catch (IOException ioe)
        {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject getMyFeedInfo(String user) {

        String query = "";
        try
        {
            query = "?user=" + user;
            String urlString = "http://subtle-analyzer-90706.appspot.com/myvideos/info" + query;

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive"); // Needed?
        } catch (IOException ioe)
        {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject addComment(String userId, String videoId, String commentId, String comment) {
        DataOutputStream dos;
        String query = "";
        try
        {
            String encodedComment = URLEncoder.encode(comment, "UTF-8");
            query = "comment=" + encodedComment + "&user=" + userId + "&id=" + commentId ;
            Log.i(TAG, query);

            String urlString = "http://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/comments";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
            dos = new DataOutputStream( conn.getOutputStream() );
            dos.writeBytes(query);
            dos.flush();
            dos.close();
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject getComments(String videoId) {
        try
        {
            String urlString = "http://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/comments";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject reportVideo(String videoId) {
        try
        {
            String urlString = "http://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/flag";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }


    protected JSONObject reportComment(String commentId) {
        try
        {
            String urlString = "http://subtle-analyzer-90706.appspot.com/comments/" + commentId + "/flag";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject rateVideo(String videoId, String rating) {
        DataOutputStream dos;
        String query = "";
        try
        {
            query = "rating=" + rating;
            Log.i(TAG, query);

            String urlString = "http://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/rating";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
            dos = new DataOutputStream( conn.getOutputStream() );
            dos.writeBytes(query);
            dos.flush();
            dos.close();
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject rateComment(String commentId, String rating) {
        DataOutputStream dos;
        String query = "";
        try
        {
            query = "rating=" + rating;
            Log.i(TAG, query);

            String urlString = "http://subtle-analyzer-90706.appspot.com/comments/" + commentId + "/rating";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
            dos = new DataOutputStream( conn.getOutputStream() );
            dos.writeBytes(query);
            dos.flush();
            dos.close();
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }

    protected JSONObject verifyUser(String userId) {
        try
        {
            String urlString = "http://subtle-analyzer-90706.appspot.com/users/" + userId + "/verify";

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
        } catch (IOException ioe) {
            Log.e(TAG, "error: " + ioe.getMessage(), ioe);
        }
        return getResponse();
    }



    private JSONObject getResponse() {
        BufferedReader reader;
        response = "";
        JSONObject out = null;
        try {
            reader =  new BufferedReader( new InputStreamReader(conn.getInputStream() )); // getErrorStream
            String line;

            while (( line = reader.readLine()) != null)
            {
                response += line;
            }
            Log.i(TAG,"Server Response: \n" + response);
            reader.close();
        } catch (IOException ioex){
            Log.e(TAG, "error: " + ioex.getMessage(), ioex);
        }

        try {
            out = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return out;
    }
}
