package com.yonder.android;

import android.app.Activity;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

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
	static File uploadDir;
	static File loadedDir;
	static boolean obfuscating;

	// Constructor to convert JSON object into a Java class instance
	public Video(JSONObject object){
		try {
			this.id = object.getString("id");
			this.caption = object.getString("caption");
			this.commentsTotal = object.getString("comments_total");
			this.rating = object.getString("rating");
		} catch (JSONException e) {
			e.printStackTrace();
			Crashlytics.logException(e);
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
				Crashlytics.logException(e);
			}
		}
		return videos;
	}

	static void obfuscate(boolean encrypt) {
		if (obfuscating) {
			return;
		}
		obfuscating = true;
		File [] listFile = loadedDir.listFiles();
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
		obfuscating = false;
	}

	static void swapByte(File file) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			raf.seek(5);
			int b1 = raf.read();
			raf.seek(11);
			int b2 = raf.read();
			raf.seek(11);
			raf.write(b1);
			raf.seek(5);
			raf.write(b2);
			Crashlytics.log(Log.INFO, "Log.Video", "Swap Bytes " + file.getPath() + " b1 " + b1 + " b2 " + b2);
		} catch (Exception e) {
			e.printStackTrace();
			Crashlytics.logException(e);
		} finally {
			try {
				if (raf != null) {
					raf.close(); // Flush/save changes and close resource.
				}
			} catch (IOException e) {
				e.printStackTrace();
				Crashlytics.logException(e);
			}
		}
	}

	static void cleanup (File folder) {
		File [] listFile = folder.listFiles();
		if (listFile != null) {
			for (File file :listFile) {
				long last = file.lastModified();
				long now = System.currentTimeMillis();
				if ((now - last)/3600000 > 24 || file.getAbsolutePath().contains(".mp4")) {
					Crashlytics.log(Log.INFO, "Log.Video", "Deleting " + file.getAbsolutePath());
					file.delete();
				}
			}
		}
	}

	static void setPaths(Activity activity) {
		loadedDir = activity.getExternalFilesDir("lvd");
		uploadDir = activity.getExternalFilesDir("uvd");
	}
}