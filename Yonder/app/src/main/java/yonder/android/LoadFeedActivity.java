package yonder.android;

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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class LoadFeedActivity extends Activity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
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
    static boolean myVideosOnly;
    String userId, longitude, latitude;
    Timer timer;
    String listIds;
    String score;
    Switch myVideosOnlySwitch;

    private DownloadManager downloadManager;
    static LinkedHashMap<String, JSONObject> videoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.animation_enter,
                R.anim.animation_leave);
        mActivity = this;
        setContentView(R.layout.activity_load_feed);
        Crashlytics.log(Log.INFO, TAG, "Creating Activity");
        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loadFeedImageView = (ImageView) findViewById(R.id.loadFeedImageView);
        loadFeedImageView.setOnClickListener(loadListener);
        userId = User.getId(this);
        loadFeedTextView = (TextView) findViewById(R.id.loadFeedTextView);
        myVideosOnlySwitch = (Switch) findViewById(R.id.switch_my_videos);
        myVideosOnlySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    loadFeedTextView.setText("Tap to watch your feed");
                } else {
                    loadFeedTextView.setText("Tap to look for Yondors near you");
                }
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "Resuming Activity");
        GetMyFeedInfoTask getMyFeedInfo = new GetMyFeedInfoTask();
        getMyFeedInfo.execute(userId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Crashlytics.log(Log.INFO, TAG, "Pausing Activity");
        Video.obfuscate(true);
    }

    OnClickListener loadListener = new OnClickListener() {
        @Override
        public void onClick(View v) { // dismiss multiple clicks
            loadFeedImageView.setClickable(false);
            loadFeedImageView.startAnimation(rotation);

            myVideosOnly = myVideosOnlySwitch.isChecked();
            if (myVideosOnly) {
                loadFeedTextView.setText("Looking for your Yondors...");
            } else {
                loadFeedTextView.setText("Looking for Yondors around you...");
                SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                        "yonder.android", Context.MODE_PRIVATE);
                long lastRequest = sharedPreferences.getLong("last_request", 0);
                long now = System.currentTimeMillis();
                Random generator = new Random();
                int wait = generator.nextInt(10) + 10;
                if (lastRequest != 0 && !User.admin) {
                    if ((now - lastRequest) / 60000 < wait) {
                        timer = new Timer();
                        timer.schedule(new StopAnimationTask(), 3000);
                        loadFeedImageView.setClickable(true);
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

    class GetFeedTask extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            try {
                ArrayList<String> location = User.getLocation(mActivity);
                if (location != null) {
                    longitude = location.get(0);
                    latitude = location.get(1);
                } else { // Default SJSU
                    longitude = "-121.881072222222";
                    latitude = "37.335187777777";
                }
                uris = new ArrayList<>();
                AppEngine gae = new AppEngine();
                Crashlytics.log(Log.INFO, TAG, String.format("Getting feed for userId %s longitude %s latitude %s myVideosOnly %s",
                        userId, longitude, latitude, myVideosOnly));
                JSONObject response = gae.getFeed(userId, longitude, latitude, myVideosOnly, false);
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            if (response != null) {
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");
                        listIds = "";
                        for (int i = 0; i < videos.length(); i++) {
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
                        if (videos.length() > 0) {
                            loadFeedTextView.setText("Found " + videos.length() + " Yondors");
                            remaining = uris.size();
                            setRequests();
                            for (Request request : requests) {
                                downloadManager.enqueue(request);
                            }
                            loadFeedTextView.setText("Loading...");
                            if (remaining == 0) {
                                Crashlytics.log(Log.INFO, TAG, "All videos in cache");
                                getFeedInfoTask infoTask = new getFeedInfoTask();
                                infoTask.execute(listIds, userId);
                            } else {
                                Crashlytics.log(Log.INFO, TAG, remaining + " videos to download");
                                registerReceiver(loadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                            }
                        } else {
                            if (myVideosOnly) {
                                loadFeedTextView.setText("We did not find any Yondor you uploaded or commented on " +
                                        "in the past 24 hours");
                                loadFeedImageView.clearAnimation();
                            } else {
                                timer = new Timer();
                                timer.schedule(new StopAnimationTask(), 3000);
                            }
                            loadFeedImageView.setClickable(true);
                        }
                    } else {
                        Crashlytics.logException(new Exception("Server Side Failure"));
                        loadFeedTextView.setText("Could not retrieve new Yondors. Please try again later");
                        loadFeedImageView.clearAnimation();
                        loadFeedImageView.setClickable(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);;
                    loadFeedTextView.setText("Could not retrieve new Yondors. Please try again later");
                    loadFeedImageView.clearAnimation();
                }
            } else {
                loadFeedTextView.setText("Please check your connectivity and try again later");
                loadFeedImageView.clearAnimation();
                loadFeedImageView.setClickable(true);
            }
        }
    }

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            Crashlytics.log(Log.INFO, TAG, "Remaining " + remaining);
            if (remaining >= 0) { // Bug < 0?
                loadFeedTextView.setText("Loading... " + remaining);
            }
            if (remaining == 0) {
                Video.obfuscate(true);
                getFeedInfoTask infoTask = new getFeedInfoTask();
                infoTask.execute(listIds, userId);
            }
        }
    };

    class StopAnimationTask extends TimerTask {
        public void run() {
            // When you need to modify a UI element, do so on the UI thread.
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadFeedTextView.setText("No new Yondors found. Please try again later");
                    loadFeedImageView.clearAnimation();
                }
            });
            timer.cancel(); //Terminate the timer thread
        }
    }

    class GetMyFeedInfoTask extends AsyncTask<String, Void, JSONObject> { // Rename to GetStats

        protected JSONObject doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getMyFeedInfo(params[0]);
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                        score = response.getString("score");
                        TextView scoreView = (TextView)findViewById(R.id.textView_score);
                        if (Integer.parseInt(score) == 1 || Integer.parseInt(score) == -1) {
                            scoreView.setText(score + " Coin");
                        } else {
                            scoreView.setText(score + " Coins");
                        }


                        JSONArray videosArray = response.getJSONArray("videos");
                        videos = Video.fromJson(videosArray);
                        ListView listView = (ListView) findViewById(R.id.listView_my_videos);
                        TextView noVideos = (TextView)findViewById(R.id.textView_no_videos);
                        if (videos.size() > 0) {
                            // Create the adapter to convert the array to views
                            adapter = new VideosAdapter(mActivity);
                            // Attach the adapter to a ListView
                            listView.setAdapter(adapter);
                            noVideos.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noVideos.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Crashlytics.logException(new Exception("Server Side Failure"));
                        TextView noVideos = (TextView)findViewById(R.id.textView_no_videos);
                        noVideos.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    TextView noVideos = (TextView)findViewById(R.id.textView_no_videos);
                    noVideos.setVisibility(View.VISIBLE);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
                spinner = (ProgressBar)findViewById(R.id.progress_videos);
                spinner.setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
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
            int ratingInt = Integer.parseInt(video.getRating());
            int commentsInt = Integer.parseInt(video.getCommentsTotal());
            String commentUnit;
            String ratingUnit;
            if (ratingInt == 1 || ratingInt == -1) {
                ratingUnit = " Like";
            } else {
                ratingUnit = " Likes";
            }
            if (commentsInt == 1) {
                commentUnit = " Comment";
            } else {
                commentUnit = " Comments";
            }
            rating.setText(video.getRating() + ratingUnit);
            comments_total.setText(video.getCommentsTotal() + commentUnit);

            // Return the completed view to render on screen
            return convertView;

        }

    }

    class getFeedInfoTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getFeedInfo(params[0], params[1]);
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");
                        videoInfo = new LinkedHashMap<String, JSONObject>();
                        for (int i = 0; i < videos.length(); i++) { // background?
                            JSONObject vid = videos.getJSONObject(i);
                            videoInfo.put(vid.getString("id"), vid);
                        }
                        Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                        startActivity(intentFeedStart);
                        loadFeedImageView.clearAnimation();
                        if (myVideosOnly) {
                            loadFeedTextView.setText("Tap to watch your feed");
                        } else {
                            loadFeedTextView.setText("Tap to look for Yondors near you");
                        }
                    } else {
                        Crashlytics.logException(new Exception("Server Side Failure"));
                        loadFeedImageView.clearAnimation();
                        loadFeedTextView.setText("Could not retrieve new Yondors. Please try again later");
                    }
                } else {
                    loadFeedImageView.clearAnimation();
                    loadFeedTextView.setText("Please check your connectivity and try again later");
                }
                loadFeedImageView.setClickable(true);
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
            }
        }
    }

    private void setRequests() {
        requests = new ArrayList<>();
        for (Uri uri : uris) {
            Request request = new Request(uri);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
            request.setDestinationUri(Uri.fromFile(new File(Video.loadedDir.getAbsolutePath() + "/" + uri.getLastPathSegment())));
            requests.add(request);
        }
    }

    private boolean isLoaded (String id) {
        boolean loaded = new File(Video.loadedDir.getAbsolutePath()+"/"+id).isFile();
        return loaded;
    }

}