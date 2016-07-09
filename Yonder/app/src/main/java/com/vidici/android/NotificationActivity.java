package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
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

import java.io.File;
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
    Resources resources;

    static HashMap<String, LinkedHashMap<String, JSONObject>> notificationInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_notification);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        resources = getResources();
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
                        } else {
                            noNotifications.setVisibility(View.VISIBLE);
                            noNotificationsImage.setVisibility(View.VISIBLE);
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
        Notification item;
        ProgressBar progressThumbnail, progressLoading;
        ImageView thumbnail, bullet;

        public NotificationAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, notifications);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);
            }

            TextView notificationBody = (TextView) convertView.findViewById(R.id.textView_notification_body);
            thumbnail = (ImageView) convertView.findViewById(R.id.imageView_notification_item);
            bullet = (ImageView) convertView.findViewById(R.id.imageView_item_notification_bullet);
            progressThumbnail = (ProgressBar) convertView.findViewById(R.id.progress_notification_thumbnail);
            progressLoading = (ProgressBar) convertView.findViewById(R.id.progress_notification_loading);

            notificationBody.setText(item.getContent());

            int notificationId = item.getNotificationId();
            if (notificationId == 1) {
                bullet.setBackgroundResource(R.drawable.oval_gold);
            } else if (notificationId == 2) {
                bullet.setBackgroundResource(R.drawable.oval_purple);
            } else if (notificationId < 6) {
                bullet.setBackgroundResource(R.drawable.oval_blue);
            } else if (notificationId == 6) {
                bullet.setBackgroundResource(R.drawable.oval_green);
            } else if (notificationId == 7) {
                bullet.setBackgroundResource(R.drawable.oval_dark_green);
            } else if (notificationId < 11) {
                bullet.setBackgroundResource(R.drawable.oval_red);
            } else if (notificationId == 11) {
                bullet.setBackgroundResource(R.drawable.oval_grey);
            }

            if (item.getThumbnailId().length() > 0 && !item.isDownloadThumbnailFailed()) {
                String path = Video.loadedDir.getAbsolutePath(); // NPE
                File thumbnailFile = new File(path+"/"+item.getThumbnailId()+".jpg");
                if (thumbnailFile.exists()) {
                    Bitmap src = BitmapFactory.decodeFile(thumbnailFile.getPath());
                    RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(resources, src);
                    dr.setCircular(true);
                    thumbnail.setVisibility(View.VISIBLE);
                    thumbnail.setImageDrawable(dr);
                    progressThumbnail.setVisibility(View.INVISIBLE);
                } else {
                    thumbnail.setVisibility(View.INVISIBLE);
                    progressThumbnail.setVisibility(View.VISIBLE);
                    DownloadThumbnail downloadTask = new DownloadThumbnail(item, progressThumbnail);
                    downloadTask.execute("http://storage.googleapis.com/yander/" + item.getThumbnailId() + ".jpg");
                }
            } else {
                thumbnail.setVisibility(View.INVISIBLE);
                progressThumbnail.setVisibility(View.INVISIBLE);
            }

            if (item.isPlayable()) {
                progressLoading.setVisibility(View.INVISIBLE);
            } else if (item.isDownloading()) {
                progressLoading.setVisibility(View.VISIBLE);
            } else if (item.isVideosEmpty()) {
                progressLoading.setVisibility(View.INVISIBLE);
            } else if (item.isEmpty()) {
                progressLoading.setVisibility(View.INVISIBLE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                Notification myItem = item;
                ProgressBar myProgressLoading = progressLoading;

                @Override
                public void onClick(View v) {
                    if (myItem.isPlayable() && !GetVideosTask.loading) {
                        Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
                        intentFeedStart.putExtra("notificationId", myItem.getId());
                        startActivity(intentFeedStart);
                    } else if (!GetVideosTask.loading && myItem.isLoadable()) {
                        GetVideosTask.loading = true;
                        myItem.setDownloading(true);
                        myProgressLoading.setVisibility(View.VISIBLE);
                        Logger.trackEvent(mActivity, "Notification", "Load Request");
                        GetVideosTask getFeed = new GetVideosTask(mActivity, myItem, myItem.getId(), adapter, notificationInfo, "notification");
                        getFeed.execute();
                    }
                }
            });
            // Return the completed view to render on screen
            return convertView;
        }
    }

    class DownloadThumbnail extends DownloadTask {
        ProgressBar progressThumbnail;
        Notification item;

        DownloadThumbnail (Notification item, ProgressBar progressThumbnail) {
            super(mActivity);
            this.item = item;
            this.progressThumbnail = progressThumbnail;
        }

        @Override
        protected void onPostExecute(Integer error) {
            super.onPostExecute(error);
            if (error > 0) {
                item.setDownloadThumbnailFailed();
                progressThumbnail.setVisibility(View.INVISIBLE);
            }
            adapter.notifyDataSetChanged();
        }
    }
}