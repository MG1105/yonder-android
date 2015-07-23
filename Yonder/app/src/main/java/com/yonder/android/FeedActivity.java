package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FeedActivity extends Activity {
    private final String TAG = "Log." + this.getClass().getSimpleName();
	private int tap = 0;
	private GestureDetectorCompat mDetector;
    VideoView currentVideo;
    String currentVideoId;
    String listIds;
    ArrayList<Uri> uris;
    File listFile[];
    Button flagButton;
    Button likeButton;
    Button dislikeButton;
    Button commentButton;
    Button ratingButton;
    private Activity myContext;
    Map<String, JSONObject> videoInfo;
	boolean infoReceived;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
	    uris = getContent();

        flagButton = (Button) findViewById(R.id.flag_button);
        likeButton = (Button) findViewById(R.id.like_button);
        dislikeButton = (Button) findViewById(R.id.dislike_button);
        commentButton = (Button) findViewById(R.id.comment_button);
        ratingButton = (Button) findViewById(R.id.rating);

        likeButton.setOnClickListener(likeListener);
        dislikeButton.setOnClickListener(dislikeListener);
        flagButton.setOnClickListener(flagListener);
	    commentButton.setOnClickListener(commentListener);

        commentButton.setVisibility(View.GONE);
        ratingButton.setVisibility(View.GONE);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        currentVideo = (VideoView) findViewById(R.id.currentVideo);
	    currentVideo.setVideoURI(uris.get(0));
        currentVideoId = uris.get(0).getLastPathSegment().substring(0,13);
    }

	@Override
	protected void onResume() {
		super.onResume(); // loop?
		currentVideo.start();
		if (CommentActivity.comments != null) {
			commentButton.setText(CommentActivity.comments.size() + " Comments");
		}
	}

	@Override
	protected void onDestroy() { // Maybe let them resume viewing?
		super.onDestroy(); // Not called all the time
		if (listFile != null) {
			for (int i = 0; i < listFile.length; i++) {
				listFile[i].delete();
			}
		}
		finish();
	}

	public ArrayList<Uri> getContent() {
		uris = new ArrayList<>();
        File videosFolder  = this.getExternalFilesDir("loaded_videos");
        listFile = videosFolder.listFiles();
        listIds = "";
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                uris.add(Uri.parse(listFile[i].getAbsolutePath()));
                if (i < listFile.length - 1) {
                    listIds = listIds + uris.get(i).getLastPathSegment().substring(0,13) + "xxx";
                } else {
                    listIds = listIds + uris.get(i).getLastPathSegment().substring(0,13);
                }

            }
        }
        Log.d(TAG, "uris = " + uris.toString());
        getFeedInfoTask infoTask = new getFeedInfoTask();
        infoTask.execute(listIds);
        return uris;
	}

    protected void showVideoInfo(int myRating) {
        if (infoReceived) {
		    try {
			    String commentsTotal = videoInfo.get(currentVideoId).getString("comments_total"); // total wrong when old video left
			    String rating = videoInfo.get(currentVideoId).getString("rating");
			    commentButton.setText(commentsTotal + " Comments");
			    int latestRating = Integer.valueOf(rating) + myRating;
			    ratingButton.setText(latestRating + " Likes");
		    } catch (JSONException e) {
			    e.printStackTrace();
		    }
		    commentButton.setVisibility(View.VISIBLE);
		    ratingButton.setVisibility(View.VISIBLE);
	    } else {
	        // try again, too slow or failed to get it?
        }
    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RateTask rate = new RateTask(); // spinner and disappear buttons
            rate.execute(currentVideoId, "1");
            showVideoInfo(1);
        }
    };

    View.OnClickListener dislikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RateTask rate = new RateTask();
            rate.execute(currentVideoId, "-1");
            showVideoInfo(-1);
        }
    };

    View.OnClickListener flagListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ReportTask report = new ReportTask();
            report.execute(currentVideoId);
        }
    };

	View.OnClickListener commentListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(myContext, CommentActivity.class);
			intent.putExtra("videoId", currentVideoId);
			startActivity(intent);
		}
	};


	class RateTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            JSONObject response = gae.rateVideo(params[0], params[1]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response.getString("success").equals("1")) {
                    Toast toast = Toast.makeText(myContext, "Rated!", Toast.LENGTH_LONG); //Liked? or Disliked?
                    toast.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class ReportTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            JSONObject response = gae.reportVideo(params[0]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response.getString("success").equals("1")) {
                    Toast toast = Toast.makeText(myContext, "Flagged!", Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class getFeedInfoTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... params) {
            try {
                AppEngine gae = new AppEngine();
                JSONObject response = gae.getFeedInfo(params[0]);
                try {
                    if (response.getString("success").equals("1")) {
                        JSONArray videos = response.getJSONArray("videos");
                        videoInfo = new HashMap<String, JSONObject>();
                        for (int i = 0; i < videos.length(); i++) { // background?
                            JSONObject vid = videos.getJSONObject(i);
                            videoInfo.put(vid.getString("id"), vid);
                        }
	                    infoReceived = true;
                        return null;
                    } else {
                        return null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

	// Handle Touch

	@Override
	public boolean onTouchEvent(MotionEvent event){
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		private static final String DEBUG_TAG = "Gestures";

		@Override
		public boolean onDown(MotionEvent event) {
			Log.d(DEBUG_TAG, "onDown: " + event.toString());
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			Log.d(TAG, "uris = " + uris.toString());
			if (tap < uris.size()-1) {
				tap++;
                currentVideo.stopPlayback();
				commentButton.setVisibility(View.GONE);
				ratingButton.setVisibility(View.GONE);
                currentVideo.setVideoURI(uris.get(tap));
                currentVideoId = uris.get(tap).getLastPathSegment().substring(0,13);
                currentVideo.start();
				return true;
			}
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) { // or replay auto on?
			currentVideo.start();
			return true;
		}
	}
}