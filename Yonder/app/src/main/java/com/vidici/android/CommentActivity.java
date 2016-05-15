package com.vidici.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class CommentActivity extends Activity {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	public static ArrayList<Comment> comments;
	private String videoId;
	Activity myActivity;
	private ProgressBar spinner;
	EditText commentText;
	String comment;
	CommentsAdapter adapter;
	String nickname;
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
			commentText = (EditText) findViewById(R.id.add_comment_text);
			comment = commentText.getText().toString().replace("\n", " ").trim();
			if (comment.length() == 0) {
				Toast.makeText(myActivity, "Please write a comment first", Toast.LENGTH_LONG).show();
				sendButton.setEnabled(true);
				return;
			}
			SharedPreferences sharedPreferences = myActivity.getSharedPreferences(
					"com.vidici.android", Context.MODE_PRIVATE);
			nickname = sharedPreferences.getString("nickname", null);
			if (nickname == null || User.admin) {
				final AlertDialog alertDialog = new AlertDialog.Builder(myActivity)
						.setPositiveButton(android.R.string.ok, null)
						.create();
				alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				alertDialog.setMessage("Choose your nickname");
				Logger.trackEvent(myActivity, "Settings", "Choose Nickname");
				Logger.log(Log.INFO, TAG, "Choosing nickname");

				final EditText input = new EditText(myActivity);
				input.setHint("nickname");
				alertDialog.setView(input);

				alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								String name = input.getText().toString().trim();
								if (isValidNickname(name)) {
									alertDialog.dismiss();
									SharedPreferences sharedPreferences = myActivity.getSharedPreferences(
											"com.vidici.android", Context.MODE_PRIVATE);
									name = name.toLowerCase();
									sharedPreferences.edit().putString("nickname", name).apply();
									Logger.trackEvent(myActivity, "Settings", "Add Nickname");
									nickname = name;
									sendComment();
								}
							}
						});
					}
				});
				alertDialog.show();
				sendButton.setEnabled(true);
			}	else {
				sendComment();
			}
		}

		void sendComment() {
			sendButton.setEnabled(false);
			String userId = User.getId(myActivity);
			Logger.log(Log.INFO, TAG, String.format("Sending comment: nickname %s userId %s videoId %s " +
							"commentText %s",
					nickname, userId, videoId, comment));
			Logger.trackEvent(myActivity, "Comment", "Add Comment");
			AddCommentTask addComment = new AddCommentTask();
			addComment.execute(nickname, userId, videoId, comment);
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
			nickname.setText(comment.getNickname());
			content.setText(comment.getContent());
			 // no s if 1
			int ratingInt = Integer.valueOf(comment.getRating());
			if (ratingInt >= 0) {
				rating.setText(comment.getRating());
			} else {
				rating.setText(comment.getRating());
			}


			if (comment.getRated() == 1) {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(false);
				likeButton.setBackgroundResource(R.drawable.ic_up_dark);
				dislikeButton.setBackgroundResource(R.drawable.ic_down);
			} else if (comment.getRated() == -1) {
				dislikeButton.setEnabled(false);
				likeButton.setEnabled(true);
				likeButton.setBackgroundResource(R.drawable.ic_up);
				dislikeButton.setBackgroundResource(R.drawable.ic_down_dark);
			} else {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(true);
				likeButton.setBackgroundResource(R.drawable.ic_up);
				dislikeButton.setBackgroundResource(R.drawable.ic_down);
			}

			if (User.admin) {
				dislikeButton.setEnabled(true);
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
								ImageView noCommentsImage = (ImageView)findViewById(R.id.image_no_comments);
								noComments.setVisibility(View.VISIBLE);
								noCommentsImage.setVisibility(View.VISIBLE);
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
			JSONObject response = gae.addComment(params[0], params[1], params[2], params[3]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						if (comments.size() == 0) {
							TextView noComments = (TextView)findViewById(R.id.textView_no_comments);
							ImageView noCommentsImage = (ImageView)findViewById(R.id.image_no_comments);
							noComments.setVisibility(View.GONE);
							noCommentsImage.setVisibility(View.GONE);
						}
						comments.add(new Comment(response.getString("comment_id"), comment, nickname));
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


	protected Boolean isValidNickname(String name) {
		String pattern= "^[a-zA-Z0-9_]*$";
		if (name.contains(" ")) {
			Toast.makeText(myActivity, "Cannot contain spaces", Toast.LENGTH_LONG).show();
			return false;
		} else if (!name.matches(pattern)) {
			Toast.makeText(myActivity, "Cannot contain special characters", Toast.LENGTH_LONG).show();
			return false;
		} else if (name.length() > 16) {
			Toast.makeText(myActivity, "Cannot be longer than 16 characters", Toast.LENGTH_LONG).show();
			return false;
		} else {
			return true;
		}
	}



}
