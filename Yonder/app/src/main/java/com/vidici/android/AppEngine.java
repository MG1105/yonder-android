package com.vidici.android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class AppEngine {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    String response;
    HttpURLConnection conn = null;

    protected JSONObject uploadVideo(String uploadPath, String videoId, String caption, String userId, String channelId) {
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
            query = "?caption=" + encodedCaption + "&user=" + userId + "&channel=" + channelId;

            String urlString = "https://subtle-analyzer-90706.appspot.com/videos" + query;

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
            try {
                dos = new DataOutputStream( conn.getOutputStream() );
            } catch (UnknownHostException e) {
                Logger.log(Log.ERROR, TAG, "Failed to connect");
                return null;
            }
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

            Logger.log(Log.INFO, TAG, "File written");

            // close streams
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (Exception e) {
            Logger.log(e);
            return null;
        }
        return getResponse();
    }

    protected JSONObject getFeed(String userId, String channelId, String videoId) {

        String query = "";
        query = "?user=" + userId;
        if (channelId.length() > 0) {
            query += "&channel=" + channelId;
        }
        if (videoId.length() > 0) {
            query += "&video=" + videoId;
        }
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos" + query;
        return get(urlString);
    }

    protected JSONObject getFeedInfo(String ids, String userId) {

        String query = "";
        String encodedIds = null;
        encodedIds = encode(ids);
        query = "?ids=" + encodedIds + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos/info" + query;
        return get(urlString);
    }

    protected JSONObject getMyFeedInfo(String user) {

        String query = "";
        query = "?user=" + user;
        String urlString = "https://subtle-analyzer-90706.appspot.com/myvideos/info" + query;
        return get(urlString);
    }

    protected JSONObject getChannels(String user, String sort) {
        String query = "";
        query = "?user=" + user + "&sort=" + sort;
        String urlString = "https://subtle-analyzer-90706.appspot.com/channels" + query;
        return get(urlString);
    }

    protected JSONObject getNotifications(String user, String seen) {
        String query = "";
        query = "?user=" + user + "&seen=" + seen;
        String urlString = "https://subtle-analyzer-90706.appspot.com/notifications" + query;
        return get(urlString);
    }

    protected JSONObject addComment(String nickname, String userId, String videoId, String commentId, String comment) {
        String query = "";
        String encodedComment = encode(comment);
        query = "comment=" + encodedComment + "&user=" + userId + "&id=" + commentId + "&nickname=" + nickname;
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/comments";
        return post(urlString, query);
    }

    protected JSONObject addChannel(String userId, String channelName) {
        String query = "";
        channelName = encode(channelName);
        query = "channel=" + channelName + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/channels";
        return post(urlString, query);
    }

    protected JSONObject contactUs(String userId, String message) {
        String query = "";
        message = encode(message);
        query = "message=" + message + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/contact";
        return post(urlString, query);
    }

    protected JSONObject getComments(String videoId, String userId) {
        String query = "?user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/comments"+ query;
        return get(urlString);
    }

    protected JSONObject reportVideo(String videoId, String userId) {
        String query = "user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/flag";
        return post(urlString, query);
    }


    protected JSONObject reportComment(String commentId, String userId) {
        String query = "user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/comments/" + commentId + "/flag";
        return post(urlString, query);
    }

    protected JSONObject rateVideo(String videoId, String rating, String userId) {
        String query = "";
        query = "rating=" + rating + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/videos/" + videoId + "/rating";
        return post(urlString, query);
    }

    protected JSONObject rateComment(String commentId, String rating, String userId) {
        String query = "";
        query = "rating=" + rating + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/comments/" + commentId + "/rating";
        return post(urlString, query);
    }

    protected JSONObject rateChannel(String channelId, String rating, String userId) {
        String query = "";
        query = "rating=" + rating + "&user=" + userId;
        String urlString = "https://subtle-analyzer-90706.appspot.com/channels/" + channelId + "/rating";
        return post(urlString, query);
    }

    protected JSONObject verifyUser(String userId) {
        String query = "";
        query = "?version=" + BuildConfig.VERSION_CODE;
        String urlString = "https://subtle-analyzer-90706.appspot.com/users/" + userId + "/verify" + query;
        return get(urlString);
    }

    protected JSONObject pingHome(String userId) {
        String query = "";
        query = "?version=" + BuildConfig.VERSION_CODE;
        String urlString = "https://subtle-analyzer-90706.appspot.com/users/" + userId + "/ping" + query;
        return get(urlString);
    }

    private String encode (String in) {
        String out = "";
        try {
            out = URLEncoder.encode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.log(e);
        }
        return out;
    }
    private JSONObject get(String urlString) {
        try
        {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive"); // Needed?
        } catch (Exception e) {
            Logger.log(e);
            return null;
        }
        return getResponse();
    }

    private JSONObject post(String urlString, String query) {
        try
        {
            DataOutputStream dos;
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
            try {
                dos = new DataOutputStream( conn.getOutputStream() );
            } catch (UnknownHostException e) {
                Logger.log(Log.ERROR, TAG, "Failed to connect");
                return null;
            }
            dos.writeBytes(query);
            dos.flush();
            dos.close();
        } catch (Exception e) {
            Logger.log(e);
            return null;
        }
        return getResponse();
    }

    private JSONObject getResponse() {
        BufferedReader reader;
        response = "";
        JSONObject out = null;
        try {
            try {
                reader =  new BufferedReader( new InputStreamReader(conn.getInputStream() )); // getErrorStream
            } catch (UnknownHostException e) {
                Logger.log(Log.ERROR, TAG, "Failed to connect");
                return null;
            }
            String line;

            while (( line = reader.readLine()) != null)
            {
                response += line;
            }
            Logger.log(Log.INFO, TAG, "Server Response:\n" + response);
            reader.close();
        } catch (Exception e){
            Logger.log(e);;
        }

        try {
            if (!response.equals("")) {
                out = new JSONObject(response);
            }
        } catch (JSONException e) {
            Logger.log(e);
        }
        return out;
    }
}
