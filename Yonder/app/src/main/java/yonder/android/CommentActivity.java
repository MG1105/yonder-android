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
		Button likeButton, dislikeButton;

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


			if (comment.getRated() == 1) {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(false);
			} else if (comment.getRated() == -1) {
				dislikeButton.setEnabled(false);
				likeButton.setEnabled(true);
			}

			likeButton.setOnClickListener(new View.OnClickListener() {
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				Comment myComment = comment;
				@Override
				public void onClick(View v) {
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateTask rateComment = new RateTask(myComment, myLike, myDislike);
					rateComment.execute("1", User.getId(myActivity));
					myComment.setRating(1);
					Logger.trackEvent(myActivity, "Comment", "Like Comment");
				}
			});

			dislikeButton.setOnClickListener(new View.OnClickListener() {
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				Comment myComment = comment;
				@Override
				public void onClick(View v) {
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateTask rateComment = new RateTask(myComment, myLike, myDislike);
					rateComment.execute("-1", User.getId(myActivity));
					myComment.setRating(-1);
					Logger.trackEvent(myActivity, "Comment", "Dislike Comment");
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


	class RateTask extends AsyncTask<String, Void, JSONObject> {
		Comment myComment;
		Button myLike;
		Button myDislike;

		protected RateTask(Comment comment, Button like, Button dislike) {
			myComment = comment;
			myLike = like;
			myDislike = dislike;
		}

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, "Rating comment " + params[0]+ " " + params[1]);
			JSONObject response = gae.rateComment(myComment.getId(), params[0], params[1]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						myComment.updateRating();
						// myLike chane to bold
						adapter.notifyDataSetChanged();
					} else {
						myDislike.setEnabled(true);
						myLike.setEnabled(true);
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(myActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					}
				} else {
					myDislike.setEnabled(true);
					myLike.setEnabled(true);
					Toast.makeText(myActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Logger.log(e);
				Toast.makeText(myActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				myDislike.setEnabled(true);
				myLike.setEnabled(true);
			}
		}
	}






}
