package yonder.android;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

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

	public static void setLocation(Context context) {
		UserLocation userLocation = new UserLocation();
		userLocation.update(context);
	}

	public static ArrayList<String> getLocation(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
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

	public static String getNickname (Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
		String nickname = sharedPreferences.getString("nickname", null);
		if (nickname == null || admin) {
			String AB = "abcdefghijklmnopqrstuvwxyz";
			Random rnd = new Random();
			StringBuilder sb = new StringBuilder(4);
			for( int i = 0; i < 4; i++ )
				sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
			nickname = sb.toString();
			sharedPreferences.edit().putString("nickname", nickname).apply();
		}
		return nickname;
	}

	public static void verify(Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
		int upgrade = sharedPreferences.getInt("upgrade", 0);
		int ban = sharedPreferences.getInt("ban", 0);
		if (upgrade != 0) { // if just upgraded after being blocked, this should not happen
			Alert.forceUpgrade(activity, upgrade);
		} else if (ban != 0) {
			Alert.ban(activity, ban);
		}
	}



	// Check if user is root

	public static void checkRoot(Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"yonder.android", Context.MODE_PRIVATE);
		long lastRootCheck = sharedPreferences.getLong("last_root", 0);
		long now = System.currentTimeMillis();
		if (lastRootCheck == 0 || (now - lastRootCheck) / 3600000 > 24) {
			Crashlytics.log(Log.INFO, "Log.User", "Checking if device is rooted");
			try {
				if (checkRootMethod1() || checkRootMethod2() || checkRootMethod3()) {
					Crashlytics.logException(new Exception("Rooted Device"));
					Toast.makeText(activity, "Yonder is not supported on rooted devices for now", Toast.LENGTH_LONG).show();
					activity.finish();
					sharedPreferences.edit().putBoolean("root", true).apply();
				} else {
					Crashlytics.log(Log.INFO, "Log.User", "Device is not rooted");
					sharedPreferences.edit().putBoolean("root", false).apply();
				}
				sharedPreferences.edit().putLong("last_root", now).apply();
				return;
			} catch (Exception e) {
				e.printStackTrace();
				Crashlytics.logException(e);;
			}
		}
		if (sharedPreferences.getBoolean("root", false)) {
			Crashlytics.log(Log.INFO, "Log.User", "Exiting since device is rooted");
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
