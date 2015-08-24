package yonder.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class YonderDbHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database version.
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "Yonder.db";

	private static final String SQL_CREATE_ENTRIES = "CREATE TABLE `comments` (" +
			"  `comment_id` varchar(25) NOT NULL," +
			"  `video_id` varchar(25) DEFAULT NULL," +
			"  `rated` int(11) DEFAULT NULL," +
			"  `flagged` int(11) DEFAULT NULL," +
			"  `ts` integer DEFAULT NULL," +
			"  PRIMARY KEY (`comment_id`)" +
			")";

	public YonderDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ENTRIES);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}