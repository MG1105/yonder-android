package com.vidici.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class Notification extends  Loadable{
	private String content;


	// Constructor to convert JSON object into a Java class instance
	public Notification(JSONObject object) {
		try {
			this.content = object.getString("content");
			this.videoId = object.getString("video_id");
			this.channelId = object.getString("channel_id");
			this.id = Long.toString(System.currentTimeMillis());
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public String getContent() {
		return content;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Notification> fromJson(JSONArray jsonObjects) {
		ArrayList<Notification> list = new ArrayList<>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				list.add(new Notification(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return list;
	}

}