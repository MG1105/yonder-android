package com.vidici.android;

import android.app.AlertDialog;
import android.app.DownloadManager;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
    Activity mActivity;
    String userId, channelSort;
    ActionBar actionBar;
    private ShareActionProvider mShareActionProvider;
    FragmentManager fragmentManager;
    ViewPager mViewPager;
    PagerAdapter mainPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_main);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        userId = User.getId(this);
        User.verify(mActivity);

        fragmentManager = getSupportFragmentManager();

        actionBar = getSupportActionBar();
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionBar.setDisplayShowTitleEnabled(false);
//        actionBar.setDisplayShowHomeEnabled(false);
        // Create a tab listener that is called when the user changes tabs.

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

        mainPagerAdapter = new MainPagerAdapter(fragmentManager);
        mViewPager.setAdapter(mainPagerAdapter);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
                mViewPager.setCurrentItem(tab.getPosition());
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
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary_color)));

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.log(Log.INFO, TAG, "Destroying Activity");
        Video.cleanup(Video.loadedDir);
        Video.cleanup(Video.uploadDir);
    }

    public class MainPagerAdapter extends FragmentPagerAdapter {
        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                Logger.trackEvent(mActivity, "Feed", "View");
                Logger.log(Log.INFO, TAG, "Viewing Feed");
                FeedFragment fragment = new FeedFragment();
                return fragment;
            } else if (i == 1) {
                channelSort = "hot";
                Logger.trackEvent(mActivity, "Channel", "View Hot");
                Logger.log(Log.INFO, TAG, "Hot channel view");
                ChannelFragment fragment = new ChannelFragment();
                Bundle bundle = new Bundle(1);
                bundle.putString("channelSort", channelSort);
                fragment.setArguments(bundle);
                return fragment;
            } else if (i == 2) {
                channelSort = "new";
                Logger.trackEvent(mActivity, "Channel", "View New");
                Logger.log(Log.INFO, TAG, "New channel view");
                ChannelFragment fragment = new ChannelFragment();
                Bundle bundle = new Bundle(1);
                bundle.putString("channelSort", channelSort);
                fragment.setArguments(bundle);
                return fragment;
            } else if (i == 3) {
                channelSort = "top";
                Logger.trackEvent(mActivity, "Channel", "View Top");
                Logger.log(Log.INFO, TAG, "Top channel view");
                ChannelFragment fragment = new ChannelFragment();
                Bundle bundle = new Bundle(1);
                bundle.putString("channelSort", channelSort);
                fragment.setArguments(bundle);
                return fragment;
            } else if (i == 4) {
                SharedPreferences sharedPreferences = mActivity.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
                if (!sharedPreferences.getBoolean("logged_in", false)) {
                    Logger.trackEvent(mActivity, "Profile", "Login View");
                    Logger.log(Log.INFO, TAG, "Viewing Login fragment");
                    LoginFragment fragment = new LoginFragment();
                    return fragment;
                } else {
                    Logger.trackEvent(mActivity, "Profile", "View");
                    Logger.log(Log.INFO, TAG, "Viewing Profile");
                    ProfileFragment fragment = new ProfileFragment();
                    Bundle bundle = new Bundle(1);
                    bundle.putString("user_id", sharedPreferences.getString("facebook_id", ""));
                    fragment.setArguments(bundle);
                    return fragment;
                }
            }

            return null;
        }

        @Override
        public int getCount() {
            return 5;
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





























}