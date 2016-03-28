package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
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

class GetFeedTask extends AsyncTask<Void, Void, JSONObject> {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	Loadable loadable;
	TextView load;
	ArrayList<Uri> uris;
	Activity mActivity;
	String userId, loadableId, channelSort;
	private DownloadManager downloadManager;
	ArrayAdapter adapter;
	HashMap<String, LinkedHashMap<String, JSONObject>> info;

	protected GetFeedTask(Activity activity, Loadable loadable, String id, ArrayAdapter adapter, HashMap<String, LinkedHashMap<String, JSONObject>> info , TextView myLoad, String channelSort) {
		this.loadable = loadable;
		this.channelSort = channelSort;
		loadableId = id;
		this.info = info;
		load = myLoad;
		this.adapter = adapter;
		mActivity = activity;
		userId = User.getId(mActivity);
		downloadManager = (DownloadManager) mActivity.getSystemService(Activity.DOWNLOAD_SERVICE);
	}

	protected JSONObject doInBackground(Void... params) {
		try {
			uris = new ArrayList<>();
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, String.format("Getting feed for userId %s",
					userId));
			JSONObject response = gae.getFeed(userId, loadable.getChannelId(), loadable.getVideoId(), channelSort);
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
						uris.add(Uri.parse(url));
					}

					if (videos.length() > 0) {
						info.put(loadableId, videoInfo);
						loadable.setEmpty(false);
						ArrayList<DownloadManager.Request> requests = getRequests(uris);
						for (DownloadManager.Request request : requests) {
							downloadManager.enqueue(request);
						}
						if (uris.size() == 0) {
							Logger.log(Log.INFO, TAG, "All videos in cache");
							// channel is ready
							adapter.notifyDataSetChanged();
							Logger.log(Log.INFO, TAG, "Channel modified");
						} else {
							Logger.log(Log.INFO, TAG, uris.size() + " more videos to download");
						}
					} else {
						load.setText("no reactions yet");
						loadable.setEmpty(true);
						load.setEnabled(true);
					}

				} else {
					Logger.log(new Exception("Server Side Failure"));
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					load.setText("load");
					load.setEnabled(true);

				}
			} catch (Exception e) {
				Logger.log(e);
				Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				load.setText("load");
				load.setEnabled(true);
			}
		} else {
			Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
			load.setText("load");
			load.setEnabled(true);
		}
	}

	private ArrayList<DownloadManager.Request> getRequests(ArrayList<Uri> uris) {
		ArrayList<DownloadManager.Request> requests = new ArrayList<>();
		for (Uri uri : uris) {
			DownloadManager.Request request = new DownloadManager.Request(uri);
			request.setVisibleInDownloadsUi(false);
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
			request.setDestinationUri(Uri.fromFile(new File(Video.loadedDir.getAbsolutePath() + "/" + uri.getLastPathSegment()+".tmp")));
			requests.add(request);
		}
		return requests;
	}

	private boolean isLoaded (String id) {
		boolean loaded = new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile();
		return loaded;
	}
}
