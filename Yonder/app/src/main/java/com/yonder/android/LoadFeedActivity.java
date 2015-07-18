package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.ArrayList;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadFeedActivity extends ActionBarActivity {

    Animation rotation;
    ImageView loadFeedImageView;
    TextView loadFeedTextView;
    Activity mActivity;
    ArrayList<Request> requests;
    ArrayList<Uri> uris = new ArrayList<>();
    int remaining;

    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.animation_enter,
                R.anim.animation_leave);
        mActivity = this;
        setContentView(R.layout.activity_load_feed);
        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loadFeedImageView = (ImageView) findViewById(R.id.loadFeedImageView);
        loadFeedImageView.setOnClickListener(loadListener);

    }

    OnClickListener loadListener = new OnClickListener() {
        @Override
        public void onClick(View v) { // dismiss multiple clicks, check last request ts, sleep for longer animation
            loadFeedImageView.startAnimation(rotation);
            loadFeedTextView = (TextView) findViewById(R.id.loadFeedTextView);
            loadFeedTextView.setText("Looking for videos around you...");
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            getFeedTask getFeed = new getFeedTask();
            getFeed.execute();

        }
    };

    class getFeedTask extends AsyncTask<Void, Void, JSONArray> {

        protected JSONArray doInBackground(Void... params) {
            try {
                uris = new ArrayList<>();
                AppEngine gae = new AppEngine();
                String userId = "45";
                String longitude = "-121.886329";
                String latitude = "37.338208";
                JSONObject response = gae.getFeed(userId, longitude, latitude);
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");

                        for (int i = 0; i < videos.length(); i++) { // background?
                            String id = videos.getString(i);
                            String url = "http://storage.googleapis.com/yander/" + id + ".mp4";
                            uris.add(Uri.parse(url));
                        }
                        return videos;
                    } else {
                        return null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(JSONArray videos) {
            if (videos != null) {
                // TODO: check exception?
                if (videos.length() > 0) {
                    loadFeedTextView.setText("Found " + videos.length() + " videos");
                    remaining = uris.size();
                    setRequests();
                    for (Request request : requests) {
                        downloadManager.enqueue(request);
                    }
                    loadFeedTextView.setText("Loading...");
                    registerReceiver(loadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                } else {
                    loadFeedTextView.setText("No new videos found. Please try again later.");

                    loadFeedImageView.clearAnimation();
                }
            } else {
                loadFeedTextView.setText("Could not retrieve new videos.");
                loadFeedImageView.clearAnimation();
            }

        }
    }

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            loadFeedTextView.setText("Remaining = " + remaining);
            if (remaining == 0) {
                Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                startActivity(intentFeedStart);
                loadFeedImageView.clearAnimation();
                loadFeedTextView.setText("Tap to load Yonders near you");
            }
        }
    };

    private void setRequests() {
        requests = new ArrayList<>();
        for (Uri uri : uris) {
            Request request = new Request(uri);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalFilesDir(mActivity, "loaded_videos", uri.getLastPathSegment());
            requests.add(request);
        }
    }

    private void getLocation() {

    }

    private void getUserId() {

    }





    // Menu Code

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_load_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}