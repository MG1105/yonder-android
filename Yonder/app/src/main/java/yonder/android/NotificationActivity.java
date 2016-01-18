package yonder.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
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
    int remaining;
    static boolean myVideosOnly;
    String userId, longitude, latitude;
    Timer timer;
    String listIds;
    String score;
    Switch myVideosOnlySwitch;
    String gaCategory;

    private DownloadManager downloadManager;
    static LinkedHashMap<String, JSONObject> videoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_notification);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        spinner = (ProgressBar)findViewById(R.id.progress_notifications);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.log(Log.INFO, TAG, "Resuming Activity");
        Logger.fbActivate(this, true);
        GetNotificationsTask getNotificationTask = new GetNotificationsTask();
        getNotificationTask.execute(userId, "1");

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

            // Lookup view for data population
            TextView notificationBody = (TextView) convertView.findViewById(R.id.textView_notification_body);
            notificationBody.setText(notification.getContent());


            load = (TextView) convertView.findViewById(R.id.textView_channel_load);
            if (channel.isLoaded()) {
                channel.setLoading(false);
                load.setText("Play");
            }

            // Lookup view for data population
            TextView name = (TextView) convertView.findViewById(R.id.textView_channel_name);
            name.setText((position+1) + ". #" + channel.getName());


            load.setOnClickListener(new View.OnClickListener() {
                Channel myChannel = channel;
                TextView myLoad = load;
                Boolean loaded = channel.isLoaded();
                Boolean loading = channel.isLoading();

                @Override
                public void onClick(View v) {
                    if (loading) {

                    } else if (loaded) {
                        Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                        intentFeedStart.putExtra("channelId", myChannel.getId());
                        startActivity(intentFeedStart);
                    } else {
                        channel.setLoading(true);
                        gaCategory = "Channel";
                        Logger.trackEvent(mActivity, gaCategory, "Load Request");
                        myLoad.setText("Loading...");
                        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        GetFeedTask getFeed = new GetFeedTask(myChannel);
                        getFeed.execute();
                    }

                }
            });

            // Return the completed view to render on screen
            return convertView;

        }

    }

}