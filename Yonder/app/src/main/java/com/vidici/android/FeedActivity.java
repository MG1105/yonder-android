package com.vidici.android;

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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FeedActivity extends Activity {
    // Crash? Cleanup video folders
    private final String TAG = "Log." + this.getClass().getSimpleName();
	private int tap = 0;
	private GestureDetectorCompat mDetector;
    VideoView currentVideo;
    String currentVideoId;
    ArrayList<Uri> uris;
    Button likeButton;
    Button dislikeButton;
    Button commentButton;
    Button ratingButton;
    TextView caption;
    int myRating = 0;
    int rating;
    int rated;
    private Activity myContext;
    LinkedHashMap<String, JSONObject> videoInfo;
    int percentageWatched;
    String gaCategory; // not set

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        myContext = this;
        if (getIntent().getExtras().containsKey("channelId")) {
            videoInfo = ChannelActivity.channelInfo.get(getIntent().getExtras().getString("channelId"));
        } else {
            videoInfo = NotificationActivity.notificationInfo.get(getIntent().getExtras().getString("notificationId"));
        }

	    uris = getContent();

        likeButton = (Button) findViewById(R.id.like_button);
        dislikeButton = (Button) findViewById(R.id.dislike_button);
        commentButton = (Button) findViewById(R.id.comment_button);
        ratingButton = (Button) findViewById(R.id.rating);
        caption = (TextView) findViewById(R.id.textView_caption);

        likeButton.setOnClickListener(likeListener);
        dislikeButton.setOnClickListener(dislikeListener);
	    commentButton.setOnClickListener(commentListener);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        currentVideo = (VideoView) findViewById(R.id.currentVideo);
	    currentVideo.setVideoURI(uris.get(0));
        percentageWatched = 0;
        currentVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                percentageWatched = 100;
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });
        currentVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Logger.log(Log.ERROR, TAG, "Failed to play video " + currentVideoId + " Type " + what + " Code " + extra);
                Logger.log(new Exception("Failed to play video"));
                playNextVideo();
                return true;
            }
        });
        currentVideoId = uris.get(0).getLastPathSegment().replace(".mp4", "");
        Logger.log(Log.INFO, TAG, "currentVideoId " + currentVideoId);

        showCommentsTotal();
        showLikesTotal();
    }

	@Override
	protected void onResume() {
        super.onResume(); // loop?
        Logger.log(Log.INFO, TAG, "Resuming Activity");
		currentVideo.start();
        showCaption();
		if (CommentActivity.comments != null && CommentActivity.updateTotal) {
			commentButton.setText(CommentActivity.comments.size() + " Comments");
            CommentActivity.updateTotal = false;
		}
        Logger.fbActivate(this, true);
	}

	@Override
	protected void onPause() {
        super.onPause();
        Logger.log(Log.INFO, TAG, "Pausing Activity");
        Logger.fbActivate(this, false);
	}

	public ArrayList<Uri> getContent() {
		uris = new ArrayList<>();
        String videosPath = Video.loadedDir.getAbsolutePath();

        for (Map.Entry<String, JSONObject> entry : videoInfo.entrySet()) {
            uris.add(Uri.parse(videosPath + "/" +entry.getKey()+".mp4"));
        }
        Logger.log(Log.INFO, TAG, "Feed videos " + uris.toString());
        return uris;
	}

    protected void showCommentsTotal(){
        try {
            String commentsTotal = videoInfo.get(currentVideoId).getString("comments_total");
            if (commentsTotal.equals("0")) {
                commentButton.setText("Add Comment");
            } else if (commentsTotal.equals("1")) {
                commentButton.setText(commentsTotal + " Comment");
            } else {
                commentButton.setText(commentsTotal + " Comments");
            }
        } catch (Exception e) {
            Logger.log(e);;
        }
    }

    protected void showLikesTotal() {
        try {
            myRating = 0;
            rated = videoInfo.get(currentVideoId).getInt("rated");
            rating = Integer.valueOf(videoInfo.get(currentVideoId).getString("rating"));
            if (rated == 1) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(false);
            } else if (rated == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
            }

            String unit;
            if (rating == 1 || rating == -1) {
                unit = "Like";
            } else {
                unit = "Likes";
            }
            ratingButton.setText(rating + " " + unit);
        } catch (Exception e) {
            Logger.log(e);;
        }
    }

    void updateLikesTotal() {
        try {
            int overrideOldRating = 0;
            if (rated == 1) {
                overrideOldRating = -1;
            } else if (rated == -1) {
                overrideOldRating = 1;
            }
            rated = myRating;
            if (myRating == 1) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(false);
            } else if (myRating == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
            }

            rating = rating + myRating + overrideOldRating;
            String unit;
            if (rating == 1 || rating == -1) {
                unit = "Like";
            } else {
                unit = "Likes";
            }
            ratingButton.setText(rating + " " + unit);
        } catch (Exception e) {
            Logger.log(e);;
        }

    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            RateTask rate = new RateTask(1); // spinner and disappear buttons
            Logger.log(Log.INFO, TAG, "Liking video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Like Video");
            rate.execute(currentVideoId, "1", User.getId(myContext));
        }
    };

    View.OnClickListener dislikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            RateTask rate = new RateTask(-1);
            Logger.log(Log.INFO, TAG, "Disliking video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Dislike Video");
            rate.execute(currentVideoId, "-1", User.getId(myContext));
        }
    };

	View.OnClickListener commentListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(myContext, CommentActivity.class);
			intent.putExtra("videoId", currentVideoId);
			startActivity(intent);
            Logger.trackEvent(myContext, gaCategory, "Open Comments");
		}
	};


	class RateTask extends AsyncTask<String, Void, JSONObject> {

        int rating;
        RateTask (int rating) {
            this.rating = rating;
        }

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            JSONObject response = gae.rateVideo(params[0], params[1], params[2]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        myRating = rating;
                        updateLikesTotal();
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
                Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void playNextVideo() {
        if (tap < uris.size()-1) {
            tap++;
            setPercentageWatched();
            if (percentageWatched != 0) {
                Logger.trackEvent(myContext, gaCategory, "View Video", percentageWatched);
                if (percentageWatched < 33) {
                    Logger.trackEvent(myContext, gaCategory, "Skip Video", percentageWatched);
                }
            }
            Logger.log(Log.INFO, TAG, "Watched " + percentageWatched + "%");
            currentVideo.stopPlayback();
            currentVideo.setVideoURI(uris.get(tap));
            currentVideoId = uris.get(tap).getLastPathSegment().replace(".mp4", "");
            Logger.log(Log.INFO, TAG, "Playing " + currentVideoId);
            currentVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    percentageWatched = 100;
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                }
            });
            currentVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Logger.log(Log.ERROR, TAG, "Failed to play video " + currentVideoId + " Type " + what + " Code " + extra);
                    Logger.log(new Exception("Failed to play video"));
                    playNextVideo();
                    return true;
                }
            });
            percentageWatched = 0;
            currentVideo.start();
            showCaption();
            showCommentsTotal();
            showLikesTotal();
            likeButton.setEnabled(true);
            dislikeButton.setEnabled(true);
        } else {
            setPercentageWatched();
            if (percentageWatched != 0) {
                Logger.trackEvent(myContext, gaCategory, "View Video", percentageWatched);
                if (percentageWatched < 33) {
                    Logger.trackEvent(myContext, gaCategory, "Skip Video", percentageWatched);
                }
            }
            Logger.log(Log.INFO, TAG, "Watched " + percentageWatched + "%");
            finish();
        }
    }


    public void showCaption () {
        try {
            String captionContent = videoInfo.get(currentVideoId).getString("caption");
            caption.setText(captionContent);
        } catch (JSONException e) {
            Logger.log(e);;
        }
    }

    public void setPercentageWatched () {
        if (percentageWatched == 100) {
            return;
        }
        double current = currentVideo.getCurrentPosition();
        double duration = currentVideo.getDuration();
        System.out.println("current "+ current +"duration "+ + duration);
        if (current > 0 && duration > 0) {
            percentageWatched = (int) (current/duration * 100);
        } else {
            percentageWatched = 0;
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