package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
    static NotificationAdapter adapter;
    private ProgressBar spinner;
    Activity mActivity;
    String userId;
    String gaCategory;
    static boolean active = false;

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
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.setNotificationAlarm(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.log(Log.INFO, TAG, "Pausing Activity");
        Logger.fbActivate(this, false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.log(Log.INFO, TAG, "Stopping Activity");
        active = false;
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

            // Lookup view for data population
            TextView notificationBody = (TextView) convertView.findViewById(R.id.textView_notification_body);
            notificationBody.setText(notification.getContent());

            load = (TextView) convertView.findViewById(R.id.textView_notification_load);
            if (notification.getChannelId().equals("") && notification.getVideoId().equals("")) {
                load.setVisibility(View.INVISIBLE);
            } else {
                load.setVisibility(View.VISIBLE);
            }

            int toLoad = notification.getRemaining();
            if (toLoad == 0) {
                load.setText("play");
                load.setEnabled(true);
            } else if (toLoad > 0) {
                load.setText("loading... " + toLoad);
            }


            load.setOnClickListener(new View.OnClickListener() {
                Notification myNotification = notification;
                TextView myLoad = load;
                int remaining = myNotification.getRemaining();

                @Override
                public void onClick(View v) {
                    if (remaining == 0) {
                        Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                        intentFeedStart.putExtra("notificationId", myNotification.getId());
                        startActivity(intentFeedStart);
                        myNotification.setReload();
                        adapter.notifyDataSetChanged();
                        myLoad.setText("load");
                    } else {
                        myLoad.setEnabled(false);
                        gaCategory = "Notification";
                        Logger.trackEvent(mActivity, gaCategory, "Load Request");
                        myLoad.setText("loading...");
                        GetFeedTask getFeed = new GetFeedTask(mActivity, myNotification, myNotification.getId(), adapter, notificationInfo, myLoad);
                        getFeed.execute();
                    }

                }
            });

            // Return the completed view to render on screen
            return convertView;

        }

    }

}