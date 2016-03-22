package com.vidici.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
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
import android.support.v7.app.AppCompatActivity;
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
    static ChannelAdapter adapter;
    private ProgressBar spinner;
    Activity mActivity;
    String userId;
    String gaCategory;
    ActionBar actionBar;
    static boolean active = false;
    static HashMap<String, LinkedHashMap<String, JSONObject>> channelInfo = new HashMap<>();
    FloatingActionButton addChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_channel);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);

        spinner = (ProgressBar)findViewById(R.id.progress_channels);
        addChannel = (FloatingActionButton)findViewById(R.id.button_add_channel);

        actionBar = getSupportActionBar();
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
                spinner.setVisibility(View.VISIBLE);
                GetChannelsTask getChannelsTask = new GetChannelsTask();
                if (tab.getText().equals("hot")) {
                    User.setId(mActivity, "user1");
                    userId = "user1";
                    getChannelsTask.execute(userId, "hot");
                } else if (tab.getText().equals("new")) {
                    User.setId(mActivity, "user2");
                    userId = "user2";
                    getChannelsTask.execute(userId, "new");
                } else if (tab.getText().equals("top")) {
                    User.setId(mActivity, "user3");
                    userId = "user3";
                    getChannelsTask.execute(userId, "top");
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
        actionBar.addTab(actionBar.newTab().setText("hot").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("new").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("top").setTabListener(tabListener));
        //actionBar.setStackedBackgroundDrawable(getResources().getDrawable(R.drawable.));

        addChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.setMessage("Create a new channel");

                final EditText input = new EditText(mActivity);
                input.setHint("#channelname");
                alertDialog.setView(input);

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
        active = true;
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
        TextView rating;
        TextView reply;
        TextView unseen;
        Button likeButton;
        Button dislikeButton;

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
            reply = (TextView) convertView.findViewById(R.id.textView_channel_reply);
            int toLoad = channel.getRemaining();
            if (toLoad == 0) {
                load.setText("play");
                unseen.setText("");
                load.setEnabled(true);
            } else if (toLoad > 0) {
                load.setText("loading... " + toLoad);
            }
            if (channel.getRated() == 1) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(false);
            } else if (channel.getRated() == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
            }

            // Lookup view for data population
            TextView name = (TextView) convertView.findViewById(R.id.textView_channel_name);
            name.setText((position+1) + ". #" + channel.getName());
            rating.setText(channel.getRating());
            if (!channel.getUnseen().equals("0")) {
                unseen.setText(channel.getUnseen() + " new");
            }


            load.setOnClickListener(new View.OnClickListener() {
                Channel myChannel = channel;
                TextView myLoad = load;
                int remaining = myChannel.getRemaining();

                @Override
                public void onClick(View v) {
                    if (remaining == 0) {
                        Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                        intentFeedStart.putExtra("channelId", myChannel.getId());
                        startActivity(intentFeedStart);
                        myChannel.setReload();
                        adapter.notifyDataSetChanged();
                        myLoad.setText("load");
                    } else {
                        myLoad.setEnabled(false);
                        gaCategory = "Channel";
                        Logger.trackEvent(mActivity, gaCategory, "Load Request");
                        myLoad.setText("loading...");
                        Logger.log(Log.INFO, TAG, "Loading channel " + myChannel.getName());
                        GetFeedTask getFeed = new GetFeedTask(mActivity, myChannel, myChannel.getId(), adapter, channelInfo, myLoad);
                        getFeed.execute();
                    }

                }
            });

            reply.setOnClickListener(new View.OnClickListener() {
                String myChannelId = channel.getId();

                @Override
                public void onClick(View v) {
                    reply.setEnabled(false);
                    Toast.makeText(mActivity, "Opening your camera...", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ChannelActivity.this,CameraPreviewActivity.class);
                    intent.putExtra("channelId", myChannelId);
                    startActivity(intent);
                    reply.setEnabled(true);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_notification:
                Intent intent = new Intent(mActivity, NotificationActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_contact:
                showContactForm();
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
        alertDialog.setMessage("You got feedback? This is a direct line to our CEO");

        LinearLayout layout = new LinearLayout(mActivity);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText subject = new EditText(mActivity);
        subject.setHint("subject");
        layout.addView(subject);
        final EditText email = new EditText(mActivity);
        email.setHint("your email");
        layout.addView(email);
        final EditText message = new EditText(mActivity);
        message.setHint("message");
        layout.addView(message);
        alertDialog.setView(layout);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        layout.f
                        String message = input.getText().toString().trim();
                        alertDialog.dismiss();
                        ContactUsTask contactUsTask = new ContactUsTask();
                        contactUsTask.execute(userId, message);
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
            JSONObject response = gae.contactUs(params[0], params[1]);
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
                        adapter.notifyDataSetChanged();
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
        String pattern= "^[a-zA-Z0-9]*$";
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
            Toast.makeText(mActivity, "Cannot be longer than 32 letters", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

}