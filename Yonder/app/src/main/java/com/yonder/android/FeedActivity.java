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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.cert.CRLSelector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
    String rating;
    private Activity myContext;
    LinkedHashMap<String, JSONObject> videoInfo;
    Animation rotation;
    Timer timer;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
        Crashlytics.log(Log.INFO, TAG, "Creating Activity");
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
        currentVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        currentVideoId = uris.get(0).getLastPathSegment().replace(".mp4", "");
        Crashlytics.log(Log.INFO, TAG, "currentVideoId " + currentVideoId);

        if (User.admin) {
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            showVideoInfo(0);
        } else if (LoadFeedActivity.myVideosOnly) {
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            flagButton.setVisibility(View.GONE);
            showVideoInfo(0);
        } else {
            commentButton.setVisibility(View.GONE);
            ratingButton.setVisibility(View.GONE);
        }
    }

	@Override
	protected void onResume() {
        Video.obfuscate(false);
        super.onResume(); // loop?
        Crashlytics.log(Log.INFO, TAG, "Resuming Activity");
		currentVideo.start();
        showCaption();
		if (CommentActivity.comments != null && CommentActivity.updateTotal) {
			commentButton.setText(CommentActivity.comments.size() + " Comments");
            CommentActivity.updateTotal = false;
		}
	}

	@Override
	protected void onPause() {
        Video.obfuscate(true);
        super.onPause();
        Crashlytics.log(Log.INFO, TAG, "Pausing Activity");
	}

	public ArrayList<Uri> getContent() {
		uris = new ArrayList<>();
        String videosPath = Video.loadedDir.getAbsolutePath();

        for (Map.Entry<String, JSONObject> entry : videoInfo.entrySet()) {
            uris.add(Uri.parse(videosPath + "/" +entry.getKey()+".mp4"));
        }
        Crashlytics.log(Log.INFO, TAG, "Feed videos " + uris.toString());
        return uris;
	}

    protected void showVideoInfo(int myRating) {
        try {
            String commentsTotal = videoInfo.get(currentVideoId).getString("comments_total");
            String rating = videoInfo.get(currentVideoId).getString("rating");
            if (commentsTotal.equals("0")) {
                commentButton.setText("Add Comment");
            } else if (commentsTotal.equals("1")) {
                commentButton.setText(commentsTotal + " Comment");
            } else {
                commentButton.setText(commentsTotal + " Comments");
            }
            int latestRating = Integer.valueOf(rating) + myRating;
            if (latestRating == 1 || latestRating == -1) {
                ratingButton.setText(latestRating + " Like");
            } else {
                ratingButton.setText(latestRating + " Likes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);;
        }
        commentButton.setVisibility(View.VISIBLE);
        ratingButton.setVisibility(View.VISIBLE);
    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            rotation = AnimationUtils.loadAnimation(myContext, R.anim.rotate_fast);
            likeButton.startAnimation(rotation);
            RateTask rate = new RateTask(); // spinner and disappear buttons
            Crashlytics.log(Log.INFO, TAG, "Liking video " + currentVideoId);
            rating = "1";
            rate.execute(currentVideoId, rating, User.getId(myContext));
            timer = new Timer();
            timer.schedule(new StopAnimationTask(), 200);
        }
    };

    View.OnClickListener dislikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            rotation = AnimationUtils.loadAnimation(myContext, R.anim.rotate_fast);
            dislikeButton.startAnimation(rotation);
            RateTask rate = new RateTask();
            Crashlytics.log(Log.INFO, TAG, "Disliking video " + currentVideoId);
            rating = "-1";
            rate.execute(currentVideoId, rating, User.getId(myContext));
            timer = new Timer();
            timer.schedule(new StopAnimationTask(), 200);
        }
    };

    View.OnClickListener flagListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ReportTask report = new ReportTask();
            Crashlytics.log(Log.INFO, TAG, "Flagging video " + currentVideoId);
            report.execute(currentVideoId, User.getId(myContext));
        }
    };

    class StopAnimationTask extends TimerTask {
        public void run() {
            // When you need to modify a UI element, do so on the UI thread.
            myContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (rating.equals("1")) {
                        //Toast.makeText(myContext, "Liked!", Toast.LENGTH_LONG).show();
                        showVideoInfo(1);
                        likeButton.clearAnimation();
                    } else {
                        //Toast.makeText(myContext, "Disliked!", Toast.LENGTH_LONG).show();
                        showVideoInfo(-1);
                        dislikeButton.clearAnimation();
                    }
                    likeButton.setVisibility(View.GONE);
                    dislikeButton.setVisibility(View.GONE);
                }
            });
            timer.cancel(); //Terminate the timer thread
        }
    }

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
            JSONObject response = gae.rateVideo(params[0], params[1], params[2]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {

                    } else {
                        Crashlytics.logException(new Exception("Server Side Failure"));
                        //Toast.makeText(myContext, "Failed to rate the video!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    //Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
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
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        Toast.makeText(myContext, "Flagged!", Toast.LENGTH_LONG).show();
                        flagButton.setVisibility(View.GONE);
                    } else {
                        Crashlytics.logException(new Exception("Server Side Failure"));
                        Toast.makeText(myContext, "Failed to flag the video!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);;
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
            Crashlytics.log(Log.INFO, TAG, "Playing " + currentVideoId);
            currentVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
            currentVideo.start();
            showCaption();
            if (User.admin) {
                likeButton.setVisibility(View.GONE);
                dislikeButton.setVisibility(View.GONE);
                showVideoInfo(0);
            } else if (LoadFeedActivity.myVideosOnly) {
                likeButton.setVisibility(View.GONE);
                dislikeButton.setVisibility(View.GONE);
                flagButton.setVisibility(View.GONE);
                showVideoInfo(0);
            } else {
                commentButton.setVisibility(View.GONE);
                ratingButton.setVisibility(View.GONE);
                likeButton.setVisibility(View.VISIBLE);
                dislikeButton.setVisibility(View.VISIBLE);
                likeButton.setEnabled(true);
                dislikeButton.setEnabled(true);
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
            Crashlytics.logException(e);;
        }
    }
	// Handle Touch

	@Override
	public boolean onTouchEvent(MotionEvent event){
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) { // looping?
            playNextVideo();
            return true;
		}
	}
}