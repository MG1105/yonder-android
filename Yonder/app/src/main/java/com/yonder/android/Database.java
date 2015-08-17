package com.yonder.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

public class Database {
	private final String TAG = "Log." + this.getClass().getSimpleName();

	protected void flagComment(SQLiteDatabase db, String commentId, String videoId) {
		ContentValues values = new ContentValues();
		values.put("comment_id", commentId);
		values.put("video_id", videoId);
		values.put("flagged", 1);
		values.put("ts", System.currentTimeMillis());
		int updated = db.update("comments", values, "comment_id = ? and video_id = ?", new String[]{commentId, videoId});
		if (updated==0) {
			db.insert("comments", null, values);
		}
		Crashlytics.log(Log.INFO, TAG, "Recording comment flag " + values.valueSet().toString());
	}

	protected boolean isFlagged(SQLiteDatabase db, String commentId, String videoId) {
		String[] projection = {"flagged"};
		String selection = "comment_id = ? and video_id = ? and flagged = 1";
		String[] selectionArgs = {commentId, videoId};
		Cursor c = db.query(
				"comments",  // The table to query
				projection,                               // The columns to return
				selection,                                // The columns for the WHERE clause
				selectionArgs,                            // The values for the WHERE clause
				null,                                     // don't group the rows
				null,                                     // don't filter by row groups
				null                                 // The sort order
		);
		if (c.getCount() > 0) {
			return true;
		}
		return false;
	}

	protected void rateComment(SQLiteDatabase db, String commentId, String videoId) {
		ContentValues values = new ContentValues();
		values.put("comment_id", commentId);
		values.put("video_id", videoId);
		values.put("rated", 1);
		values.put("ts", System.currentTimeMillis());
		int updated = db.update("comments", values, "comment_id = ? and video_id = ?", new String[]{commentId, videoId});
		if (updated==0) {
			db.insert("comments", null, values);
		}
		Crashlytics.log(Log.INFO, TAG, "Recording comment rating " + values.valueSet().toString());
	}

	protected boolean isRated(SQLiteDatabase db, String commentId, String videoId) {
		String[] projection = {"rated"};
		String selection = "comment_id = ? and video_id = ? and rated = 1";
		String[] selectionArgs = {commentId, videoId};
		Cursor c = db.query(
				"comments",  // The table to query
				projection,                               // The columns to return
				selection,                                // The columns for the WHERE clause
				selectionArgs,                            // The values for the WHERE clause
				null,                                     // don't group the rows
				null,                                     // don't filter by row groups
				null                                 // The sort order
		);
		if (c.getCount() > 0) {
			return true;
		}
		return false;
	}

	protected void cleanup(SQLiteDatabase db) {
		// Define 'where' part of query.
		long now = System.currentTimeMillis();
		String selection = "("+ now+ "- ts)/3600000 > 24";
		// Specify arguments in placeholder order.
//		String[] selectionArgs = {now+""};
		// Issue SQL statement.
		db.delete("comments", selection, null);
	}


}