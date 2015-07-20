package com.yonder.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Comment {
	private String content;
	private String id;

	// Constructor to convert JSON object into a Java class instance
	public Comment(JSONObject object){
		try {
			this.id = object.getString("id");
			this.content = object.getString("content");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Comment> fromJson(JSONArray jsonObjects) {
		ArrayList<Comment> users = new ArrayList<Comment>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				users.add(new Comment(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return users;
	}
}