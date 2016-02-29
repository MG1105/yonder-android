package yonder.android;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Channel {
	private String id;
	private String rating;
	private int myRating;
	private String name;
	private Boolean loading = false;
	private int rated = 0;
	private String unseen;
	ArrayList<String> videos = new ArrayList<>();

	// Constructor to convert JSON object into a Java class instance
	public Channel(JSONObject object) {
		try {
			this.id = object.getString("id");
			this.name = object.getString("name");
			this.rating = object.getString("rating");
			this.unseen = object.getString("unseen");
			rated = object.getInt("rated");
		} catch (JSONException e) {
			Logger.log(e);
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUnseen() {
		return unseen;
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

	public void setReload() {
		videos.clear();
	}

	public void setLoading(Boolean loading) {
		this.loading = loading;
	}

	public void addVideo(String video) {
		videos.add(video);
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
		ArrayList<Channel> channels = new ArrayList<Channel>();
		for (int i = 0; i < jsonObjects.length(); i++) {
			try {
				channels.add(new Channel(jsonObjects.getJSONObject(i)));
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		return channels;
	}

}