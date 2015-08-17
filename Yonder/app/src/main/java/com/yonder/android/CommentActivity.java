package com.yonder.android;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class CommentActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	public static ArrayList<Comment> comments;
	private String videoId;
	Activity myActivity;
	private ProgressBar spinner;
	EditText commentText;
	CommentsAdapter adapter;
	String commentId, nickname;
	static SQLiteDatabase yonderDb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comment);
		myActivity = this;
		videoId = getIntent().getExtras().getString("videoId");
		GetCommentsTask getComments = new GetCommentsTask();
		getComments.execute();
		Button sendButton = (Button) findViewById(R.id.add_comment_button);
		sendButton.setOnClickListener(sendListener);
		Alert.showCommentRule(this);
		YonderDbHelper mDbHelper = new YonderDbHelper(this);
		yonderDb = mDbHelper.getReadableDatabase();
	}

	@Override
	protected void onStop() {
		super.onStop();
		yonderDb.close();
	}


	View.OnClickListener sendListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			AddCommentTask addComment = new AddCommentTask();
			commentText = (EditText) findViewById(R.id.add_comment_text); // test with '

			int extra = commentText.getText().length()- 250;
			if (commentText.getText().length() == 0) {
				Toast toast = Toast.makeText(myActivity, "Please write a comment first!", Toast.LENGTH_LONG);
				toast.show();
			} else if (extra > 0) {
				Toast toast = Toast.makeText(myActivity, "Your comment has " + extra + " too many characters!", Toast.LENGTH_LONG);
				toast.show();
			} else {
				commentId = Long.toString(System.currentTimeMillis());
				nickname = User.getNickname(myActivity);
				String userId = User.getId(myActivity);
				Crashlytics.log(Log.INFO, TAG, String.format("Sending comment: nickname %s userId %s videoId %s commentId %s " +
								"commentText %s",
						nickname, userId, videoId, commentId, commentText.getText().toString()));
				addComment.execute(nickname, userId, videoId, commentId, commentText.getText().toString());
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
			rating = (TextView) convertView.findViewById(R.id.textView_comment_item_rating);
			flagButton = (Button) convertView.findViewById(R.id.button_flag);
			likeButton = (Button) convertView.findViewById(R.id.button_comment_item_like);
			dislikeButton = (Button) convertView.findViewById(R.id.button_comment_item_dislike);

			// Populate the data into the template view using the data object
			String sourceString = "<b>" + "@" + comment.getNickname() + "</b> " + "<br>" + comment.getContent();
			content.setText(Html.fromHtml(sourceString));
			rating.setText(comment.getRating() + " Likes"); // no s if 1

			if (comment.isFlagged()) {
				flagButton.setVisibility(View.GONE);
			}
			if (comment.isRated()) {
				rating.setVisibility(View.VISIBLE);
				likeButton.setVisibility(View.GONE);
				dislikeButton.setVisibility(View.GONE);
			}

			flagButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				Button myFlag = flagButton;
				@Override
				public void onClick(View v) {
					ReportTask report = new ReportTask();
					report.execute(id, User.getId(myActivity));
					myFlag.setVisibility(View.GONE);
				}
			});

			likeButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				int pos = position;
				TextView myRating = rating;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				@Override
				public void onClick(View v) {
					RateTask rateComment = new RateTask();
					rateComment.execute(id, "1");
					getItem(pos).updateRating(1);
					myRating.setVisibility(View.VISIBLE);
					myLike.setVisibility(View.GONE);
					myDislike.setVisibility(View.GONE);
					adapter.notifyDataSetChanged();
				}
			});

			dislikeButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				int pos = position;
				TextView myRating = rating;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				@Override
				public void onClick(View v) {
					RateTask rateComment = new RateTask();
					rateComment.execute(id, "-1");
					getItem(pos).updateRating(-1);
					myRating.setVisibility(View.VISIBLE);
					myLike.setVisibility(View.GONE);
					myDislike.setVisibility(View.GONE);
					adapter.notifyDataSetChanged();
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
				Crashlytics.log(Log.INFO, TAG, "Getting comments for " + videoId);
				JSONObject response = gae.getComments(videoId);
				return response;
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
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
							ListView listView = (ListView) findViewById(R.id.listView_comments);
							listView.setAdapter(adapter);
							spinner = (ProgressBar)findViewById(R.id.progress_comments);
							spinner.setVisibility(View.GONE);
							if (comments.size() == 0) {
								TextView noComments = (TextView)findViewById(R.id.textView_no_comments);
								noComments.setVisibility(View.VISIBLE);
							}
						}
					} else {
						Crashlytics.logException(new Exception("Server Side Failure"));
						Toast.makeText(myActivity, "Failed to retrieve comments!", Toast.LENGTH_LONG).show();
						finish();
					}
				} else {
					Toast.makeText(myActivity, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
					finish();
				}
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
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
						comments.add(new Comment(commentId, commentText.getText().toString(),nickname));
						adapter.notifyDataSetChanged();
						if (commentText != null) {
							commentText.setText("");
						}
						Toast.makeText(myActivity, "Sent!", Toast.LENGTH_LONG).show();
					} else {
						Crashlytics.logException(new Exception("Server Side Failure"));
						Toast.makeText(myActivity, "Failed to send comment!", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(myActivity, "Please check your connectivity and try again later!", Toast.LENGTH_LONG).show();
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
			Crashlytics.log(Log.INFO, TAG, "Reporting comment " + params[0]);
			JSONObject response = gae.reportComment(params[0], params[1]);
			Database db = new Database();
			db.flagComment(yonderDb, params[0], videoId);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						Toast toast = Toast.makeText(myActivity, "Flagged!", Toast.LENGTH_LONG);
						toast.show();
					} else {
						Crashlytics.logException(new Exception("Server Side Failure"));
					}
				} else {

				}
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
			}
		}
	}

	class RateTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Crashlytics.log(Log.INFO, TAG, "Rating comment " + params[0]+ " " + params[1]);
			JSONObject response = gae.rateComment(params[0], params[1]);
			Database db = new Database();
			db.rateComment(yonderDb, params[0], videoId);
			db.cleanup(yonderDb); // too often?
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						Toast toast = Toast.makeText(myActivity, "Rated!", Toast.LENGTH_LONG); //Liked? or Disliked?
						toast.show();
					} else {
						Crashlytics.logException(new Exception("Server Side Failure"));
					}
				} else {

				}
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
			}
		}
	}






}
