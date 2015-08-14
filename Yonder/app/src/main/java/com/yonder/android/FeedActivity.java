package com.yonder.android;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class FeedActivity extends Activity {
    // Crash? Cleanup video folders
    private final String TAG = "Log." + this.getClass().getSimpleName();
	private int tap = 0;
	private GestureDetectorCompat mDetector;
    VideoView currentVideo;
    String currentVideoId;
    ArrayList<Uri> uris;
    Button flagButton;
    Button likeButton;
    Button dislikeButton;
    Button commentButton;
    Button ratingButton;
    TextView caption;
    private Activity myContext;
    LinkedHashMap<String, JSONObject> videoInfo;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        videoInfo = LoadFeedActivity.videoInfo;
	    uris = getContent();

        flagButton = (Button) findViewById(R.id.flag_button);
        likeButton = (Button) findViewById(R.id.like_button);
        dislikeButton = (Button) findViewById(R.id.dislike_button);
        commentButton = (Button) findViewById(R.id.comment_button);
        ratingButton = (Button) findViewById(R.id.rating);
        caption = (TextView) findViewById(R.id.textView_caption);

        likeButton.setOnClickListener(likeListener);
        dislikeButton.setOnClickListener(dislikeListener);
        flagButton.setOnClickListener(flagListener);
	    commentButton.setOnClickListener(commentListener);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        currentVideo = (VideoView) findViewById(R.id.currentVideo);
	    currentVideo.setVideoURI(uris.get(0));
        currentVideoId = uris.get(0).getLastPathSegment().replace(".mp4", "");

        if (User.admin) {
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            showVideoInfo(0);
        } else {
            commentButton.setVisibility(View.GONE);
            ratingButton.setVisibility(View.GONE);
        }
    }

	@Override
	protected void onResume() {
        Video.obfuscate(myContext, false);
        super.onResume(); // loop?
		currentVideo.start();
        showCaption();
		if (CommentActivity.comments != null) {
			commentButton.setText(CommentActivity.comments.size() + " Comments");
		}
	}

	@Override
	protected void onPause() {
        Video.obfuscate(myContext, true);
        super.onPause();
	}

	public ArrayList<Uri> getContent() {
		uris = new ArrayList<>();
        String videosPath = this.getExternalFilesDir("loaded_videos").getAbsolutePath();
        Set keys = videoInfo.keySet();
        Iterator itr = keys.iterator();
        for (int i = 0; i < keys.size(); i++) {
            uris.add(Uri.parse(videosPath + "/"+itr.next()+".mp4"));
        }
        Log.d(TAG, "uris = " + uris.toString());

        return uris;
	}

    protected void showVideoInfo(int myRating) {
        try {
            String commentsTotal = videoInfo.get(currentVideoId).getString("comments_total"); // total wrong when old video left
            String rating = videoInfo.get(currentVideoId).getString("rating");
            if (commentsTotal.equals("0")) {
                commentButton.setText("Add Comment");
            } else {
                commentButton.setText(commentsTotal + " Comments");
            }

            int latestRating = Integer.valueOf(rating) + myRating;
            ratingButton.setText(latestRating + " Likes");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        commentButton.setVisibility(View.VISIBLE);
        ratingButton.setVisibility(View.VISIBLE);
	    
    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RateTask rate = new RateTask(); // spinner and disappear buttons
            rate.execute(currentVideoId, "1");
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            showVideoInfo(1);
        }
    };

    View.OnClickListener dislikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RateTask rate = new RateTask();
            rate.execute(currentVideoId, "-1");
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            showVideoInfo(-1);
        }
    };

    View.OnClickListener flagListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ReportTask report = new ReportTask();
            report.execute(currentVideoId, User.getId(myContext));
            flagButton.setVisibility(View.GONE);
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
                if (response.getString("success").equals("1")) { // NPE
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
            JSONObject response = gae.reportVideo(params[0],params[1]);
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

    public void playNextVideo() {
        if (tap < uris.size()-1) {
            tap++;
            currentVideo.stopPlayback();
            flagButton.setVisibility(View.VISIBLE);
            currentVideo.setVideoURI(uris.get(tap));
            currentVideoId = uris.get(tap).getLastPathSegment().replace(".mp4", "");
            currentVideo.start();
            showCaption();
            if (User.admin) {
                likeButton.setVisibility(View.GONE);
                dislikeButton.setVisibility(View.GONE);
                showVideoInfo(0);
            } else {
                commentButton.setVisibility(View.GONE);
                ratingButton.setVisibility(View.GONE);
                likeButton.setVisibility(View.VISIBLE);
                dislikeButton.setVisibility(View.VISIBLE);
            }
        } else {
            finish();
        }
    }


    public void showCaption () {
        try {
            String captionContent = videoInfo.get(currentVideoId).getString("caption");
            caption.setText(captionContent);
        } catch (JSONException e) {
            e.printStackTrace();
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
            playNextVideo();
            return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) { // or replay auto on?
			currentVideo.start();
			return true;
		}
	}
}