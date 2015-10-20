package yonder.android;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
    int percentageWatched;
    String gaCategory;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
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

        if (LoadFeedActivity.myVideosOnly) {
            likeButton.setVisibility(View.GONE);
            dislikeButton.setVisibility(View.GONE);
            flagButton.setVisibility(View.GONE);
            showVideoInfo(0);
            gaCategory = "My Feed";
        } else if (User.admin) {
            showVideoInfo(0);
            likeButton.setY(-150);
            dislikeButton.setY(-150);
        } else {
            commentButton.setVisibility(View.GONE);
            ratingButton.setVisibility(View.GONE);
            gaCategory = "Nearby Feed";
        }
    }

	@Override
	protected void onResume() {
        Video.obfuscate(false);
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
        Video.obfuscate(true);
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

            int ratingInt = Integer.valueOf(rating) + myRating;
            String unit;
            if (ratingInt == 1 || ratingInt == -1) {
                unit = "Like";
            } else {
                unit = "Likes";
            }
            ratingButton.setText(ratingInt + " " + unit);
        } catch (Exception e) {
            Logger.log(e);;
        }
        commentButton.setVisibility(View.VISIBLE);
        ratingButton.setVisibility(View.VISIBLE);
    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!User.admin) {
                likeButton.setEnabled(false);
                dislikeButton.setEnabled(false);
            }
            rotation = AnimationUtils.loadAnimation(myContext, R.anim.rotate_fast);
            likeButton.startAnimation(rotation);
            RateTask rate = new RateTask(); // spinner and disappear buttons
            Logger.log(Log.INFO, TAG, "Liking video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Like Video");
            rating = "1";
            rate.execute(currentVideoId, rating, User.getId(myContext));
            timer = new Timer();
            timer.schedule(new StopAnimationTask(), 200);
        }
    };

    View.OnClickListener dislikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!User.admin) {
                likeButton.setEnabled(false);
                dislikeButton.setEnabled(false);
            }
            rotation = AnimationUtils.loadAnimation(myContext, R.anim.rotate_fast);
            dislikeButton.startAnimation(rotation);
            RateTask rate = new RateTask();
            Logger.log(Log.INFO, TAG, "Disliking video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Dislike Video");
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
            Logger.log(Log.INFO, TAG, "Flagging video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Flag Video");
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
                    if (!User.admin) {
                        likeButton.setVisibility(View.GONE);
                        dislikeButton.setVisibility(View.GONE);
                    }
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
            Logger.trackEvent(myContext, gaCategory, "Open Comments");
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
                        Logger.log(new Exception("Server Side Failure"));
                        //Toast.makeText(myContext, "Failed to rate the video!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    //Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);;
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
                        Toast.makeText(myContext, "Flagged", Toast.LENGTH_LONG).show();
                        flagButton.setVisibility(View.GONE);
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(myContext, "Failed to flag the video!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(myContext, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
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
            flagButton.setVisibility(View.VISIBLE);
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
            if (LoadFeedActivity.myVideosOnly) {
                likeButton.setVisibility(View.GONE);
                dislikeButton.setVisibility(View.GONE);
                flagButton.setVisibility(View.GONE);
                showVideoInfo(0);
            } else if (User.admin) {
                showVideoInfo(0);
                likeButton.setVisibility(View.VISIBLE);
                dislikeButton.setVisibility(View.VISIBLE);
                likeButton.setEnabled(true);
                dislikeButton.setEnabled(true);
            } else {
                commentButton.setVisibility(View.GONE);
                ratingButton.setVisibility(View.GONE);
                likeButton.setVisibility(View.VISIBLE);
                dislikeButton.setVisibility(View.VISIBLE);
                likeButton.setEnabled(true);
                dislikeButton.setEnabled(true);
            }
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