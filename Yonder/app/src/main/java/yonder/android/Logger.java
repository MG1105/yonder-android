package yonder.android;


import android.app.Activity;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import io.fabric.sdk.android.Fabric;

public class Logger {

	public static void init (Context myContext) {
		if (User.admin) {
			return;
		}
		Fabric.with(myContext, new Crashlytics());
		Crashlytics.setUserIdentifier(User.getId(myContext));
	}

	public static void log (int priority, String tag, String message) {
		if (User.admin) {
			android.util.Log.println(priority, tag, message);
			return;
		}
		Crashlytics.log(priority, tag, message);
	}

	public static void log (Throwable e) {
		if (User.admin) {
			e.printStackTrace();
			return;
		}
		e.printStackTrace();
		Crashlytics.logException(e);
	}

	public static void fbActivate(Context context, boolean on) {
		if (User.admin) {
			return;
		}
		if (on) {
			AppEventsLogger.activateApp(context);
		} else {
			AppEventsLogger.deactivateApp(context);
		}
	}

	// Google Analytics

	public static void startSession(Activity activity) {
		// Obtain the shared Tracker instance.
		YondorApplication application = (YondorApplication) activity.getApplication();
		Tracker mTracker = application.getDefaultTracker();
		// Set screen name.
		mTracker.setScreenName("Splash Activity"); // start session with no screen?
		mTracker.send(new HitBuilders.ScreenViewBuilder()
				.setNewSession()
				.build());
	}

	public static void trackEvent(Activity activity, String category, String action) {
		// Obtain the shared Tracker instance.
		YondorApplication application = (YondorApplication) activity.getApplication();
		Tracker mTracker = application.getDefaultTracker();
		// Build and send an Event.
		mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.build());
	}

	public static void trackEvent(Activity activity, String category, String action, int value) {
		// Obtain the shared Tracker instance.
		YondorApplication application = (YondorApplication) activity.getApplication();
		Tracker mTracker = application.getDefaultTracker();
		// Build and send an Event.
		mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setValue(value)
				.build());
	}

}