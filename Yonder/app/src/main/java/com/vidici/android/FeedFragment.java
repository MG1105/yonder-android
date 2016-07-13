package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class FeedFragment extends Fragment {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	FeedAdapter feedAdapter;
	Activity mActivity;
	ArrayList<FeedItem> feed;
	static HashMap<String, LinkedHashMap<String, JSONObject>> feedInfo = new HashMap<>();
	View view;
	private ProgressBar progressBar;
	Resources resources;
	boolean homeFeed;
	TextView noFeed, noVideos;
	ImageView noFeedImage, noVideosImage;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_feed, container, false);
		mActivity = getActivity();
		resources = getResources();
		homeFeed = getArguments().getBoolean("home");
		noFeed = (TextView)view.findViewById(R.id.textView_no_feed);
		noFeedImage = (ImageView)view.findViewById(R.id.image_no_feed);
		noVideos = (TextView)view.findViewById(R.id.textView_no_recent_videos);
		noVideosImage = (ImageView)view.findViewById(R.id.image_no_recent_videos);
		progressBar = (ProgressBar)view.findViewById(R.id.progress_feed);
		return view;
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		GetFeedTask getFeedTask = new GetFeedTask();
		if (homeFeed) {
			if (User.isLoggedIn(mActivity)) {
				getFeedTask.execute(User.getId(mActivity), "home");
				noFeed.setText("Follow more people to improve\nyour Home Feed");
				noFeed.setVisibility(View.INVISIBLE);
				noFeedImage.setVisibility(View.INVISIBLE);
				progressBar.setVisibility(View.VISIBLE);
			}
		} else {
			String profileId = getArguments().getString("user_id");
			getFeedTask.execute(profileId, "recent");
			noFeed.setVisibility(View.INVISIBLE);
			noFeedImage.setVisibility(View.INVISIBLE);
			noFeed = noVideos;
			noFeedImage = noVideosImage;
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	class GetFeedTask extends AsyncTask<String, Void, JSONObject> {

		ListView listView;

		GetFeedTask () {
			listView = (ListView) view.findViewById(R.id.listView_feed);
		}

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				JSONObject response = gae.getFeed(params[0], params[1]);
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

						JSONArray FeedArray = response.getJSONArray("feed");
						feed = FeedItem.fromJson(FeedArray);
						if (feed.size() > 0) {
							// Create the adapter to convert the array to views
							feedAdapter = new FeedAdapter(mActivity);
							// Attach the adapter to a ListView
							listView.setAdapter(feedAdapter);
							noFeed.setVisibility(View.GONE);
							noFeedImage.setVisibility(View.GONE);
						} else {
							noFeed.setVisibility(View.VISIBLE);
							noFeedImage.setVisibility(View.VISIBLE);
						}
					} else {
						Logger.log(new Exception("Server Side Failure"));
						noFeed.setVisibility(View.VISIBLE);
						noFeedImage.setVisibility(View.VISIBLE);
					}
				} else { // no internet
					noFeed.setVisibility(View.VISIBLE);
					noFeedImage.setVisibility(View.VISIBLE);
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
				progressBar.setVisibility(View.GONE);
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}

	class FeedAdapter extends ArrayAdapter<FeedItem> {
		FeedItem item;
		ImageView thumbnail;
		TextView rating;
		TextView channel;
		TextView caption;
		TextView username, time;
		ProgressBar progressThumbnail, progressLoading;

		public FeedAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, feed);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			item = getItem(position);

			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_feed, parent, false);
			}

			thumbnail = (ImageView) convertView.findViewById(R.id.imageView_feed_item);
			progressThumbnail = (ProgressBar) convertView.findViewById(R.id.progress_feed_thumbnail);
			progressLoading = (ProgressBar) convertView.findViewById(R.id.progress_feed_loading);
//			rating = (TextView) convertView.findViewById(R.id.textView_feed_item_rating);
			caption = (TextView) convertView.findViewById(R.id.textView_feed_item_caption);
//			comments = (TextView) convertView.findViewById(R.id.textView_feed_item_comments);
			channel = (TextView) convertView.findViewById(R.id.textView_feed_item_channel);
			username = (TextView) convertView.findViewById(R.id.textView_feed_item_username);
			time = (TextView) convertView.findViewById(R.id.textView_feed_item_time);

			if (item.getThumbnailId().length() > 0 && !item.isDownloadThumbnailFailed()) {
				if (Video.loadedDir == null) { // if crash happens
					Video.setPaths(mActivity);
					Logger.init(mActivity);
				}
				String path = Video.loadedDir.getAbsolutePath();
				File thumbnailFile = new File(path+"/"+item.getThumbnailId()+".jpg");
				if (thumbnailFile.exists()) {
					Bitmap src = BitmapFactory.decodeFile(thumbnailFile.getPath());
					RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(resources, src);
					dr.setCircular(true);
					thumbnail.setImageDrawable(dr);
					thumbnail.setVisibility(View.VISIBLE);
					progressThumbnail.setVisibility(View.INVISIBLE);
				} else {
					progressThumbnail.setVisibility(View.VISIBLE);
					thumbnail.setVisibility(View.INVISIBLE);
					DownloadThumbnail downloadTask = new DownloadThumbnail(item, progressThumbnail);
					downloadTask.execute("http://storage.googleapis.com/yander/" + item.getThumbnailId() + ".jpg");
				}
			} else {
				thumbnail.setVisibility(View.INVISIBLE);
				progressThumbnail.setVisibility(View.INVISIBLE);
			}

			if (item.isPlayable()) {
				progressLoading.setVisibility(View.INVISIBLE);
			} else if (item.isDownloading()) {
				progressLoading.setVisibility(View.VISIBLE);
			} else if (item.isVideosEmpty()) {
				progressLoading.setVisibility(View.INVISIBLE);
			} else if (item.isEmpty()) {
				progressLoading.setVisibility(View.INVISIBLE);
			}

			caption.setText(item.getCaption());
//			rating.setText(item.getRating());
//			comments.setText(item.getComments());
			channel.setText("#" + item.getChannel());
			username.setText("@" + item.getUsername());
			time.setText(item.getTs());

			convertView.setOnClickListener(new View.OnClickListener() {
				FeedItem myItem = item;
				ProgressBar myProgressLoading = progressLoading;

				@Override
				public void onClick(View v) {
					if (myItem.isPlayable() && !GetVideosTask.loading) {
						Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
						intentFeedStart.putExtra("feedItemId", myItem.getId());
						startActivity(intentFeedStart);
					} else if (!GetVideosTask.loading) {
						GetVideosTask.loading = true;
						myItem.setDownloading(true);
						if (homeFeed) {
							Logger.trackEvent(mActivity, "Home Feed", "Load Request");
						} else {
							Logger.trackEvent(mActivity, "Profile", "Load Request");
						}
						myProgressLoading.setVisibility(View.VISIBLE);
						Logger.log(Log.INFO, TAG, "Loading video " + myItem.getVideoId());
						GetVideosTask getFeed = new GetVideosTask(mActivity, myItem, myItem.getId(), feedAdapter, feedInfo, "new");
						getFeed.execute();
					}
				}
			});

			// Return the completed view to render on screen
			return convertView;

		}

	}

	class DownloadThumbnail extends DownloadTask {
		FeedItem item;
		ProgressBar progressThumbnail;
		DownloadThumbnail (FeedItem item, ProgressBar progressThumbnail) {
			super(mActivity);
			this.item = item;
			this.progressThumbnail = progressThumbnail;
		}

		@Override
		protected void onPostExecute(Integer error) {
			super.onPostExecute(error);
			if (!isAdded()) {
				return;
			}
			if (error > 0) {
				item.setDownloadThumbnailFailed();
				progressThumbnail.setVisibility(View.INVISIBLE);
			}
			feedAdapter.notifyDataSetChanged();
		}
	}
}