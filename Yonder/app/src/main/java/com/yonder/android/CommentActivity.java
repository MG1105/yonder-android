package com.yonder.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CommentActivity extends Activity {

	public static ArrayList<Comment> comments;
	private String videoId;
	Activity myActivity;
	private ProgressBar spinner;
	EditText commentText;
	CommentsAdapter adapter;
	String commentId;

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
	}

	View.OnClickListener sendListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			AddCommentTask addComment = new AddCommentTask();
			commentText = (EditText) findViewById(R.id.add_comment_text); // test with '
			commentId = Long.toString(System.currentTimeMillis());
			addComment.execute("12345677", videoId, commentId, commentText.getText().toString());
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
			content.setText(comment.getContent());
			rating.setText(comment.getRating() + " LIKES");

			flagButton.setOnClickListener(new View.OnClickListener() {
				String id = comment.getId();
				Button myFlag = flagButton;
				@Override
				public void onClick(View v) {
					ReportTask report = new ReportTask();
					report.execute(id);
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

	class GetCommentsTask extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				JSONObject response = gae.getComments(videoId);
				try {
					if (response.getString("success").equals("1")) {
						JSONArray commentsArray = response.getJSONArray("comments");
						comments = Comment.fromJson(commentsArray);
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

		protected void onPostExecute(Void params) {
			if (comments != null) {
				// TODO: check exception?
				Log.d("", comments.toString());
				// Create the adapter to convert the array to views
				adapter = new CommentsAdapter(myActivity);
				// Attach the adapter to a ListView
				ListView listView = (ListView) findViewById(R.id.listView_comments);
				listView.setAdapter(adapter);
				spinner = (ProgressBar)findViewById(R.id.progress_comments);
				spinner.setVisibility(View.GONE);
				if (comments.size() < 0) {


				}
			} else {

			}

		}
	}


	class AddCommentTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			JSONObject response = gae.addComment(params[0], params[1], params[2], params[3]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response.getString("success").equals("1")) {
					comments.add(new Comment(commentId, commentText.getText().toString()));
					adapter.notifyDataSetChanged();
					if (commentText != null) {
						commentText.setText("");
					}
					Toast toast = Toast.makeText(myActivity, "Sent!", Toast.LENGTH_LONG);
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
			JSONObject response = gae.reportComment(params[0]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response.getString("success").equals("1")) {
					Toast toast = Toast.makeText(myActivity, "Flagged!", Toast.LENGTH_LONG);
					toast.show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	class RateTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			JSONObject response = gae.rateComment(params[0], params[1]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response.getString("success").equals("1")) {
					Toast toast = Toast.makeText(myActivity, "Rated!", Toast.LENGTH_LONG); //Liked? or Disliked?
					toast.show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}






}
