package com.vidici.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FeedItem extends  Loadable{
	private String caption;
	private String rating;
	private String comments;
	private String channel;
	private String username;


	// Constructor to convert JSON object into a Java class instance
	public FeedItem(JSONObject object) {
		try {
			this.caption = object.getString("caption");
			this.videoId = object.getString("video_id");
			this.channelId = object.getString("channel_id");
			this.thumbnailId = object.getString("thumbnail_id");
			this.id = Long.toString((int)(Math.random() * 1000000 + 1));
			this.rating = object.getString("rating");
			this.comments = object.getString("comments_count");
			this.channel = object.getString("channel_name");
			this.username = object.getString("username");
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public String getCaption() {
		return caption;
	}

	public String getRating() {
		return rating;
	}

	public String getComments() {
		return comments;
	}

	public String getChannel() {
		return channel;
	}

	public String getUsername() {
		return username;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<FeedItem> fromJson(JSONArray jsonObjects) {
		ArrayList<FeedItem> list = new ArrayList<>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				list.add(new FeedItem(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return list;
	}

}