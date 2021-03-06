package com.vidici.android;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Channel extends Loadable{
	private String rating;
	private int myRating, gold;
	private String name;
	private String username;
	private int rated = 0;
	private String unseen, videos, nsfw;
	private String ts;

	// Constructor to convert JSON object into a Java class instance
	public Channel(JSONObject object) {
		try {
			this.id = object.getString("id");
			this.channelId = object.getString("id");
			this.name = object.getString("name");
			this.rating = object.getString("rating");
			this.unseen = object.getString("unseen");
			this.nsfw = object.getString("nsfw");
			this.videos = object.getString("videos");
			this.username = object.getString("username");
			this.gold = object.getInt("gold");
			this.ts = getRemainingTime(object.getLong("ts"));
			rated = object.getInt("rated");
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public String getTs() {
		return ts;
	}

	public int getGold() {
		return gold;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public String getUnseen() {
		return unseen;
	}

	public String isNsfw() {
		return nsfw;
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

	public String getRating() {
		return rating;
	}

	public int getRated() {
		return rated;
	}

	// Factory method to convert an array of JSON objects into a list of objects
	// User.fromJson(jsonArray);
	public static ArrayList<Channel> fromJson(JSONArray jsonObjects) {
		ArrayList<Channel> list = new ArrayList<>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				list.add(new Channel(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return list;
	}

	String getRemainingTime(long ts) {
		long delta = (System.currentTimeMillis() / 1000) - ts;
		if (delta < 0) {
			return "";
		}

		long days = TimeUnit.SECONDS.toDays(delta);
		if (days >= 1) {
			return days + "d";
		} else {
			long hour = TimeUnit.SECONDS.toHours(delta);
			if (hour >= 1) {
				return hour + "h";
			} else {
				long min = TimeUnit.SECONDS.toMinutes(delta);
				if (min >= 1) {
					return min + "m";
				} else {
					long sec = TimeUnit.SECONDS.toSeconds(delta);
					return sec + "s";
				}
			}
		}
	}

}