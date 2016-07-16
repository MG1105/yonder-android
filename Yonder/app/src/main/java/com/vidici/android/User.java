package com.vidici.android;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public class User {
	static boolean admin = true;
	static String androidId;

	public static String getId(Context context) {

		if (admin) {
			return "897d1e5hb8u47u56jh6";
		}
		SharedPreferences sharedPreferences = context.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
		if (sharedPreferences.getBoolean("logged_in", false)) {
			return sharedPreferences.getString("facebook_id", "");
		}

		return getAndroidId(context);
	}

	public static String getAndroidId(Context context) {
		if (androidId != null) {
			return androidId;
		}
		String androidId = Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
		if (androidId == null || androidId.length() == 0 || androidId.contains("9774d56d682e549c")) {
			androidId = "";
			androidId += Build.SERIAL;
			androidId += "xxx" + (Build.PRODUCT.length() % 10) + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10);
		}
		return androidId;
	}

	static boolean loggedIn(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
		if (!sharedPreferences.getBoolean("logged_in", false)) {
			Intent intent = new Intent(context, ProfileActivity.class);
//			intent.putExtra("profileId", profileId);
			context.startActivity(intent);
			return false;
		} else {
			return true;
		}
	}

	static boolean isLoggedIn(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("logged_in", false);
	}

	public static void setLocation(Context context) {
		UserLocation userLocation = new UserLocation();
		userLocation.update(context);
	}

	public static ArrayList<String> getLocation(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		String lon = sharedPreferences.getString("location_lon", null);
		String lat = sharedPreferences.getString("location_lat", null);
		if (lon != null && lat != null) {
			ArrayList<String> location = new ArrayList<>();
			location.add(lon);
			location.add(lat);
			return location;
		}
		return null; // default location?
	}

	public static void verify(Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		int upgrade = sharedPreferences.getInt("upgrade", 0);
		int ban = sharedPreferences.getInt("ban", 0);
		int warn = sharedPreferences.getInt("warn", 0);
		if (warn == 1) { // -1 already warned, 0 no warning
			Alert.showWarning(activity);
		} else if (upgrade != 0) { // if just upgraded after being blocked, this should not happen
			Alert.forceUpgrade(activity, upgrade);
		} else if (ban != 0) {
			Alert.ban(activity, ban);
		}
	}

	// Check if user is root

	public static void checkRoot(Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"com.vidici.android", Context.MODE_PRIVATE);
		long lastRootCheck = sharedPreferences.getLong("last_root", 0);
		long now = System.currentTimeMillis();
		if (lastRootCheck == 0 || (now - lastRootCheck) / 3600000 > 24) {
			Logger.log(Log.INFO, "Log.User", "Checking if device is rooted");
			try {
				if (checkRootMethod1() || checkRootMethod2() || checkRootMethod3()) {
					Logger.log(new Exception("Rooted Device"));
					Toast.makeText(activity, "Yonder is not supported on rooted devices for now", Toast.LENGTH_LONG).show();
					activity.finish();
					sharedPreferences.edit().putBoolean("root", true).apply();
				} else {
					Logger.log(Log.INFO, "Log.User", "Device is not rooted");
					sharedPreferences.edit().putBoolean("root", false).apply();
				}
				sharedPreferences.edit().putLong("last_root", now).apply();
				return;
			} catch (Exception e) {
				Logger.log(e);;
			}
		}
		if (sharedPreferences.getBoolean("root", false)) {
			Logger.log(Log.INFO, "Log.User", "Exiting since device is rooted");
			Toast.makeText(activity, "Yonder is not supported on rooted devices for now", Toast.LENGTH_LONG).show();
			activity.finish();
		}
	}

	private static boolean checkRootMethod1() {
		String buildTags = android.os.Build.TAGS;
		return buildTags != null && buildTags.contains("test-keys");
	}

	private static boolean checkRootMethod2() {
		String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
				"/system/bin/failsafe/su", "/data/local/su" };
		for (String path : paths) {
			if (new File(path).exists()) return true;
		}
		return false;
	}

	private static boolean checkRootMethod3() { // creates zombie processes?
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if (in.readLine() != null) return true;
			return false;
		} catch (Throwable t) {
			return false;
		} finally {
			if (process != null) process.destroy();
		}
	}


}
