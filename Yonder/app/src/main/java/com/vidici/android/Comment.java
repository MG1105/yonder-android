package com.vidici.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Comment {
	private String content;
	private String id;
	private String rating;
	private String nickname;
	private String videoId;
	private int rated = 0;
	private int myRating;

	// Constructor to convert JSON object into a Java class instance
	public Comment(JSONObject object, String videoId){
		try {
			this.id = object.getString("id");
			this.content = object.getString("content");
			this.rating = object.getString("rating");
			this.nickname = object.getString("username");
			this.videoId = videoId;
			this.rated = object.getInt("rated");
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public Comment(String id, String comment, String nickname){
		this.id = id;
		this.content = comment;
		this.rating = "0";
		this.nickname = nickname;
	}

	public void updateRating() {
		int overrideOldRating = 0;
		if (rated == 1) {
			overrideOldRating = -1;
		} else if (rated == -1) {
			overrideOldRating = 1;
		}
		int rating = Integer.valueOf(this.rating) + myRating + overrideOldRating;
		this.rating = "" + rating;
		rated = myRating;
	}

	public void setRating(int rating) { // tmp until post exec calls updateRating
		myRating = rating;
	}

	public int getRated() {
		return rated;
	}

	public String getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public String getRating() {
		return rating;
	}

	public String getNickname() {
		return nickname;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Comment> fromJson(JSONArray jsonObjects, String videoId) {
		ArrayList<Comment> users = new ArrayList<Comment>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				users.add(new Comment(jsonObjects.getJSONObject(i), videoId));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return users;
	}
}