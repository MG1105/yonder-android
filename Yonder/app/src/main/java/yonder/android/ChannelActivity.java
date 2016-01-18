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
import android.content.SharedPreferences;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ChannelActivity extends Activity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    ArrayList<Channel> channels;
    ChannelAdapter adapter;
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
    static HashMap<String, LinkedHashMap<String, JSONObject>> channelInfo = new HashMap<>();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_channel);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        Video.setPaths(mActivity);
        VerifyUserTask verifyUserTask = new VerifyUserTask();
        verifyUserTask.execute();
        User.verify(mActivity);
        User.setLocation(mActivity);
        Video.cleanup(Video.loadedDir, false);
        Video.cleanup(Video.uploadDir, true);

        spinner = (ProgressBar)findViewById(R.id.progress_channels);

        final ActionBar actionBar = getActionBar();
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                spinner.setVisibility(View.VISIBLE);
                GetChannelsTask getChannelsTask = new GetChannelsTask();
                if (tab.getText().equals("hot")) {
                    getChannelsTask.execute(userId, "hot");
                } else if (tab.getText().equals("new")) {
                    getChannelsTask.execute(userId, "new");
                } else if (tab.getText().equals("top")) {
                    getChannelsTask.execute(userId, "top");
                }

                //adapter.notifyDataSetChanged();
            }
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };
        // Add 3 tabs, specifying the tab's text and TabListener
        actionBar.addTab(actionBar.newTab().setText("hot").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("new").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("top").setTabListener(tabListener));
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary)));
        //actionBar.setStackedBackgroundDrawable(getResources().getDrawable(R.drawable.));

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


    class GetChannelsTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getChannels(params[0], params[1]);
                return response;
            } catch (Exception e) {
                Logger.log(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            try {
                TextView noChannels = (TextView)findViewById(R.id.textView_no_channels);
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                        JSONArray channelsArray = response.getJSONArray("channels");
                        channels = Channel.fromJson(channelsArray);
                        ListView listView = (ListView) findViewById(R.id.listView_channels);
                        if (channels.size() > 0) {
                            // Create the adapter to convert the array to views
                            adapter = new ChannelAdapter(mActivity);
                            // Attach the adapter to a ListView
                            listView.setAdapter(adapter);
                            noChannels.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noChannels.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        noChannels.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    noChannels.setVisibility(View.VISIBLE);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
                spinner = (ProgressBar)findViewById(R.id.progress_channels);
                spinner.setVisibility(View.GONE);
            } catch (Exception e) {
                Logger.log(e);
            }
        }
    }

    class ChannelAdapter extends ArrayAdapter<Channel> {
        Channel channel;
        TextView load;

        public ChannelAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, channels);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            channel = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_channel, parent, false);
            }

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

    class GetFeedTask extends AsyncTask<Void, Void, JSONObject> {

        Channel channel;
        protected GetFeedTask(Channel channel) {
            this.channel = channel;
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
                JSONObject response = gae.getFeed(userId, longitude, latitude, myVideosOnly, false);
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
                            channel.addVideo(id);

                            if (isLoaded(id)) {
                                continue;
                            }
                            String url = "http://storage.googleapis.com/yander/" + id + ".mp4";
                            uris.add(Uri.parse(url));
                        }

                        if (videos.length() > 0) {
                            remaining = uris.size();
                            channelInfo.put(channel.getId(), videoInfo);
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
                            channel.setLoading(false);
                            if (myVideosOnly) {
                                loadFeedTextView.setText("We did not find any Yondor you uploaded or commented on " +
                                        "in the past 24 hours");
                                loadFeedImageView.clearAnimation();
                                Logger.trackEvent(mActivity, gaCategory, "No Content");
                            }
                        }

                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        loadFeedTextView.setText("Could not retrieve new Yondors. Please try again later");
                        channel.setLoading(false);

                    }
                } catch (Exception e) {
                    Logger.log(e);
                    loadFeedTextView.setText("Could not retrieve new Yondors. Please try again later");
                    channel.setLoading(false);
                }
            } else {
                loadFeedTextView.setText("Please check your connectivity and try again later");
                channel.setLoading(false);
                //reply.setClickable(true);
            }
        }
    }

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            Logger.log(Log.INFO, TAG, "Remaining " + remaining);
            if (remaining >= 0) { // Bug < 0?
//                loadFeedTextView.setText("Loading... " + remaining);
            }
            if (remaining == 0) {
                Video.obfuscate(true);
                // channel is ready
                adapter.notifyDataSetChanged();

            }
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
        boolean loaded = new File(Video.loadedDir.getAbsolutePath()+"/"+id).isFile();
        return loaded;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_notification:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(mActivity, NotificationActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_contact:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    // Verify User

    class VerifyUserTask extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            try {
                AppEngine gae = new AppEngine();
                Logger.log(Log.INFO, TAG, "Verifying user id " + userId);
                JSONObject response = gae.verifyUser(userId);
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        JSONObject userObject = response.getJSONObject("user");
                        return userObject;
                    } else { // server side failure
                        Logger.log(new Exception("Server Side failure"));
                        return null;
                    }
                } else return null; // no internet
            } catch (Exception e) {
                Logger.log(e);;
                return null;
            }
        }

        protected void onPostExecute(JSONObject user) {
            if (user != null) {
                try {
                    SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                            "yonder.android", Context.MODE_PRIVATE);
                    sharedPreferences.edit().putInt("upgrade", user.getInt("upgrade")).apply();
                    sharedPreferences.edit().putInt("ban", user.getInt("ban")).apply();
                    if (user.getInt("warn") != 0 ) {
                        Alert.showWarning(mActivity);
                    } else if (user.getInt("upgrade") != 0) {
                        Alert.forceUpgrade(mActivity, user.getInt("upgrade"));
                    } else if (user.getInt("ban") != 0) {
                        Alert.ban(mActivity, user.getInt("ban"));
                    }
                } catch (JSONException e) {
                    Logger.log(e);;
                }
            } else { // could also be server side failure
                Toast.makeText(mActivity, "Could not connect to the Internet. Please try again later", Toast.LENGTH_LONG).show();
                finish();
            }

        }
    }

}