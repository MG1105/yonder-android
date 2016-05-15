package com.vidici.android;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class LoadBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(downloadId);
			DownloadManager mDownloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
			Cursor c = mDownloadManager.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

				String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
				//check if this is your download uri
				if (!uri.contains("yander"))
					return;

//				String downloadedPackageUriString =
//						c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
				try {
					String downloadedPackageUriString = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))).getPath();
					File file = new File(downloadedPackageUriString);
					File out = new File(downloadedPackageUriString.replace(".tmp",""));

					//if completed successfully
					if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)){
						file.renameTo(out);
						file.delete(); // make sure it is gone
						Logger.log(Log.INFO, "Log.LoadBroadcastReceiver", "Downloaded " + uri);
					}else if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)){
						//retry? infinite loop?
						Logger.log(Log.ERROR, "Log.LoadBroadcastReceiver", "Failed downloading video " + uri);
						file.delete();
					}
				} catch (NullPointerException e) {
					Logger.log(Log.ERROR, "Log.LoadBroadcastReceiver", "Failed downloading " + uri);
					Logger.log(e);
				}
			}
			if (ChannelActivity.active) {
				if (ChannelActivity.adapter != null) {
					ChannelActivity.adapter.notifyDataSetChanged();
				}
			}
			if (NotificationActivity.active) {
				if (NotificationActivity.adapter != null) {
					NotificationActivity.adapter.notifyDataSetChanged();
				}
			}
		}
	}
}
