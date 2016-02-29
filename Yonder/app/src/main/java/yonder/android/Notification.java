package yonder.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class Notification {
	private String id;
	private String content;
	String videoId;
	String channelId;
	private Boolean loading = false;
	ArrayList<String> videos = new ArrayList<>();

	// Constructor to convert JSON object into a Java class instance
	public Notification(JSONObject object) {
		try {
			this.content = object.getString("content");
			this.videoId = object.getString("video_id");
			this.channelId = object.getString("channel_id");
			if (videoId.equals("")) videoId = null;
			if (channelId.equals("")) channelId = null;
			this.id = Long.toString(System.currentTimeMillis());
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public String getId() {
		return id;
	}

	public String getVideoId() {
		return videoId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setReload() {
		videos.clear();
	}

	public String getContent() {
		return content;
	}

	public Boolean isLoaded() {
		if (videos.isEmpty())
			return false;
		boolean loaded = true;
		for (String id : videos) {
			if (!new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile()){
				loaded = false;
				break;
			}
		}
		return loaded;
	}

	public Boolean isLoading() {
		return loading;
	}

	public void setLoading(Boolean loading) {
		this.loading = loading;
	}

	public void addVideo(String video) {
		videos.add(video);
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Notification> fromJson(JSONArray jsonObjects) {
		ArrayList<Notification> channels = new ArrayList<Notification>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				channels.add(new Notification(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return channels;
	}

}