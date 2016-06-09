package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

class GetVideosTask extends AsyncTask<Void, Void, JSONObject> {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	Loadable loadable;
	ArrayList<String> uris;
	Activity mActivity;
	String userId, loadableId, channelSort;
	ArrayAdapter adapter;
	HashMap<String, LinkedHashMap<String, JSONObject>> info;
	static boolean loading = false;

	protected GetVideosTask(Activity activity, Loadable loadable, String id, ArrayAdapter adapter, HashMap<String, LinkedHashMap<String, JSONObject>> info ,
	                        String channelSort) {
		this.loadable = loadable;
		this.channelSort = channelSort;
		loadableId = id;
		this.info = info;
		this.adapter = adapter;
		mActivity = activity;
		userId = User.getId(mActivity);
	}

	protected JSONObject doInBackground(Void... params) {
		try {
			uris = new ArrayList<>();
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, String.format("Getting videos for userId %s",
					userId));
			JSONObject response = gae.getVideos(userId, loadable.getChannelId(), loadable.getVideoId(), channelSort);
			return response;
		} catch (Exception e) {
			Logger.log(e);
			return null;
		}
	}

	protected void onPostExecute(JSONObject response) {
		if (response != null) {
			try {
				if (response.getString("success").equals("1")) {
					JSONArray videos = response.getJSONArray("videos");

					LinkedHashMap<String, JSONObject> videoInfo = new LinkedHashMap<>();
					for (int i = 0; i < videos.length(); i++) { // background?
						JSONObject vid = videos.getJSONObject(i);
						videoInfo.put(vid.getString("id"), vid);

						String id = vid.getString("id");
						loadable.addVideo(id);

						if (isLoaded(id)) {
							continue;
						}
						String url = "http://storage.googleapis.com/yander/" + id + ".mp4";
						uris.add(url);
					}

					if (videos.length() > 0) {
						info.put(loadableId, videoInfo);
						loadable.setEmpty(false);

						if (uris.size() == 0) {
							Logger.log(Log.INFO, TAG, "All videos in cache");
							playVideos();
						} else {
							Logger.log(Log.INFO, TAG, uris.size() + " more videos to download");
							DownloadVideos downloadVideos = new DownloadVideos(loadable);
							downloadVideos.execute(uris.get(0));
						}
					} else {
						loadable.setDownloading(false);
						loadable.setEmpty(true);
						loading = false;
						adapter.notifyDataSetChanged();
						Toast.makeText(mActivity, "No new broadcasts", Toast.LENGTH_LONG).show();
					}

				} else {
					Logger.log(new Exception("Server Side Failure"));
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					loadable.setDownloading(false);
					loading = false;
					adapter.notifyDataSetChanged();

				}
			} catch (Exception e) {
				Logger.log(e);
				Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				loadable.setDownloading(false);
				loading = false;
				adapter.notifyDataSetChanged();
			}
		} else {
			Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
			loadable.setDownloading(false);
			loading = false;
			adapter.notifyDataSetChanged();
		}
	}

	private boolean isLoaded (String id) {
		boolean loaded = new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile();
		return loaded;
	}

	class DownloadVideos extends DownloadTask {
		Loadable loadable;
		DownloadVideos (Loadable loadable) {
			super(mActivity);
			this.loadable = loadable;
		}

		@Override
		protected void onPostExecute(Integer error) {
			super.onPostExecute(error);
			playVideos();
		}
	}

	protected void playVideos() {
		loadable.setDownloading(false);
		loadable.setPlayable();
		Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
		String loadableId = "";
		if (loadable instanceof Channel) {
			loadableId = "channelId";
		} else if (loadable instanceof Notification) {
			loadableId = "notificationId";
		} else if (loadable instanceof FeedItem) {
			loadableId = "feedItemId";
		}
		intentFeedStart.putExtra(loadableId, loadable.getId());
		mActivity.startActivity(intentFeedStart);
		loadable.setReload();
		loading = false;
		adapter.notifyDataSetChanged();
	}

}
