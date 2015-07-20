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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CommentActivity extends ActionBarActivity {

	private ArrayList<Comment> comments;
	private String videoId;
	Activity myActivity;
	private ProgressBar spinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comment);
		myActivity = this;
		videoId = getIntent().getExtras().getString("videoId");
		getCommentsTask getComments = new getCommentsTask();
		getComments.execute();
	}


	class getCommentsTask extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				String videoId = "123";
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
				if (comments.size() > 0) {
					Log.d("", comments.toString());
					// Create the adapter to convert the array to views
					CommentsAdapter adapter = new CommentsAdapter(myActivity);
					// Attach the adapter to a ListView
					ListView listView = (ListView) findViewById(R.id.listView_comments);
					listView.setAdapter(adapter);
					spinner = (ProgressBar)findViewById(R.id.progress_comments);
					spinner.setVisibility(View.GONE);
				} else {

				}
			} else {

			}

		}
	}

	class CommentsAdapter extends ArrayAdapter<Comment> {

		public CommentsAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, comments);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Comment comment = getItem(position);

			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_comment, parent, false);
			}

			// Lookup view for data population
			TextView content = (TextView) convertView.findViewById(R.id.textView_comment);
			Button flagButton = (Button) convertView.findViewById(R.id.button_flag);

			// Populate the data into the template view using the data object
			content.setText(comment.getContent());

			// Return the completed view to render on screen
			return convertView;

		}

	}








	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_comment, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
