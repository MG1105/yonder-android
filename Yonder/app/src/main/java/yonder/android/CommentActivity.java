package yonder.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class CommentActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	public static ArrayList<Comment> comments;
	private String videoId;
	Activity myActivity;
	private ProgressBar spinner;
	EditText commentText;
	String comment;
	CommentsAdapter adapter;
	String commentId, nickname;
	static SQLiteDatabase yonderDb;
	static boolean updateTotal;
	ListView commentList;
	Button sendButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comment);
		Logger.log(Log.INFO, TAG, "Creating Activity");
		myActivity = this;
		videoId = getIntent().getExtras().getString("videoId");
		GetCommentsTask getComments = new GetCommentsTask();
		getComments.execute();
		sendButton = (Button) findViewById(R.id.add_comment_button);
		sendButton.setOnClickListener(sendListener);
		Alert.showCommentRule(this);
		YonderDbHelper mDbHelper = new YonderDbHelper(this);
		if (yonderDb == null) {
			Logger.log(Log.INFO, TAG, "Creating YonderDb");
			yonderDb = mDbHelper.getWritableDatabase();
		}
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
		updateTotal = true;
		Logger.fbActivate(this, false);
	}

	View.OnClickListener sendListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			sendButton.setEnabled(false);
			AddCommentTask addComment = new AddCommentTask();
			commentText = (EditText) findViewById(R.id.add_comment_text);
			comment = commentText.getText().toString().replace("\n", " ");
			if (comment.replace(" ", "").length() == 0) {
				Toast.makeText(myActivity, "Please write a comment first", Toast.LENGTH_LONG).show();
				sendButton.setEnabled(true);
			} else {
				commentId = Long.toString(System.currentTimeMillis());
				nickname = User.getNickname(myActivity);
				String userId = User.getId(myActivity);
				if (User.admin) {
					if (comment.startsWith("@")) {
						nickname = comment.substring(1,5);
						comment = comment.substring(6);
					}
				}
				Logger.log(Log.INFO, TAG, String.format("Sending comment: nickname %s userId %s videoId %s commentId %s " +
								"commentText %s",
						nickname, userId, videoId, commentId, comment));
				Logger.trackEvent(myActivity, "Comment", "Add Comment");
				addComment.execute(nickname, userId, videoId, commentId, comment);
			}
		}
	};


	class CommentsAdapter extends ArrayAdapter<Comment> {
		Comment comment;
		TextView rating;
		Button likeButton, dislikeButton, flagButton;

		public CommentsAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, comments);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			comment = getItem(position);
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_comment, parent, false);
			}

			// Lookup view for data population
			TextView content = (TextView) convertView.findViewById(R.id.textView_comment);
			TextView nickname = (TextView) convertView.findViewById(R.id.textView_nickname);
			rating = (TextView) convertView.findViewById(R.id.textView_comment_item_rating);
			flagButton = (Button) convertView.findViewById(R.id.button_flag);
			likeButton = (Button) convertView.findViewById(R.id.button_comment_item_like);
			dislikeButton = (Button) convertView.findViewById(R.id.button_comment_item_dislike);

			// Populate the data into the template view using the data object
			nickname.setText("@" + comment.getNickname());
			content.setText(comment.getContent());
			 // no s if 1
			int ratingInt = Integer.valueOf(comment.getRating());
			if (ratingInt >= 0) {
				rating.setTextColor(Color.parseColor("#00FF00"));
				rating.setText("+" + comment.getRating());
			} else {
				rating.setTextColor(Color.parseColor("#ff0000"));
				rating.setText("\u2212" + comment.getRating().replace("-", ""));
			}

			if (comment.isFlagged()) {
				flagButton.setVisibility(View.GONE);
			} else {
				flagButton.setVisibility(View.VISIBLE);
			}
			if (comment.isRated()) {
				rating.setVisibility(View.VISIBLE);
				likeButton.setVisibility(View.GONE);
				dislikeButton.setVisibility(View.GONE);
			} else {
				rating.setVisibility(View.GONE);
				likeButton.setVisibility(View.VISIBLE);
				dislikeButton.setVisibility(View.VISIBLE);
			}

			flagButton.setEnabled(true);
			likeButton.setEnabled(true);
			dislikeButton.setEnabled(true);

			flagButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				Button myFlag = flagButton;
				Comment myComment = comment;
				@Override
				public void onClick(View v) {
					myFlag.setVisibility(View.GONE);
					ReportTask report = new ReportTask();
					report.execute(id, User.getId(myActivity));
					myComment.setFlagged();
					Logger.trackEvent(myActivity, "Comment", "Flag Comment");
				}
			});

			likeButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				int pos = position;
				TextView myRating = rating;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				Animation rotation = AnimationUtils.loadAnimation(myActivity, R.anim.rotate_fast);
				Timer timer;
				Comment myComment = comment;
				@Override
				public void onClick(View v) {
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateTask rateComment = new RateTask();
					rateComment.execute(id, "1", User.getId(myActivity));
					getItem(pos).updateRating(1);
					myLike.startAnimation(rotation);
					timer = new Timer();
					timer.schedule(new StopAnimationTask(), 200);
					myComment.setRated();
					Logger.trackEvent(myActivity, "Comment", "Like Comment");
				}

				class StopAnimationTask extends TimerTask {
					public void run() {
						// When you need to modify a UI element, do so on the UI thread.
						myActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								myLike.clearAnimation();
								myRating.setVisibility(View.VISIBLE);
								myLike.setVisibility(View.GONE);
								myDislike.setVisibility(View.GONE);
								adapter.notifyDataSetChanged();
							}
						});
						timer.cancel(); //Terminate the timer thread
					}
				}
			});

			dislikeButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				int pos = position;
				TextView myRating = rating;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				Animation rotation = AnimationUtils.loadAnimation(myActivity, R.anim.rotate_fast);
				Timer timer;
				Comment myComment = comment;
				@Override
				public void onClick(View v) {
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateTask rateComment = new RateTask();
					rateComment.execute(id, "-1", User.getId(myActivity));
					getItem(pos).updateRating(-1);
					myDislike.startAnimation(rotation);
					timer = new Timer();
					timer.schedule(new StopAnimationTask(), 200);
					myComment.setRated();
					Logger.trackEvent(myActivity, "Comment", "Dislike Comment");
				}

				class StopAnimationTask extends TimerTask {
					public void run() {
						// When you need to modify a UI element, do so on the UI thread.
						myActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								myDislike.clearAnimation();
								myRating.setVisibility(View.VISIBLE);
								myLike.setVisibility(View.GONE);
								myDislike.setVisibility(View.GONE);
								adapter.notifyDataSetChanged();
							}
						});
						timer.cancel(); //Terminate the timer thread
					}
				}
			});

			// Return the completed view to render on screen
			return convertView;

		}

	}

	class GetCommentsTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				Logger.log(Log.INFO, TAG, "Getting comments for " + videoId);
				JSONObject response = gae.getComments(videoId, User.getId(myActivity));
				return response;
			} catch (Exception e) {
				Logger.log(e);
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						JSONArray commentsArray = response.getJSONArray("comments");
						comments = Comment.fromJson(commentsArray, videoId);
						if (comments != null) {
							// Create the adapter to convert the array to views
							adapter = new CommentsAdapter(myActivity);
							// Attach the adapter to a ListView
							commentList= (ListView) findViewById(R.id.listView_comments);
							commentList.setAdapter(adapter);
							spinner = (ProgressBar)findViewById(R.id.progress_comments);
							spinner.setVisibility(View.GONE);
							if (comments.size() == 0) {
								TextView noComments = (TextView)findViewById(R.id.textView_no_comments);
								noComments.setVisibility(View.VISIBLE);
							}
						}
					} else {
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(myActivity, "Failed to retrieve comments!", Toast.LENGTH_LONG).show();
						finish();
					}
				} else {
					Toast.makeText(myActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					finish();
				}
			} catch (Exception e) {
				Logger.log(e);;
			}
		}
	}


	class AddCommentTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			JSONObject response = gae.addComment(params[0], params[1], params[2], params[3], params[4]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						if (comments.size() == 0) {
							TextView noComments = (TextView)findViewById(R.id.textView_no_comments);
							noComments.setVisibility(View.GONE);
						}
						comments.add(new Comment(commentId, comment, nickname));
						adapter.notifyDataSetChanged();
						if (commentText != null) {
							commentText.setText("");
						}
						commentList.smoothScrollToPosition(adapter.getCount()-1);
						InputMethodManager inputManager = (InputMethodManager)
								getSystemService(Context.INPUT_METHOD_SERVICE);
						inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
								getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						//Toast.makeText(myActivity, "Sent!", Toast.LENGTH_LONG).show();
					} else {
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(myActivity, "Failed to send comment", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(myActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
				sendButton.setEnabled(true);
			} catch (Exception e) {
				Logger.log(e);;
			}
		}
	}

	class ReportTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, "Reporting comment " + params[0]);
			JSONObject response = gae.reportComment(params[0], params[1]);
			SQL sql = new SQL();
			sql.flagComment(yonderDb, params[0], videoId);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						Toast toast = Toast.makeText(myActivity, "Flagged", Toast.LENGTH_LONG);
						toast.show();
					} else {
						Logger.log(new Exception("Server Side Failure"));
					}
				} else {

				}
			} catch (Exception e) {
				Logger.log(e);;
			}
		}
	}

	class RateTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, "Rating comment " + params[0]+ " " + params[1]);
			JSONObject response = gae.rateComment(params[0], params[1], params[2]);
			SQL sql = new SQL();
			sql.rateComment(yonderDb, params[0], videoId);
			SharedPreferences sharedPreferences = myActivity.getSharedPreferences(
					"yonder.android", Context.MODE_PRIVATE);
			long lastDbCleanup = sharedPreferences.getLong("last_db_cleanup", 0);
			long now = System.currentTimeMillis();
			if (lastDbCleanup == 0 || (now - lastDbCleanup) / 3600000 > 24) {
				Logger.log(Log.INFO, TAG, "Cleaning up android db");
				sql.cleanup(yonderDb);
				sharedPreferences.edit().putLong("last_db_cleanup", now).apply();
			}
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						//Toast toast = Toast.makeText(myActivity, "Rated!", Toast.LENGTH_LONG); //Liked? or Disliked?
						//toast.show();
					} else {
						Logger.log(new Exception("Server Side Failure"));
					}
				} else {

				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}






}
