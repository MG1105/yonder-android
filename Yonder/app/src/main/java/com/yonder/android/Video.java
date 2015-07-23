package com.yonder.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Video {
	private String id;
	private String rating;
	private String commentsTotal;
	private String caption;

	// Constructor to convert JSON object into a Java class instance
	public Video(JSONObject object){
		try {
			this.id = object.getString("id");
			this.caption = object.getString("caption");
			this.commentsTotal = object.getString("comments_total");
			this.rating = object.getString("rating");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return id;
	}

	public String getRating() {
		return rating;
	}

	public String getCommentsTotal() {
		return commentsTotal;
	}

	public String getCaption() {
		return caption;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Video> fromJson(JSONArray jsonObjects) {
		ArrayList<Video> videos = new ArrayList<Video>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				videos.add(new Video(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return videos;
	}
}