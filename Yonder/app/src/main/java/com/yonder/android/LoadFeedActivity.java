package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class LoadFeedActivity extends ActionBarActivity {

    Animation rotation;
    ImageView loadFeedImageView;
    TextView loadFeedTextView;
    Activity mActivity;
    ArrayList<Request> requests;
    ArrayList<Uri> uris = new ArrayList<>();
    int remaining;

    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.animation_enter,
                R.anim.animation_leave);
        mActivity = this;
        setContentView(R.layout.activity_load_feed);
        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loadFeedImageView = (ImageView) findViewById(R.id.loadFeedImageView);
        loadFeedImageView.setOnClickListener(loadListener);

    }


    OnClickListener loadListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            loadFeedImageView.startAnimation(rotation);
            loadFeedTextView = (TextView) findViewById(R.id.loadFeedTextView);
            loadFeedTextView.setText("Loading...");
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            setVideoUris();
            remaining = uris.size();
            setRequests();
            for (Request request : requests) {
                downloadManager.enqueue(request);
            }
            registerReceiver(loadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    };

    BroadcastReceiver loadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            remaining--;
            loadFeedTextView.setText("Remaining = " + remaining);
            if (remaining == 0) {
                Intent intentFeedStart = new Intent(mActivity, FeedActivity.class);
                startActivity(intentFeedStart);
            }
        }
    };

    private void setVideoUris() {
        uris = new ArrayList<>();
//        uris.add(Uri.parse("http://storage.googleapis.com/yander/20150306_235451_001.mp4"));
//		uris.add(Uri.parse("http://storage.googleapis.com/yander/20130810_182659.mp4"));
//		uris.add(Uri.parse("http://storage.googleapis.com/yander/20120809_015947.mp4"));
		uris.add(Uri.parse("http://storage.googleapis.com/yander/1436326261092.mp4"));

    }

    private void setRequests() {
        requests = new ArrayList<>();
        for (Uri uri : uris) {
            Request request = new Request(uri);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalFilesDir(mActivity, "loaded_videos", uri.getLastPathSegment());
            requests.add(request);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(loadBroadcastReceiver);

    }

    // Menu Code

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_load_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}