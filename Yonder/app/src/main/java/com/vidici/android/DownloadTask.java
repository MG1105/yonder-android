package com.vidici.android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class DownloadTask extends AsyncTask<String, Integer, Integer> {

	private Context context;
	private PowerManager.WakeLock mWakeLock;
	private final String TAG = "Log." + this.getClass().getSimpleName();
	String outputId = "";
	boolean outOfSpace = false;

	public DownloadTask(Context context) {
		this.context = context;
		if (Video.loadedDir.getFreeSpace() < 20000000) {
			Logger.log(Log.ERROR, TAG, "Running out of space");
			Toast.makeText(context, "Your phone is running out of space. Please increase your free memory and try again", Toast.LENGTH_LONG).show();
			outOfSpace = true;
		}
	}

	void setOutputId (String output) {
		outputId = output;
	}

	@Override
	protected Integer doInBackground(String... urls) {
		if (outOfSpace) {
			return 1;
		}
		for (String url : urls) {
			if (new File(Video.loadedDir + "/" + url.substring(url.lastIndexOf('/') + 1)).exists()) {
				continue;
			}
			if (download(url) != 0) {
				return 1;
			}
		}
		return 0;
	}

	protected int download (String path) {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		String outputPath;
		try {
			URL url = new URL(path);
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			// expect HTTP 200 OK, so we don't mistakenly save error report
			// instead of the file
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Logger.log(Log.ERROR, TAG, "Server returned HTTP " + connection.getResponseCode()
						+ " " + connection.getResponseMessage());
				return 1;
			}

			if (outputId.length() > 0) {
				outputPath = Video.loadedDir + "/" + outputId + ".tmp";
			} else {
				outputPath = Video.loadedDir + "/" + path.substring(path.lastIndexOf('/') + 1) + ".tmp";
			}

			// download the file
			input = connection.getInputStream();
			output = new FileOutputStream(outputPath);

			byte data[] = new byte[4096];
			int count;
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
		} catch (Exception e) {
			Logger.log(e);
			return 1;
		} finally { // wont clean up if exception
			try {
				if (output != null)
					output.close();
				if (input != null)
					input.close();
			} catch (IOException ignored) {
			}
			if (connection != null)
				connection.disconnect();
		}
		File file = new File(outputPath);
		File out = new File(outputPath.replace(".tmp", ""));
		file.renameTo(out);
		file.delete(); // make sure it is gone
		return 0;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// take CPU lock to prevent CPU from going off if the user
		// presses the power button during download
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		mWakeLock.acquire();
	}

	@Override
	protected void onPostExecute(Integer error) {
		mWakeLock.release();
		if (error == 0) {
			Logger.log(Log.INFO, TAG, "Download completed");
		} else {
			Logger.log(Log.ERROR, TAG, "Download failed");
		}
	}

}