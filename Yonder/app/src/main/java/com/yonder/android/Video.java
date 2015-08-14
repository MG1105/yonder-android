package com.yonder.android;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Video {
	private String id;
	private String rating;
	private String commentsTotal;
	private String caption;
	static String uploadPath;
	static String loadedPath;

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

	static void obfuscate(Activity activity, boolean encrypt) {
		File videosFolder  = activity.getExternalFilesDir("loaded_videos");
		File [] listFile = videosFolder.listFiles();
		if (listFile != null) {
			for (File file :listFile) {
				if ( encrypt && file.getAbsolutePath().contains(".mp4")) {
					File out = new File(file.getAbsolutePath().replace(".mp4",""));
					file.renameTo(out);
					swapByte(out);
				} else if (!encrypt && !file.getAbsolutePath().contains(".mp4")) {
					File out = new File(file.getAbsolutePath()+ ".mp4");
					file.renameTo(out);
					swapByte(out);
				}
			}
		}
	}

	static void swapByte(File file) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			raf.seek(5);
			int b1 = raf.read();

			raf.seek(11);
			int b2 = raf.read();

			raf.seek(11);
			raf.write(b1);
			raf.seek(5);
			raf.write(b2);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raf.close(); // Flush/save changes and close resource.
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void cleanup (File folder) {
		File [] listFile = folder.listFiles();
		if (listFile != null) {
			for (File file :listFile) {
				long last = file.lastModified();
				long now = System.currentTimeMillis();
				if ((now - last)/360000 > 24) {
					file.delete();
				}
			}
		}
	}
}