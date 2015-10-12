package yonder.android;


import com.google.android.gms.analytics.Tracker;
import android.app.Application;
import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class YondorApplication extends Application {
	private Tracker mTracker;

	/**
	 * Gets the default {@link Tracker} for this {@link Application}.
	 * @return tracker
	 */
	synchronized public Tracker getDefaultTracker() {
		if (mTracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			// Set the dispatch period in seconds.
			analytics.setLocalDispatchPeriod(15);
			if (User.admin) {
				analytics.setDryRun(true);
			}
			// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
			mTracker = analytics.newTracker(R.xml.app_tracker);
			// Enable Advertising Features.
			mTracker.enableAdvertisingIdCollection(true);
		}
		return mTracker;
	}
}