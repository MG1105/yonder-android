package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class NotificationActivity extends AppCompatActivity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    ArrayList notifications;
    static NotificationAdapter adapter;
    private ProgressBar spinner;
    Activity mActivity;
    String userId;
    String gaCategory;

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

    @Override
    protected void onStop() {
        super.onStop();
        Logger.log(Log.INFO, TAG, "Stopping Activity");
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
                TextView noNotifications = (TextView)findViewById(R.id.textView_no_notifications);
                ImageView noNotificationsImage = (ImageView) findViewById(R.id.image_no_notifications);
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
                            noNotifications.setVisibility(View.GONE);
                            noNotificationsImage.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noNotifications.setVisibility(View.VISIBLE);
                            noNotificationsImage.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        noNotifications.setVisibility(View.VISIBLE);
                        noNotificationsImage.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    noNotifications.setVisibility(View.VISIBLE);
                    noNotificationsImage.setVisibility(View.VISIBLE);
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

            TextView notificationBody = (TextView) convertView.findViewById(R.id.textView_notification_body);
            load = (TextView) convertView.findViewById(R.id.textView_notification_load);

            notificationBody.setText(notification.getContent());

            if (notification.getChannelId().equals("") && notification.getVideoId().equals("")) {
                load.setVisibility(View.GONE);
            } else {
                load.setVisibility(View.VISIBLE);
            }

            if (notification.isPlayable()) {
                load.setText("play");
                load.setEnabled(true);
            } else if (notification.isDownloading()) {
                load.setEnabled(false);
            } else if (notification.isVideosEmpty()) {
                load.setText("load");
                load.setEnabled(true);
            } else if (notification.isEmpty()) {
                load.setText("no reactions yet");
                load.setEnabled(true);
            }

            load.setOnClickListener(new View.OnClickListener() {
                Notification myNotification = notification;
                TextView myLoad = load;

                @Override
                public void onClick(View v) {
                    if (notification.isPlayable() && !GetVideosTask.loading) {
                        Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
                        intentFeedStart.putExtra("notificationId", myNotification.getId());
                        Logger.log(Log.INFO, TAG, notificationInfo.toString());
                        Logger.log(Log.INFO, TAG, myNotification.getId());
                        startActivity(intentFeedStart);
                        myNotification.setReload();
                        adapter.notifyDataSetChanged();
                        myLoad.setText("load");
                    } else if (!GetVideosTask.loading) {
                        GetVideosTask.loading = true;
                        myLoad.setEnabled(false);
                        myNotification.setDownloading(true);
                        gaCategory = "Notification";
                        Logger.trackEvent(mActivity, gaCategory, "Load Request");
                        myLoad.setText("loading...");
                        GetVideosTask getFeed = new GetVideosTask(mActivity, myNotification, myNotification.getId(), adapter, notificationInfo, "notification");
                        getFeed.execute();
                    }

                }
            });
            // Return the completed view to render on screen
            return convertView;
        }
    }
}