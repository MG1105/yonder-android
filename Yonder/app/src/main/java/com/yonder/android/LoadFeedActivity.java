package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadFeedActivity extends Activity {

    ArrayList videos;
    VideosAdapter adapter;
    private ProgressBar spinner;
    Animation rotation;
    ImageView loadFeedImageView;
    TextView loadFeedTextView;
    Activity mActivity;
    ArrayList<Request> requests;
    ArrayList<Uri> uris = new ArrayList<>();
    int remaining;
    boolean myVideosOnly;

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

    @Override
    protected void onResume() {
        super.onResume();
        GetMyFeedInfoTask getMyFeedInfo = new GetMyFeedInfoTask();
        getMyFeedInfo.execute("12345677");
    }

    OnClickListener loadListener = new OnClickListener() {
        @Override
        public void onClick(View v) { // dismiss multiple clicks, check last request ts, sleep for longer animation
            loadFeedImageView.startAnimation(rotation);
            Switch myVideosOnlySwitch = (Switch) findViewById(R.id.switch_my_videos);
            myVideosOnly = myVideosOnlySwitch.isChecked();
            loadFeedTextView = (TextView) findViewById(R.id.loadFeedTextView);
            if (myVideosOnly) {
                loadFeedTextView.setText("Looking for your videos...");
            } else {
                loadFeedTextView.setText("Looking for videos around you...");
            }
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            GetFeedTask getFeed = new GetFeedTask();
            getFeed.execute();

        }
    };

    class GetFeedTask extends AsyncTask<Void, Void, JSONArray> {

        protected JSONArray doInBackground(Void... params) {
            try {
                uris = new ArrayList<>();
                AppEngine gae = new AppEngine();
                String userId = "12345677";
                String longitude = "-121.886329";
                String latitude = "37.338208";
                JSONObject response = gae.getFeed(userId, longitude, latitude, myVideosOnly);
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
                    if (myVideosOnly) {
                        loadFeedTextView.setText("We did not find any video you uploaded or commented on " +
                                "in the past 24 hours");
                    } else {
                        loadFeedTextView.setText("No new videos found. Please try again later.");
                    }


                    loadFeedImageView.clearAnimation();
                }
            } else {
                loadFeedTextView.setText("Could not retrieve videos. Please try again later.");
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

    class GetMyFeedInfoTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getMyFeedInfo(params[0]);
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videosArray = response.getJSONArray("videos");
                        videos = Video.fromJson(videosArray);
                        return null;
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

        protected void onPostExecute(Void params) {
            if (videos != null) {
                // TODO: check exception?
                Log.d("", videos.toString());
                // Create the adapter to convert the array to views
                adapter = new VideosAdapter(mActivity);
                // Attach the adapter to a ListView
                ListView listView = (ListView) findViewById(R.id.listView_my_videos);
                listView.setAdapter(adapter);
                spinner = (ProgressBar)findViewById(R.id.progress_videos);
                spinner.setVisibility(View.GONE);
                if (videos.size() < 0) {


                }
            } else {

            }

        }
    }

    class VideosAdapter extends ArrayAdapter<Video> {
        Video video;

        public VideosAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, videos);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            video = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_video, parent, false);
            }

            // Lookup view for data population
            TextView date = (TextView) convertView.findViewById(R.id.textView_date);
            TextView caption = (TextView) convertView.findViewById(R.id.textView_caption);
            TextView rating = (TextView) convertView.findViewById(R.id.textView_rating);
            TextView comments_total = (TextView) convertView.findViewById(R.id.textView_comments_total);

            // Populate the data into the template view using the data object
            date.setText("Date: " + video.getId());
            caption.setText("Caption: " + video.getCaption());
            rating.setText(video.getRating() + " Likes");
            comments_total.setText(video.getCommentsTotal() + " Comments");

            // Return the completed view to render on screen
            return convertView;

        }

    }

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



}