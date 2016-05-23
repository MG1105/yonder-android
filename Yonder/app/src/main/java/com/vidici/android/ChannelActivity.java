package com.vidici.android;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ChannelActivity extends AppCompatActivity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    ArrayList<Channel> channels;
    ArrayList<FeedItem> feed;
    static ChannelAdapter channelAdapter;
    static FeedAdapter feedAdapter;
    private ProgressBar spinner;
    Activity mActivity;
    String userId, channelSort;
    ActionBar actionBar;
    static boolean channelsActive = false;
    static boolean feedActive = false;
    static HashMap<String, LinkedHashMap<String, JSONObject>> channelInfo = new HashMap<>();
    static HashMap<String, LinkedHashMap<String, JSONObject>> feedInfo = new HashMap<>();
    FloatingActionButton addChannel;
    private ShareActionProvider mShareActionProvider;
    ListView feedListView, channelsListView;
    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_channel);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        User.verify(mActivity);

        spinner = (ProgressBar)findViewById(R.id.progress_channels);
        addChannel = (FloatingActionButton)findViewById(R.id.button_add_channel);
        channelsListView = (ListView) findViewById(R.id.listView_channels);
        feedListView = (ListView) findViewById(R.id.listView_feed);

        downloadManager = (DownloadManager) mActivity.getSystemService(Activity.DOWNLOAD_SERVICE);

        actionBar = getSupportActionBar();
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionBar.setDisplayShowTitleEnabled(false);
//        actionBar.setDisplayShowHomeEnabled(false);
        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
                spinner.setVisibility(View.VISIBLE);
                GetChannelsTask getChannelsTask = new GetChannelsTask();
                if (tab.getPosition() == 0) {
                    feedActive = true;
                    channelsActive = false;
                    feedListView.setVisibility(View.VISIBLE);
                    channelsListView.setVisibility(View.GONE);
                    GetFeedTask getFeedTask = new GetFeedTask();
                    getFeedTask.execute(userId);
                    Logger.trackEvent(mActivity, "Feed", "View");
                    Logger.log(Log.INFO, TAG, "Viewing Feed");
                } else if (tab.getPosition() == 1) {
                    feedActive = false;
                    channelsActive = true;
                    feedListView.setVisibility(View.GONE);
                    channelsListView.setVisibility(View.VISIBLE);
                    getChannelsTask.execute(userId, "hot");
                    channelSort = "hot";
                    Logger.trackEvent(mActivity, "Channel", "View Hot");
                    Logger.log(Log.INFO, TAG, "Hot channel view");
                } else if (tab.getPosition() == 2) {
                    feedActive = false;
                    channelsActive = true;
                    feedListView.setVisibility(View.GONE);
                    channelsListView.setVisibility(View.VISIBLE);
                    getChannelsTask.execute(userId, "new");
                    channelSort = "new";
                    Logger.trackEvent(mActivity, "Channel", "View New");
                    Logger.log(Log.INFO, TAG, "New channel view");
                } else if (tab.getPosition() == 3) {
                    feedActive = false;
                    channelsActive = true;
                    feedListView.setVisibility(View.GONE);
                    channelsListView.setVisibility(View.VISIBLE);
                    getChannelsTask.execute(userId, "top");
                    channelSort = "top";
                    Logger.trackEvent(mActivity, "Channel", "View Top");
                    Logger.log(Log.INFO, TAG, "Top channel view");
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

            }
        };

        // Add 3 tabs, specifying the tab's text and TabListener
        actionBar.addTab(actionBar.newTab().setCustomView(findViewById(R.id.tab_icon_home)).setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setCustomView(findViewById(R.id.tab_icon_hot)).setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setCustomView(findViewById(R.id.tab_icon_new)).setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setCustomView(findViewById(R.id.tab_icon_top)).setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setCustomView(findViewById(R.id.tab_icon_profile)).setTabListener(tabListener));
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary)));


        addChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.setMessage("Create a new hashtag");

                final EditText input = new EditText(mActivity);
                input.setHint("#howhighcanyourankit");
                alertDialog.setView(input);
                Logger.trackEvent(mActivity, "Channel", "Click Add");
                Logger.log(Log.INFO, TAG, "Open add channel dialog");

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String name = input.getText().toString().trim();
                                if (isValidChannelName(name)) {
                                    alertDialog.dismiss();
                                    Toast.makeText(mActivity, "Creating " + name + "...", Toast.LENGTH_LONG).show();
                                    AddChannelTask addChannelTask = new AddChannelTask();
                                    addChannelTask.execute(userId, name.substring(1));
                                }
                            }
                        });
                    }
                });
                alertDialog.show();
            }
        });
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
                ImageView noChannelsImage = (ImageView)findViewById(R.id.image_no_channels);
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                        JSONArray channelsArray = response.getJSONArray("channels");
                        channels = Channel.fromJson(channelsArray);
                        ListView listView = (ListView) findViewById(R.id.listView_channels);
                        if (channels.size() > 0) {
                            // Create the adapter to convert the array to views
                            channelAdapter = new ChannelAdapter(mActivity);
                            // Attach the adapter to a ListView
                            listView.setAdapter(channelAdapter);
                            noChannels.setVisibility(View.GONE);
                            noChannelsImage.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noChannels.setVisibility(View.VISIBLE);
                            noChannelsImage.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        noChannels.setVisibility(View.VISIBLE);
                        noChannelsImage.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    noChannels.setVisibility(View.VISIBLE);
                    noChannelsImage.setVisibility(View.VISIBLE);
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
        TextView rating, ranking;
        TextView reply;
        TextView unseen;
        TextView name;
        Button likeButton;
        Button dislikeButton;
        ProgressBar progressThumbnail, progressLoading;
        ImageView thumbnail, overlay;

        public ChannelAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, channels);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            channel = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_channel, parent, false);
            }

            load = (TextView) convertView.findViewById(R.id.textView_channel_load);
            likeButton = (Button) convertView.findViewById(R.id.button_channel_like);
            dislikeButton = (Button) convertView.findViewById(R.id.button_channel_dislike);
            rating = (TextView) convertView.findViewById(R.id.textView_channel_item_rating);
            unseen = (TextView) convertView.findViewById(R.id.textView_channel_new);
//            total = (TextView) convertView.findViewById(R.id.textView_channel_total);
            reply = (TextView) convertView.findViewById(R.id.textView_channel_reply);
            name = (TextView) convertView.findViewById(R.id.textView_channel_name);
            ranking = (TextView) convertView.findViewById(R.id.textView_channel_item_ranking);
            thumbnail = (ImageView) convertView.findViewById(R.id.imageView_channel_item);
            progressThumbnail = (ProgressBar) convertView.findViewById(R.id.progress_channel_thumbnail);
            progressLoading = (ProgressBar) convertView.findViewById(R.id.progress_channel_loading);
            overlay = (ImageView) convertView.findViewById(R.id.imageView_channel_item_overlay);


            File thumbnailFile = new File(Video.loadedDir.getPath()+"/"+channel.getThumbnailId()+".jpg");
            if (thumbnailFile.exists()) {
                progressThumbnail.setVisibility(View.INVISIBLE);
                thumbnail.setImageURI(Uri.fromFile(thumbnailFile));
            } else {
                progressThumbnail.setVisibility(View.VISIBLE);
                downloadThumbnail(Uri.parse("http://storage.googleapis.com/yander/" + channel.getThumbnailId() + ".jpg"));
            }

            if (channel.getRemaining() == 0 || channel.canPlay()) {
                load.setText("play");
                unseen.setText("");
                load.setEnabled(true);
                progressLoading.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.INVISIBLE);
            } else if (channel.getRemaining() > 0) {
                load.setText("loading " + channel.getRemaining());
                progressLoading.setVisibility(View.VISIBLE);
                overlay.setVisibility(View.VISIBLE);
                load.setEnabled(false);
            } else if (channel.getRemaining() == -1) {
                if (channel.isFetchingVideos()) {
                    load.setText("loading...");
                    progressLoading.setVisibility(View.VISIBLE);
                    overlay.setVisibility(View.VISIBLE);
                    load.setEnabled(false);
                } else {
                    load.setText("load");
                    progressLoading.setVisibility(View.INVISIBLE);
                    overlay.setVisibility(View.VISIBLE);
                    load.setEnabled(true);
                }
            } else if (channel.getRemaining() == -2) {
                load.setText("no reactions yet");
                progressLoading.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.VISIBLE);
                load.setEnabled(true);
            }

            if (channel.getRated() == 1) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(false);
                likeButton.setBackgroundResource(R.drawable.ic_up_dark);
                dislikeButton.setBackgroundResource(R.drawable.ic_down);
            } else if (channel.getRated() == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_dark);
                likeButton.setBackgroundResource(R.drawable.ic_up);
            } else {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(true);
                likeButton.setBackgroundResource(R.drawable.ic_up);
                dislikeButton.setBackgroundResource(R.drawable.ic_down);
            }

            if (User.admin) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(true);
            }

            if (!channel.getUnseen().equals("0")) {
                unseen.setText(channel.getUnseen() + " new");
            } else {
                unseen.setText("");
            }

            name.setText("#" + channel.getName());
            ranking.setText(""+ (position+1));
            rating.setText(channel.getRating());
//            total.setText(channel.getCount()+ " reactions");

            convertView.setOnClickListener(new View.OnClickListener() {
                Channel myChannel = channel;
                TextView myLoad = load;
                int remaining = myChannel.getRemaining();
                ProgressBar myProgressLoading = progressLoading;

                @Override
                public void onClick(View v) {
                    if (remaining == 0 || myChannel.canPlay()) {
                        Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
                        intentFeedStart.putExtra("channelId", myChannel.getId());
                        startActivity(intentFeedStart);
                        myChannel.setReload();
                        channelAdapter.notifyDataSetChanged();
                        myLoad.setText("load");
                    } else {
                        myLoad.setEnabled(false);
                        myChannel.setFetchingVideos(true);
                        Logger.trackEvent(mActivity, "Channel", "Load Request");
                        myLoad.setText("loading...");
                        myProgressLoading.setVisibility(View.VISIBLE);
                        Logger.log(Log.INFO, TAG, "Loading channel " + myChannel.getName());
                        GetVideosTask getFeed = new GetVideosTask(mActivity, myChannel, myChannel.getId(), channelAdapter, channelInfo, myLoad, channelSort);
                        getFeed.execute();
                    }

                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                String myChannelId = channel.getId();
                String myChannelName = channel.getName();

                @Override
                public boolean onLongClick(View v) {
                    reply.setEnabled(false);
                    Toast.makeText(mActivity, "Opening your camera...", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ChannelActivity.this,CameraPreviewActivity.class);
                    intent.putExtra("channelId", myChannelId);
                    intent.putExtra("channelName", myChannelName);
                    startActivity(intent);
                    reply.setEnabled(true);
                    Logger.trackEvent(mActivity, "Channel", "Reply");
                    return true;
                }
            });

            likeButton.setOnClickListener(new View.OnClickListener() {
                Channel myChannel = channel;
                Button myLike = likeButton;
                Button myDislike = dislikeButton;
                @Override
                public void onClick(View v) {
                    myDislike.setEnabled(false);
                    myLike.setEnabled(false);
                    RateChannelTask rateChannelTask = new RateChannelTask(myChannel, myLike, myDislike);
                    rateChannelTask.execute("1", User.getId(mActivity));
                    myChannel.setRating(1);
                    Logger.log(Log.INFO, TAG, "Liking channel");
                    Logger.trackEvent(mActivity, "Channel", "Like Channel");
                }
            });

            dislikeButton.setOnClickListener(new View.OnClickListener() {
                Channel myChannel = channel;
                Button myLike = likeButton;
                Button myDislike = dislikeButton;
                @Override
                public void onClick(View v) {
                    myDislike.setEnabled(false);
                    myLike.setEnabled(false);
                    RateChannelTask rateChannelTask = new RateChannelTask(myChannel, myLike, myDislike);
                    rateChannelTask.execute("-1", User.getId(mActivity));
                    myChannel.setRating(-1);
                    Logger.log(Log.INFO, TAG, "Disliking channel");
                    Logger.trackEvent(mActivity, "Channel", "Dislike Channel");
                }
            });

            // Return the completed view to render on screen
            return convertView;

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_channel, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        setShareIntent();

        return true;
    }

    // Call to update the share intent
    private void setShareIntent() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "You should download this app!\n\nhttps://play.google.com/store/apps/details?id=com.vidici.android&referrer=utm_source%3Dshareapp");
        sendIntent.setType("text/plain");
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(sendIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_notification:
                Intent intent = new Intent(mActivity, NotificationActivity.class);
                startActivity(intent);
                Logger.trackEvent(mActivity, "Notification", "Click Notification Icon");
                return true;

            case R.id.action_contact:
                showContactForm();
                Logger.log(Log.INFO, TAG, "Open contact form");
                return true;

            case R.id.action_push_notifications:
                showPushNotification();
                Logger.log(Log.INFO, TAG, "Edit push notifications");
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void showContactForm() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                .setPositiveButton("SEND", null)
                .create();
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setMessage("You got feedback? We would love to hear from you");
        LayoutInflater inflater = getLayoutInflater();
        FrameLayout f1 = (FrameLayout) findViewById(android.R.id.custom); // is this the proper root?
        final View layout = inflater.inflate(R.layout.contact_form, f1);
        alertDialog.setView(layout);
        Logger.trackEvent(mActivity, "Settings", "Click Contact");

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        EditText message = (EditText) layout.findViewById(R.id.contact_message);
                        String body = message.getText().toString().trim();
                        EditText replyTo = (EditText) layout.findViewById(R.id.contact_email);
                        String reply = replyTo.getText().toString().trim();
                        if (isValidEmail(reply) && isValidMessage(body)) {
                            alertDialog.dismiss();
                            ContactUsTask contactUsTask = new ContactUsTask();
                            contactUsTask.execute(userId, body, reply);
                            Logger.trackEvent(mActivity, "Settings", "Send");
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showPushNotification() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                .setPositiveButton("OK", null)
                .setNegativeButton("CANCEL", null)
                .create();
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                "com.vidici.android", Context.MODE_PRIVATE);
        if (sharedPreferences.getString("push_notification", "on").equals("on")) {
            alertDialog.setMessage("Do you want to turn off push notifications?");
        } else {
            alertDialog.setMessage("Do you want to turn on push notifications?");
        }

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (sharedPreferences.getString("push_notification", "on").equals("on")) {
                            sharedPreferences.edit().putString("push_notification", "off").apply();
                            Logger.trackEvent(mActivity, "Settings", "Turn Off Push Notifications");
                        } else {
                            sharedPreferences.edit().putString("push_notification", "on").apply();
                            Logger.trackEvent(mActivity, "Settings", "Turn On Push Notifications");
                        }
                        alertDialog.dismiss();
                    }
                });
                Button button2 = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    class AddChannelTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            JSONObject response = gae.addChannel(params[0], params[1]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        actionBar.selectTab(actionBar.getTabAt(1));
                        GetChannelsTask getChannelsTask = new GetChannelsTask();
                        getChannelsTask.execute(userId, "new");
                        Logger.trackEvent(mActivity, "Channel", "Added Channel");
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(mActivity, "Failed to add channel", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
            }
        }
    }

    class ContactUsTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            JSONObject response = gae.contactUs(params[0], params[1], params[2]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        Toast.makeText(mActivity, "Sent", Toast.LENGTH_LONG).show();
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
            }
        }
    }

    class RateChannelTask extends AsyncTask<String, Void, JSONObject> {
        Channel myChannel;
        Button myLike;
        Button myDislike;

        protected RateChannelTask(Channel channel, Button like, Button dislike) {
            myChannel = channel;
            myLike = like;
            myDislike = dislike;
        }

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            Logger.log(Log.INFO, TAG, "Rating channel " + params[0]+ " " + params[1]);
            JSONObject response = gae.rateChannel(myChannel.getId(), params[0], params[1]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        myChannel.updateRating();
                        // myLike chane to bold
                        channelAdapter.notifyDataSetChanged();
                    } else {
                        myDislike.setEnabled(true);
                        myLike.setEnabled(true);
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                    }
                } else {
                    myDislike.setEnabled(true);
                    myLike.setEnabled(true);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
                Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                myDislike.setEnabled(true);
                myLike.setEnabled(true);
            }
        }
    }

    protected Boolean isValidChannelName(String name) {
        String pattern= "^[a-zA-Z0-9_]*$";
        if (!name.startsWith("#")) {
            Toast.makeText(mActivity, "Must start with #", Toast.LENGTH_LONG).show();
            return false;
        } else if (name.contains(" ")) {
            Toast.makeText(mActivity, "Cannot contain spaces", Toast.LENGTH_LONG).show();
            return false;
        } else if (!name.substring(1).matches(pattern)) {
            Toast.makeText(mActivity, "Cannot contain special characters", Toast.LENGTH_LONG).show();
            return false;
        } else if (name.substring(1).length() > 32) {
            Toast.makeText(mActivity, "Cannot be longer than 32 characters", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    protected Boolean isValidEmail (String email) {
        if (!email.contains("@") || !email.contains(".") || email.contains(" ")) {
            Toast.makeText(mActivity, "Please enter a valid email address", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    protected Boolean isValidMessage (String message) {
        if (message.length() == 0) {
            Toast.makeText(mActivity, "Cannot send empty message", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }



    // Home Feed

    class GetFeedTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getFeed(params[0]);
                return response;
            } catch (Exception e) {
                Logger.log(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            try {
                TextView noChannels = (TextView)findViewById(R.id.textView_no_channels);
                ImageView noChannelsImage = (ImageView)findViewById(R.id.image_no_channels);
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                        JSONArray FeedArray = response.getJSONArray("feed");
                        feed = FeedItem.fromJson(FeedArray);
                        ListView listView = (ListView) findViewById(R.id.listView_feed);
                        if (feed.size() > 0) {
                            // Create the adapter to convert the array to views
                            feedAdapter = new FeedAdapter(mActivity);
                            // Attach the adapter to a ListView
                            listView.setAdapter(feedAdapter);
                            noChannels.setVisibility(View.GONE);
                            noChannelsImage.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noChannels.setVisibility(View.VISIBLE);
                            noChannelsImage.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        noChannels.setVisibility(View.VISIBLE);
                        noChannelsImage.setVisibility(View.VISIBLE);
                    }
                } else { // no internet
                    noChannels.setVisibility(View.VISIBLE);
                    noChannelsImage.setVisibility(View.VISIBLE);
                    Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
                spinner = (ProgressBar)findViewById(R.id.progress_channels);
                spinner.setVisibility(View.GONE);
            } catch (Exception e) {
                Logger.log(e);
            }
        }
    }

    class FeedAdapter extends ArrayAdapter<FeedItem> {
        FeedItem item;
        TextView load;
        ImageView thumbnail, overlay;
        TextView rating;
        TextView comments;
        TextView channel;
        TextView caption;
        TextView username;
        ProgressBar progressThumbnail, progressLoading;

        public FeedAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, feed);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_feed, parent, false);
            }

            load = (TextView) convertView.findViewById(R.id.textView_feed_item_load);
            thumbnail = (ImageView) convertView.findViewById(R.id.imageView_feed_item);
            progressThumbnail = (ProgressBar) convertView.findViewById(R.id.progress_feed_thumbnail);
            progressLoading = (ProgressBar) convertView.findViewById(R.id.progress_feed_loading);
            overlay = (ImageView) convertView.findViewById(R.id.imageView_feed_item_overlay);
            rating = (TextView) convertView.findViewById(R.id.textView_feed_item_rating);
            caption = (TextView) convertView.findViewById(R.id.textView_feed_item_caption);
            comments = (TextView) convertView.findViewById(R.id.textView_feed_item_comments);
            channel = (TextView) convertView.findViewById(R.id.textView_feed_item_channel);
            username = (TextView) convertView.findViewById(R.id.textView_feed_item_username);

            File thumbnailFile = new File(Video.loadedDir.getPath()+"/"+item.getThumbnailId()+".jpg");
            if (thumbnailFile.exists()) {
                progressThumbnail.setVisibility(View.INVISIBLE);
                thumbnail.setImageURI(Uri.fromFile(thumbnailFile));
            } else {
                progressThumbnail.setVisibility(View.VISIBLE);
                downloadThumbnail(Uri.parse("http://storage.googleapis.com/yander/" + item.getThumbnailId() + ".jpg"));
            }

            if (item.getRemaining() == 0) {
                load.setText("play");
                load.setEnabled(true);
                progressLoading.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.INVISIBLE);
            } else if (item.getRemaining() > 0) {
                load.setText("loading " + item.getRemaining());
                load.setEnabled(false);
                progressLoading.setVisibility(View.VISIBLE);
                overlay.setVisibility(View.VISIBLE);
            } else if (item.getRemaining() == -1) {
                if (item.isFetchingVideos()) {
                    load.setText("loading...");
                    progressLoading.setVisibility(View.VISIBLE);
                    overlay.setVisibility(View.VISIBLE);
                    load.setEnabled(false);
                } else {
                    load.setText("load");
                    progressLoading.setVisibility(View.INVISIBLE);
                    overlay.setVisibility(View.VISIBLE);
                    load.setEnabled(true);
                }
            } else if (item.getRemaining() == -2) {
                load.setText("no reactions yet");
                progressLoading.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.VISIBLE);
                load.setEnabled(true);
            }

            caption.setText(item.getCaption());
            rating.setText(item.getRating());
            comments.setText(item.getComments() + " Comments");
            channel.setText("#" + item.getChannel());
            username.setText(item.getUsername());

            convertView.setOnClickListener(new View.OnClickListener() {
                FeedItem myItem = item;
                TextView myLoad = load;
                int remaining = myItem.getRemaining();
                ProgressBar myProgressLoading = progressLoading;

                @Override
                public void onClick(View v) {
                    if (remaining == 0) {
                        Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
                        intentFeedStart.putExtra("feedItemId", myItem.getId());
                        startActivity(intentFeedStart);
                        myItem.setReload();
                        feedAdapter.notifyDataSetChanged();
                        myLoad.setText("load");
                    } else {
                        myLoad.setEnabled(false);
                        myItem.setFetchingVideos(true);
                        Logger.trackEvent(mActivity, "Feed", "Load Request");
                        myLoad.setText("loading...");
                        myProgressLoading.setVisibility(View.VISIBLE);
                        Logger.log(Log.INFO, TAG, "Loading video " + myItem.getVideoId());
                        GetVideosTask getFeed = new GetVideosTask(mActivity, myItem, myItem.getId(), feedAdapter, feedInfo, myLoad, channelSort);
                        getFeed.execute();
                    }

                }
            });

            // Return the completed view to render on screen
            return convertView;

        }

    }

    protected void downloadThumbnail(Uri uri) {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setVisibleInDownloadsUi(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setDestinationUri(Uri.fromFile(new File(Video.loadedDir.getAbsolutePath() + "/" + uri.getLastPathSegment()+".tmp")));
        downloadManager.enqueue(request); // takes a few secs to start, not good

    }





























}