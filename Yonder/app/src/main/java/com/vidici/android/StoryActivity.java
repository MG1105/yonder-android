package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

public class StoryActivity extends Activity {
    // Crash? Cleanup video folders
    private final String TAG = "Log." + this.getClass().getSimpleName();
	private int tap = 0;
	private GestureDetectorCompat mDetector;
    VideoView currentVideo;
    String currentVideoId, profileId;
    ArrayList<Uri> uris;
    Button likeButton;
    Button dislikeButton;
    Button commentButton;
    TextView ratingText;
    TextView caption, channel, username;
    int myRating = 0;
    int rating;
    int rated, gold;
    private Activity myContext;
    LinkedHashMap<String, JSONObject> videoInfo;
    int percentageWatched;
    String gaCategory;
    ProgressBar spinner;
    Boolean buffering = false;
    TextView textGold;
    ImageView backgroundGold;
    FrameLayout profile;
//    int position = 0;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_story);
        Logger.log(Log.INFO, TAG, "Creating Activity");
        myContext = this;
        if (getIntent().getExtras().containsKey("channelId")) {
            videoInfo = ChannelFragment.channelInfo.get(getIntent().getExtras().getString("channelId"));
            gaCategory = "Channel Story";
        } else if (getIntent().getExtras().containsKey("notificationId")) {
            videoInfo = NotificationActivity.notificationInfo.get(getIntent().getExtras().getString("notificationId"));
            gaCategory = "Notification Story";
        } else if (getIntent().getExtras().containsKey("feedItemId")) {
            videoInfo = FeedFragment.feedInfo.get(getIntent().getExtras().getString("feedItemId"));
            gaCategory = "Feed Story";
        }

        if (videoInfo == null) {
            finish();
            return;
        }

	    uris = getContent();

        likeButton = (Button) findViewById(R.id.like_button);
        dislikeButton = (Button) findViewById(R.id.dislike_button);
        commentButton = (Button) findViewById(R.id.comment_button);
        ratingText = (TextView) findViewById(R.id.rating);
        caption = (TextView) findViewById(R.id.textView_caption);
        username = (TextView) findViewById(R.id.textview_story_username);
        channel = (TextView) findViewById(R.id.textView_channel);
        spinner = (ProgressBar)findViewById(R.id.progress_videoview);
        textGold = (TextView) myContext.findViewById(R.id.textview_story_gold);
        backgroundGold = (ImageView) myContext.findViewById(R.id.imageView_story_gold_background);
        profile = (FrameLayout) myContext.findViewById(R.id.frame_story_profile);

        likeButton.setOnClickListener(likeListener);
        dislikeButton.setOnClickListener(dislikeListener);
	    commentButton.setOnClickListener(commentListener);
	    profile.setOnClickListener(profileListener);

        backgroundGold.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!User.loggedIn(myContext)) {
                    return;
                }
                if (profileId.equals(User.getId(myContext))) {
                    Toast.makeText(myContext, "You cannot give yourself a Vidici Award", Toast.LENGTH_LONG).show();
                    return;
                }
                Logger.trackEvent(myContext, gaCategory, "Give Gold");
                GiveGoldTask giveGoldTask = new GiveGoldTask();
                giveGoldTask.execute(User.getId(myContext), profileId, currentVideoId);
            }
        });

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

        if (uris.size() > 1) {
            DownloadVideos downloadVideos = new DownloadVideos();
            downloadVideos.execute("http://storage.googleapis.com/yander/" + uris.get(1).getLastPathSegment());
        }
        Alert.showStoryIntro(myContext);
    }

	@Override
	protected void onResume() {
        super.onResume(); // loop?
        Logger.log(Log.INFO, TAG, "Resuming Activity");
		currentVideo.start();
        showCaption();
		if (CommentActivity.comments != null && CommentActivity.updateTotal) {
            if (CommentActivity.comments.size() == 1) {
                commentButton.setText(CommentActivity.comments.size() + " Comment");
            } else {
                commentButton.setText(CommentActivity.comments.size() + " Comments");
            }
            try {
                videoInfo.get(currentVideoId).put("comments_total", CommentActivity.comments.size());
            } catch (JSONException e) {
                Logger.log(e);
            }
            CommentActivity.updateTotal = false;
		}
        Logger.fbActivate(this, true);
	}

	@Override
	protected void onPause() {
        super.onPause();
        Logger.log(Log.INFO, TAG, "Pausing Activity");
        currentVideo.stopPlayback();
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
//            commentsTotal = "" + (int ) (Math.random() * 100);
//            commentButton.setText(commentsTotal + " Comments");
        } catch (Exception e) {
            Logger.log(e);;
        }
    }

    protected void showLikesTotal() {
        try {
            myRating = 0;
            rated = videoInfo.get(currentVideoId).getInt("rated");
            rating = Integer.valueOf(videoInfo.get(currentVideoId).getString("rating"));
            gold = videoInfo.get(currentVideoId).getInt("gold");

            if (rated == 1) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(false);
                likeButton.setBackgroundResource(R.drawable.ic_up_dark);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_white);
            } else if (rated == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
                likeButton.setBackgroundResource(R.drawable.ic_up_white);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_dark);
            } else {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(true);
                likeButton.setBackgroundResource(R.drawable.ic_up_white);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_white);
            }

            if (!User.admin) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(true);
            }

            String unit;
            if (rating == 1 || rating == -1) {
                unit = "Like";
            } else {
                unit = "Likes";
            }
            ratingText.setText(""+rating);

            if (gold > 0) {
                backgroundGold.setBackgroundResource(R.drawable.oval_gold);
                textGold.setText("x " + gold);
            } else {
                backgroundGold.setBackgroundResource(R.drawable.oval_white);
                textGold.setText("");
            }

        } catch (Exception e) {
            Logger.log(e);
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
                likeButton.setBackgroundResource(R.drawable.ic_up_dark);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_white);
            } else if (myRating == -1) {
                dislikeButton.setEnabled(false);
                likeButton.setEnabled(true);
                likeButton.setBackgroundResource(R.drawable.ic_up_white);
                dislikeButton.setBackgroundResource(R.drawable.ic_down_dark);
            }

            if (User.admin) {
                dislikeButton.setEnabled(true);
                likeButton.setEnabled(true);
            }

            rating = rating + myRating + overrideOldRating;
            String unit;
            if (rating == 1 || rating == -1) {
                unit = "Like";
            } else {
                unit = "Likes";
            }
            ratingText.setText("" + rating);

            videoInfo.get(currentVideoId).put("rated", myRating);
            videoInfo.get(currentVideoId).put("rating", Integer.toString(rating));
        } catch (Exception e) {
            Logger.log(e);;
        }

    }

    View.OnClickListener likeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!User.loggedIn(myContext)) {
                return;
            }
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
            if (!User.loggedIn(myContext)) {
                return;
            }
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            RateTask rate = new RateTask(-1);
            Logger.log(Log.INFO, TAG, "Disliking video " + currentVideoId);
            Logger.trackEvent(myContext, gaCategory, "Dislike Video");
            rate.execute(currentVideoId, "-1", User.getId(myContext));
        }
    };

	View.OnClickListener profileListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
//            Toast.makeText(myContext, "Followed", Toast.LENGTH_LONG).show();
//            ImageView imageFollow = (ImageView) myContext.findViewById(R.id.background_story_profile1);
//            ImageView imageCircleFollow = (ImageView) myContext.findViewById(R.id.background_story_profile);
//            imageFollow.setBackgroundResource(R.drawable.ic_followed);
//            imageCircleFollow.setBackgroundResource(R.drawable.oval_green);
            Intent intent = new Intent(myContext, ProfileActivity.class);
            intent.putExtra("profileId", profileId);
            startActivity(intent);
            Logger.trackEvent(myContext, gaCategory, "Open Profile");
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
            if (buffering) {
                return;
            }

            if (!new File(uris.get(tap+1).getPath()).exists()) {
                currentVideo.stopPlayback();
                Logger.log(Log.INFO, TAG, "Buffering");
                buffering = true;
                spinner.setVisibility(View.VISIBLE);
                BufferingTask bufferingTask = new BufferingTask();
                bufferingTask.execute();
                return;
            }

            if (uris.size() > tap+2) {
                DownloadVideos downloadVideos = new DownloadVideos();
                downloadVideos.execute("http://storage.googleapis.com/yander/" + uris.get(tap+2).getLastPathSegment());
            }

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


    public void showCaption() {
        try {
            String captionContent = videoInfo.get(currentVideoId).getString("caption");
            caption.setText(captionContent);
            String channelName = videoInfo.get(currentVideoId).getString("channel_name");
            channel.setText("#"+channelName);
            profileId = videoInfo.get(currentVideoId).getString("user_id");

            String name = videoInfo.get(currentVideoId).getString("username");
            if (name.equals("null")) {
                profile.setVisibility(View.GONE);
                username.setText("");
            } else {
                profile.setVisibility(View.VISIBLE);
                username.setText("@"+name);
            }

        } catch (JSONException e) {
            Logger.log(e);
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

    class BufferingTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            try {
                int retry = 0;
                while (!new File(uris.get(tap+1).getPath()).exists() && retry < 15) {
                    retry++;
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                Logger.log(e);
            }
            return null;
        }

        protected void onPostExecute(Void response) {
            try {
                if (new File(uris.get(tap+1).getPath()).exists()) {
                    buffering = false;
                    playNextVideo();
                    spinner.setVisibility(View.GONE);
                } else {
                    finish();
                    Logger.log(new Exception("Failed to buffer video"));
                }
            } catch (Exception e) {
                Logger.log(e);
                finish();
            }
        }
    }

    class DownloadVideos extends DownloadTask {
        DownloadVideos () {
            super(myContext);
        }

        @Override
        protected void onPostExecute(Integer error) {
            super.onPostExecute(error);
        }
    }

    class GiveGoldTask extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            AppEngine gae = new AppEngine();
            Logger.log(Log.INFO, TAG, "Giving gold to " + params[1]);
            JSONObject response = gae.giveGold(params[0], params[1], params[2]);
            return response;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null) {
                    if (response.getString("success").equals("1")) {
                        if (response.getString("gold").equals("1")) {
                            backgroundGold.setBackgroundResource(R.drawable.oval_gold);
                            gold++;
                            videoInfo.get(currentVideoId).put("gold", gold);
                            textGold.setText("x " + gold);
                            if (username.getText().length() == 0) {
                                Toast.makeText(myContext, "Vidici Award sent!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(myContext, "@"+videoInfo.get(currentVideoId).getString("username") + " received a Vidici Award from you!", Toast.LENGTH_LONG).show();
                            }
//
                        } else {
                            Toast.makeText(myContext, "You ran out of tokens", Toast.LENGTH_LONG).show();
                            Logger.trackEvent(myContext, gaCategory, "Out of Gold");
                        }
                    } else {
                        Logger.log(new Exception("Server Side Failure"));
                        Toast.makeText(myContext, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(myContext, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.log(e);
                Toast.makeText(myContext, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
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
		@Override
		public boolean onDown(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) { // looping?
//            ImageView imageFollow = (ImageView) myContext.findViewById(R.id.background_story_profile1);
//            ImageView imageCircleFollow = (ImageView) myContext.findViewById(R.id.background_story_profile);
//            imageCircleFollow.setBackgroundResource(R.drawable.oval_blue);
//            imageFollow.setBackgroundResource(R.drawable.ic_follow);
//            position++;
            playNextVideo();
            return true;
		}
	}
}