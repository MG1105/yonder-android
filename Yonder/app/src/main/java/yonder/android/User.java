package yonder.android;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Random;

public class User {
	static boolean admin = false;
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
			androidId += "::" + (Build.PRODUCT.length() % 10) + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10);
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
		if (upgrade != 0) {
			Alert.forceUpgrade(activity, upgrade);
		} else if (ban != 0) {
			Alert.ban(activity, ban);
		}
		// Add root checking
	}

}
