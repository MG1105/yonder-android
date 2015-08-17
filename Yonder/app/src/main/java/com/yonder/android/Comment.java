package com.yonder.android;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

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
	private boolean rated;
	private boolean flagged;

	// Constructor to convert JSON object into a Java class instance
	public Comment(JSONObject object, String videoId){
		try {
			this.id = object.getString("id");
			this.content = object.getString("content");
			this.rating = object.getString("rating");
			this.nickname = object.getString("nickname");
			this.videoId = videoId;
			this.flagged = isFlaggedQuery();
			this.rated = isRatedQuery();
		} catch (JSONException e) {
			e.printStackTrace();
			Crashlytics.logException(e);
		}
	}

	public Comment(String id, String comment, String nickname){
		this.id = id;
		this.content = comment;
		this.rating = "0";
		this.nickname = nickname;
	}

	public boolean isFlaggedQuery () {
		Database db = new Database();
		return db.isFlagged(CommentActivity.yonderDb, id, videoId);
	}

	public boolean isFlagged () {
		return flagged;
	}

	public boolean isRatedQuery () {
		Database db = new Database();
		return db.isRated(CommentActivity.yonderDb, id, videoId);
	}

	public boolean isRated () {
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

	public String updateRating(int add) {
		int rating = Integer.valueOf(this.rating) + add;
		this.rating = "" + rating;
		return this.rating;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Comment> fromJson(JSONArray jsonObjects, String videoId) {
		ArrayList<Comment> users = new ArrayList<Comment>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				users.add(new Comment(jsonObjects.getJSONObject(i), videoId));
			} catch (JSONException e) {
				e.printStackTrace();
				Crashlytics.logException(e);
			}
		}
		return users;
	}
}