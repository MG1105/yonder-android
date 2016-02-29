package yonder.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;

public class NotificationActivity extends Activity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    ArrayList notifications;
    NotificationAdapter adapter;
    private ProgressBar spinner;
    Animation rotation;
    ImageView loadFeedImageView;
    TextView loadFeedTextView;
    Activity mActivity;
    ArrayList<Request> requests;
    ArrayList<Uri> uris = new ArrayList<>();
    int remaining = 0;
    static boolean myVideosOnly;
    String userId, longitude, latitude;
    Timer timer;
    String listIds;
    String score;
    Switch myVideosOnlySwitch;
    String gaCategory;

    private DownloadManager downloadManager;
    static HashMap<String, LinkedHashMap<String, JSONObject>> notificationInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_notification);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        spinner = (ProgressBar)findViewById(R.id.progress_notifications);
        GetNotificationsTask getNotificationTask = new GetNotificationsTask();
        getNotificationTask.execute(userId, "1");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.log(Log.INFO, TAG, "Resuming Activity");
        Logger.fbActivate(this, true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.log(Log.INFO, TAG, "Pausing Activity");
        Logger.fbActivate(this, false);
    }


    class GetNotificationsTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getNotifications(params[0], params[1]);
                return response;
            } catch (Exception e) {
                Logger.log(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            try {
                TextView noNotfications = (TextView)findViewById(R.id.textView_no_notifications);
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                        JSONArray notificationsArray = response.getJSONArray("notifications");
                        notifications = Notification.fromJson(notificationsArray);
                        ListView listView = (ListView) findViewById(R.id.listView_notifications);
                        if (notifications.size() > 0) {
                            // Create the adapter to convert the array to views
                            adapter = new NotificationAdapter(mActivity);
                            // Attach the adapter to a ListView
                            listView.setAdapter(adapter);
                            noNotfications.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noNotfications.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        noNotfications.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    noNotfications.setVisibility(View.VISIBLE);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
                spinner = (ProgressBar)findViewById(R.id.progress_notifications);
                spinner.setVisibility(View.GONE);
            } catch (Exception e) {
                Logger.log(e);
            }
        }
    }

    class NotificationAdapter extends ArrayAdapter<Notification> {
        Notification notification;
        TextView load;

        public NotificationAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, notifications);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            notification = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);
            }

            if (notification.getChannelId() == null && notification.getVideoId() == null) {
                load.setVisibility(View.INVISIBLE);
            }

            // Lookup view for data population
            TextView notificationBody = (TextView) convertView.findViewById(R.id.textView_notification_body);
            notificationBody.setText(notification.getContent());


            load = (TextView) convertView.findViewById(R.id.textView_notification_load);
            if (notification.isLoaded()) {
                notification.setLoading(false);
                load.setText("Play");
            }


            load.setOnClickListener(new View.OnClickListener() {
                Notification myNotification = notification;
                TextView myLoad = load;
                Boolean loaded = myNotification.isLoaded();
                Boolean loading = myNotification.isLoading();

                @Override
                public void onClick(View v) {
                    if (loading) {
                        Logger.log(Log.INFO, TAG, "Ignoring click as channel is loading");
                    } else if (loaded) {
                        Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                        intentFeedStart.putExtra("notificationId", myNotification.getId());
                        startActivity(intentFeedStart);
                        myNotification.setReload();
                        adapter.notifyDataSetChanged();
                        myLoad.setText("Load");
                    } else {
                        myNotification.setLoading(true);
                        gaCategory = "Notification";
                        Logger.trackEvent(mActivity, gaCategory, "Load Request");
                        myLoad.setText("Loading...");
                        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        GetFeedTask getFeed = new GetFeedTask(myNotification, myLoad);
                        getFeed.execute();
                    }

                }
            });

            // Return the completed view to render on screen
            return convertView;

        }

    }

    class GetFeedTask extends AsyncTask<Void, Void, JSONObject> {

        Notification notification;
        TextView load;

        protected GetFeedTask(Notification notification, TextView myLoad) {
            this.notification = notification;
            load = myLoad;
        }

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
                Logger.log(Log.INFO, TAG, String.format("Getting feed for userId %s longitude %s latitude %s myVideosOnly %s",
                        userId, longitude, latitude, myVideosOnly));
                JSONObject response = gae.getFeed(userId, notification.getChannelId(), notification.getVideoId());
                return response;
            } catch (Exception e) {
                Logger.log(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            if (response != null) {
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");

                        LinkedHashMap<String, JSONObject> videoInfo = new LinkedHashMap<>();
                        for (int i = 0; i < videos.length(); i++) { // background?
                            JSONObject vid = videos.getJSONObject(i);
                            videoInfo.put(vid.getString("id"), vid);

                            String id = vid.getString("id");
                            notification.addVideo(id);

                            if (isLoaded(id)) {
                                continue;
                            }
                            String url = "http://storage.googleapis.com/yander/" + id + ".mp4";
                            uris.add(Uri.parse(url));
                        }

                        if (videos.length() > 0) {
                            remaining += uris.size();
                            notificationInfo.put(notification.getId(), videoInfo);
                            setRequests();
                            for (Request request : requests) {
                                downloadManager.enqueue(request);
                            }
                            if (remaining == 0) {
                                Logger.log(Log.INFO, TAG, "All videos in cache");
                                // channel is ready
                                adapter.notifyDataSetChanged();
                                Logger.log(Log.INFO, TAG, "Channel modified");
                            } else {
                                Logger.log(Log.INFO, TAG, remaining + " videos to download");
                                registerReceiver(loadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                            }
                        } else {
                            notification.setLoading(false);
                            load.setText("No videos found");
                        }

                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                        load.setText("Load");
                        notification.setLoading(false);

                    }
                } catch (Exception e) {
                    Logger.log(e);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                    load.setText("Load");
                    notification.setLoading(false);
                }
            } else {
                Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                load.setText("Load");
                notification.setLoading(false);
            }
        }
    }

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            Logger.log(Log.INFO, TAG, "Remaining " + remaining);
            adapter.notifyDataSetChanged();
        }
    };

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
        boolean loaded = new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile();
        return loaded;
    }

}