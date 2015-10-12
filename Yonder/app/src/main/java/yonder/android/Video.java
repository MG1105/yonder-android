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

public class Video {
	private String id;
	private String rating;
	private String commentsTotal;
	private String caption;
	static File uploadDir;
	static File loadedDir;

	// Constructor to convert JSON object into a Java class instance
	public Video(JSONObject object){
		try {
			this.id = object.getString("id");
			this.caption = object.getString("caption");
			this.commentsTotal = object.getString("comments_total");
			this.rating = object.getString("rating");
		} catch (JSONException e) {
			Logger.log(e);
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
				Logger.log(e);
			}
		}
		return videos;
	}

	static synchronized void obfuscate(boolean encrypt) {
		File [] listFile = loadedDir.listFiles();
		if (listFile != null) {
			for (File file :listFile) {
				if ( encrypt && file.getAbsolutePath().endsWith(".mp4") && file.length()>0) {
					File out = new File(file.getAbsolutePath().replace(".mp4",""));
					file.renameTo(out);
					swapByte(out);
					file.delete(); // make sure it is gone
				} else if (!encrypt && !file.getAbsolutePath().endsWith(".mp4") && file.length()>0) {
					File out = new File(file.getAbsolutePath()+ ".mp4");
					file.renameTo(out);
					swapByte(out);
					file.delete();
				}
			}
		}
	}

	static void swapByte(File file) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");

			raf.seek(5);
			int b1 = raf.read();
			raf.seek(11);
			int b2 = raf.read();

			if (b1 != -1 && b2 != -1) {
				raf.seek(11);
				raf.write(b1);
				raf.seek(5);
				raf.write(b2);
				Logger.log(Log.INFO, "Log.Video", "Swap Bytes " + file.getPath() + " b1 " + b1 + " b2 " + b2);
			} else {
				Logger.log(Log.ERROR, "Log.Video", "Skip swapping bytes because b1 or b2 = -1 in "
						+ file.getPath());
				Logger.log(new Exception("Failed to swap bytes"));
			}
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			try {
				if (raf != null) {
					raf.close(); // Flush/save changes and close resource.
				}
			} catch (IOException e) {
				Logger.log(e);
			}
		}
	}

	static void cleanup (File folder, boolean upload) {
		File [] listFile = folder.listFiles();
		if (listFile != null) {
			for (File file :listFile) {
				if (file.getAbsolutePath().endsWith("nomedia")) {
					continue;
				}
				long last = file.lastModified();
				long now = System.currentTimeMillis();
				if ((now - last)/3600000 > 24 || (file.getAbsolutePath().endsWith(".mp4") && !upload)) {
					Logger.log(Log.INFO, "Log.Video", "Deleting " + file.getAbsolutePath());
					file.delete();
				}
			}
		}
	}

	static void setPaths(Activity activity) {
		loadedDir = activity.getExternalFilesDir("lvd");
		uploadDir = activity.getExternalFilesDir("uvd");

		File loadedNomedia = new File(loadedDir, ".nomedia");
		File uploadNomedia = new File(uploadDir, ".nomedia");
		try {
			loadedNomedia.createNewFile();
			uploadNomedia.createNewFile();
		} catch (IOException e) { // Seen on TCT/TCL unknown chinese devices
			Logger.log(e);
			Toast.makeText(activity, "Sorry, we do not support this device for now", Toast.LENGTH_LONG).show();
			activity.finish();
		}
	}
}