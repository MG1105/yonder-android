package yonder.android;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class Alert {

	public static void showWarning (Activity activity) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Warning");
		alertDialog.setMessage("Many users found the content you have posted offensive.\n" +
				"If more users flag your content, you will be automatically banned from Yondor.\n" +
				"The Yondor Team would hate to see you go. Please help us build a positively fun community.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertDialog.setIcon(R.drawable.ic_flag1);
		alertDialog.show();
	}

	public static void ban (final Activity activity, int level) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Banned");
		String message = "";
		if (level == 1) {
			message = "Many users found the content you have posted offensive and flagged it.\n" +
					"You were automatically banned from Yondor for a week.\n" +
					"The Yondor Team hates to see you go. Please help us build a positively fun community when you come back.";
		} else if (level == 2) {
			message = "Many users found the content you have posted offensive and flagged it.\n" +
					"You were automatically banned from Yondor for a month.\n" +
					"The Yondor Team hates to see you go. Please help us build a positively fun community when you come back.";
		}
		alertDialog.setMessage(message);
		alertDialog.setCancelable(false);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		});
		alertDialog.setIcon(R.drawable.ic_flag1);
		alertDialog.show();

	}

	public static void showVideoRule (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
		String rule = sharedPreferences.getString("video_rule_seen", null);
		if (rule == null ) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setTitle("Automated Admin");
			alertDialog.setMessage("Please help us keep Yondor a positively fun community and refrain " +
					"from posting anything offensive.\nUsers who get flagged repeatedly" +
					" are automatically banned from Yondor.");
			alertDialog.setCancelable(false);
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					SharedPreferences sharedPreferences = activity.getSharedPreferences(
							"yonder.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putString("video_rule_seen", "yes").apply();
				}
			});
			alertDialog.setIcon(R.drawable.ic_flag1);
			alertDialog.show();
		}
	}

	public static void showCommentRule (final Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
		String rule = sharedPreferences.getString("comment_rule_seen", null);
		if (rule == null) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setTitle("Automated Admin");
			alertDialog.setMessage("Please help us keep Yondor a positively fun community by flagging" +
					" comments and videos you find offensive.\nUsers who get flagged repeatedly" +
					" are automatically banned from Yondor.");
			alertDialog.setCancelable(false);
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					SharedPreferences sharedPreferences = activity.getSharedPreferences(
							"yonder.android", Context.MODE_PRIVATE);
					sharedPreferences.edit().putString("comment_rule_seen", "yes").apply();
				}
			});
			alertDialog.setIcon(R.drawable.ic_flag1);
			alertDialog.show();
		}
	}

	public static void forceUpgrade (final Activity activity, final int level) { // non closing option

		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Upgrade Available");
		alertDialog.setMessage("Our servers have undergone a major revamp in order to increase performance and security.\n" +
				"Please upgrade to the newest version of Yondor on Google Play as this version is no longer supported.\n");
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
		alertDialog.setIcon(R.drawable.ic_flag1);
		alertDialog.show();
	}

}
