package com.yonder.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

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
    String userId, longitude, latitude;
    Timer timer;
    String listIds;

    private DownloadManager downloadManager;
    static LinkedHashMap<String, JSONObject> videoInfo;

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
        userId = User.getId(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetMyFeedInfoTask getMyFeedInfo = new GetMyFeedInfoTask();
        getMyFeedInfo.execute(userId);
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
                SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                        "com.yonder.android", Context.MODE_PRIVATE);
                long lastRequest = sharedPreferences.getLong("last_request", 0);
                long now = System.currentTimeMillis();
                if (lastRequest != 0 && !User.admin) {
                    if ((now - lastRequest) / 60000 < 10) {
                        timer = new Timer();
                        timer.schedule(new StopAnimationTask(), 3000);
                        return;
                    } else {
                        sharedPreferences.edit().putLong("last_request", now).apply();
                    }
                } else {
                    sharedPreferences.edit().putLong("last_request", now).apply();
                }
            }
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            GetFeedTask getFeed = new GetFeedTask();
            getFeed.execute();

        }
    };

    class GetFeedTask extends AsyncTask<Void, Void, JSONArray> {

        protected JSONArray doInBackground(Void... params) {
            try {
                ArrayList<String> location = User.getLocation(mActivity);
                if (location != null) {
                    longitude = location.get(0);
                    latitude = location.get(1);
                }
                uris = new ArrayList<>();
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getFeed(userId, longitude, latitude, myVideosOnly);
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");
                        listIds = "";
                        for (int i = 0; i < videos.length(); i++) { // background?
                            String id = videos.getString(i);
                            if (i < videos.length() - 1) {
                                listIds += id + "xxx";
                            } else {
                                listIds += id;
                            }
                            if (isLoaded(id)) {
                                continue;
                            }
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
                    if (remaining == 0) {
                        getFeedInfoTask infoTask = new getFeedInfoTask();
                        infoTask.execute(listIds);
                    } else {
                        registerReceiver(loadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                    }
                } else {
                    if (myVideosOnly) {
                        loadFeedTextView.setText("We did not find any Yonder you uploaded or commented on " +
                                "in the past 24 hours");
                        loadFeedImageView.clearAnimation();
                    } else {
                        timer = new Timer();
                        timer.schedule(new StopAnimationTask(), 3000);
                    }
                }
            } else {
                loadFeedTextView.setText("Could not retrieve new Yonders. Please try again later.");
                loadFeedImageView.clearAnimation();
            }

        }
    }

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            loadFeedTextView.setText("Remaining = " + remaining);
            Video.obfuscate(mActivity, true);
            if (remaining == 0) {
                getFeedInfoTask infoTask = new getFeedInfoTask();
                infoTask.execute(listIds);
            }
        }
    };

    class StopAnimationTask extends TimerTask {
        public void run() {
            // When you need to modify a UI element, do so on the UI thread.
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadFeedTextView.setText("No new Yonders found. Please try again later.");
                    loadFeedImageView.clearAnimation();
                }
            });
            timer.cancel(); //Terminate the timer thread
        }
    }

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
                spinner = (ProgressBar)findViewById(R.id.progress_videos);
                spinner.setVisibility(View.GONE);
                if (videos.size() > 0) {
                    // TODO: check exception?
                    Log.d("", videos.toString());
                    // Create the adapter to convert the array to views
                    adapter = new VideosAdapter(mActivity);
                    // Attach the adapter to a ListView
                    ListView listView = (ListView) findViewById(R.id.listView_my_videos);
                    listView.setAdapter(adapter);
                } else {
                    TextView noVideos = (TextView)findViewById(R.id.textView_no_videos);
                    noVideos.setVisibility(View.VISIBLE);
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
//            TextView date = (TextView) convertView.findViewById(R.id.textView_date);
            TextView caption = (TextView) convertView.findViewById(R.id.textView_caption);
            TextView rating = (TextView) convertView.findViewById(R.id.textView_rating);
            TextView comments_total = (TextView) convertView.findViewById(R.id.textView_comments_total);

//            long id = Long.parseLong(video.getId());
//            if (id < 10000) {
//                // admin video
//            }
//            Log.d("videoid", id/1000 + "");
//            Date ts = new Date(id);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
//            sdf.setTimeZone(TimeZone.getTimeZone("PDT")); // give a timezone reference for formating (see comment at the bottom
//            String formattedDate = sdf.format(ts);

            // Populate the data into the template view using the data object
//            date.setText("Date: " + formattedDate);
            caption.setText("Caption: " + video.getCaption());
            rating.setText(video.getRating() + " Likes");
            comments_total.setText(video.getCommentsTotal() + " Comments");

            // Return the completed view to render on screen
            return convertView;

        }

    }

    class getFeedInfoTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getFeedInfo(params[0]);
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");
                        videoInfo = new LinkedHashMap<String, JSONObject>();
                        for (int i = 0; i < videos.length(); i++) { // background?
                            JSONObject vid = videos.getJSONObject(i);
                            videoInfo.put(vid.getString("id"), vid);
                        }
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

        protected void onPostExecute(Void response) {
            Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
            startActivity(intentFeedStart);
            loadFeedImageView.clearAnimation();
            loadFeedTextView.setText("Tap to load Yonders near you");
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

    private boolean isLoaded (String id) {
        boolean loaded = new File(getApplicationContext().getExternalFilesDir("loaded_videos").getAbsolutePath()+"/"+id).isFile();
        return loaded;
    }

    private void getLocation() {

    }

    private void getUserId() {

    }



}