package com.vidici.android;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Window;
import android.widget.Toast;

public class Alert {

	public static void showWarning (Activity activity) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		alertDialog.setMessage("You have received many downvotes. " +
				"If users keep downvoting you, you will be automatically banned from Vidici. " +
				"We would hate to see you go. Please help us build a positive and fun community.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertDialog.show();
	}

	public static void ban (final Activity activity, int level) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		String message = "";
		if (level == 1) {
			message = "You have received too many downvotes. " +
					"You were automatically banned from Vidici for a week. " +
					"We hate to see you go. Please come back and help us build a positive and fun community";
		} else if (level == 2) {
			message = "You have received too many downvotes. " +
					"You were automatically banned from Vidici for a month. " +
					"We hate to see you go. Please come back and help us build a positive and fun community";
		}
		alertDialog.setMessage(message);
		alertDialog.setCancelable(false);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		});
		alertDialog.show();

	}

	public static void showVideoRule (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		String rule = sharedPreferences.getString("video_rule_seen", null);
		if (rule == null ) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			alertDialog.setMessage("Please refrain from posting anything offensive. " +
					"Users who get downvoted repeatedly are automatically banned.");
			alertDialog.setCancelable(false);
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					SharedPreferences sharedPreferences = activity.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putString("video_rule_seen", "yes").apply();
				}
			});
			alertDialog.show();
		}
	}

	public static void showCommentRule (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		String rule = sharedPreferences.getString("comment_rule_seen", null);
		if (rule == null) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			alertDialog.setMessage("Vidici is a bully free community. Please downvote abusive content. "+
					"Users who get downvoted repeatedly are automatically banned.");
			alertDialog.setCancelable(false);
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					SharedPreferences sharedPreferences = activity.getSharedPreferences(
							"com.vidici.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putString("comment_rule_seen", "yes").apply();
				}
			});
			alertDialog.show();
		}
	}

	public static void showChannelIntro (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		int count = sharedPreferences.getInt("channel_intro_count", 0);
		String watch = "Tap any hashtag to watch its video feed";
		String record = "Long press any hashtag to post videos to it";
		String hashtags = "Hashtags are ranked in the first 3 tabs (most likes, trending, and newest)";
		if (count == 0) {
			Toast.makeText(activity, watch, Toast.LENGTH_LONG).show();
			Toast.makeText(activity, watch, Toast.LENGTH_LONG).show();
			sharedPreferences.edit().putInt("channel_intro_count", ++count).apply();
		} else if (count == 1) {
			Toast.makeText(activity, hashtags, Toast.LENGTH_LONG).show();
			Toast.makeText(activity, hashtags, Toast.LENGTH_LONG).show();
			sharedPreferences.edit().putInt("channel_intro_count", ++count).apply();
		} else if (count == 2) {
			Toast.makeText(activity, record, Toast.LENGTH_LONG).show();
			Toast.makeText(activity, record, Toast.LENGTH_LONG).show();
			sharedPreferences.edit().putInt("channel_intro_count", ++count).apply();
		}
	}

	public static void showInvite(final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		int count = sharedPreferences.getInt("invite_count_16", 0);
		if (count == 2 || count == 5) {
			Intent intent = new Intent(activity, InviteActivity.class);
			activity.startActivity(intent);
		}
		if (count <= 5) {
			sharedPreferences.edit().putInt("invite_count_16", ++count).apply();
		}
	}

	public static void showStoryIntro (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		int count = sharedPreferences.getInt("story_intro_count", 0);
		String tap = "Tap to skip to the next video";
		if (count == 0) {
			Toast.makeText(activity, tap, Toast.LENGTH_LONG).show();
			sharedPreferences.edit().putInt("story_intro_count", ++count).apply();
		}
	}

	public static void forceUpgrade (final Activity activity, final int level) {

		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		alertDialog.setMessage("Our servers have undergone a major revamp in order to increase performance and security. " +
				"Please upgrade to the newest version of Vidici on Google Play as this version is no longer supported.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (level == 2) {
					activity.finish();
				} else {
					dialog.cancel();
				}
			}
		});
		alertDialog.show();
	}

}
