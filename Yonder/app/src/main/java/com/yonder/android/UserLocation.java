package com.yonder.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


public class UserLocation implements
		GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	/**
	 * Provides the entry point to Google Play services.
	 */
	protected GoogleApiClient mGoogleApiClient;

	protected android.location.Location mLastLocation;
	SharedPreferences sharedPreferences;

	/**
	 * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
	 */
	protected synchronized void update(Context context) {
		mGoogleApiClient = new GoogleApiClient.Builder(context)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
		sharedPreferences = context.getSharedPreferences(
				"com.yonder.android", Context.MODE_PRIVATE);
		mGoogleApiClient.connect();
	}

	/**
	 * Runs when a GoogleApiClient object successfully connects.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		// Provides a simple way of getting a device's location and is well suited for
		// applications that do not require a fine-grained location and that do not need location
		// updates. Gets the best and most recent location currently available, which may be null
		// in rare cases when a location is not available.
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (mLastLocation != null) {
			String lat = String.valueOf(mLastLocation.getLatitude());
			String lon = String.valueOf(mLastLocation.getLongitude());
			sharedPreferences.edit().putString("location_lon", lon).apply();
			sharedPreferences.edit().putString("location_lat", lat).apply();
		} else {
			//No location detected
		}
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Refer to the javadoc for ConnectionResult to see what error codes might be returned in
		// onConnectionFailed.

		//Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}


	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We call connect() to
		// attempt to re-establish the connection.

		//Log.i(TAG, "Connection suspended");
		mGoogleApiClient.connect();
	}

}
