package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ProfileFragment extends Fragment {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;
	String profileId;
	ProgressBar progressProfilePicture;
	ImageView profilePicture;
	File profilePictureFile;
	TextView firstName, userName, followers, following, goldReceived, tokens, reputation;
	FrameLayout followFrame;
	int followed;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_profile, container, false);
		mActivity = getActivity();
		profileId = getArguments().getString("user_id");

		progressProfilePicture = (ProgressBar) view.findViewById(R.id.progress_profile);
		profilePicture = (ImageView) view.findViewById(R.id.imageView_profile);
		firstName = (TextView) view.findViewById(R.id.textView_profile_firstname);
		userName = (TextView) view.findViewById(R.id.textView_profile_username);
		followers = (TextView) view.findViewById(R.id.textView_followers);
		following = (TextView) view.findViewById(R.id.textView_following);
		goldReceived = (TextView) view.findViewById(R.id.textView_gold_received);
		tokens = (TextView) view.findViewById(R.id.textView_tokens);
		reputation = (TextView) view.findViewById(R.id.textView_reputation);
		followFrame = (FrameLayout) view.findViewById(R.id.frame_follow);


		followFrame.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!User.loggedIn(mActivity)) {
					return;
				}
				followFrame.setEnabled(false);
				SetFollowTask setFollowTask = new SetFollowTask();
				setFollowTask.execute(User.getId(mActivity), profileId);
			}
		});


		profilePictureFile = new File(Video.loadedDir.getPath()+"/" + profileId + ".jpg");
		if (profilePictureFile.exists()) {
			Resources res = getResources();
			Bitmap src = BitmapFactory.decodeFile(profilePictureFile.getPath());
			RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(res, src);
//			dr.setCornerRadius(Math.max(src.getWidth(), src.getHeight()) / 2.0f);
			dr.setCircular(true);
			profilePicture.setImageDrawable(dr);
//			profilePicture.setImageURI(Uri.fromFile(new File(profilePictureFile.getPath())));
			progressProfilePicture.setVisibility(View.INVISIBLE);
		} else {
			progressProfilePicture.setVisibility(View.VISIBLE);
			DownloadProfilePicture downloadTask = new DownloadProfilePicture();
			downloadTask.setOutputId(profileId+".jpg");
			downloadTask.execute("https://graph.facebook.com/" +profileId+ "/picture?type=large");
		}

		GetProfileTask getProfileTask = new GetProfileTask();
		getProfileTask.execute();

		Fragment feedFragment = new FeedFragment();
		Bundle bundle = new Bundle(2);
		bundle.putBoolean("home", false);
		bundle.putString("user_id", profileId);
		feedFragment.setArguments(bundle);
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.profile_feed_fragment, feedFragment).commit();

		// Inflate the layout for this fragment
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	class GetProfileTask extends AsyncTask<Void, Void, JSONObject> {

		protected JSONObject doInBackground(Void... params) {
			try {
				AppEngine gae = new AppEngine();
				Logger.log(Log.INFO, TAG, "Getting profile for user " + profileId);
				JSONObject response = gae.getProfile(profileId, User.getId(mActivity));
				return response;
			} catch (Exception e) {
				Logger.log(e);;
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						JSONObject profileObject = response.getJSONObject("profile");
						userName.setText("@" + profileObject.getString("username"));
						firstName.setText(profileObject.getString("first_name"));
						following.setText(profileObject.getString("following"));
						followers.setText(profileObject.getString("followers"));
						goldReceived.setText("x " + profileObject.getString("gold_received"));
						tokens.setText(profileObject.getString("tokens"));
						reputation.setText(profileObject.getString("score"));
						followed = profileObject.getInt("followed");

						if (followed == 1) {
							ImageView imageFollow = (ImageView) mActivity.findViewById(R.id.image_follow);
							ImageView imageCircleFollow = (ImageView) mActivity.findViewById(R.id.image_circle_follow);
							imageCircleFollow.setBackgroundResource(R.drawable.oval_green);
							imageFollow.setBackgroundResource(R.drawable.ic_followed);
						}

						if (profileId.equals(User.getId(mActivity))) {
							followFrame.setVisibility(View.INVISIBLE);
						}

					} else {
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}

	class DownloadProfilePicture extends DownloadTask {
		DownloadProfilePicture () {
			super(mActivity);
		}

		@Override
		protected void onPostExecute(Integer error) {
			super.onPostExecute(error);
			if (profilePictureFile.exists()) {
				progressProfilePicture.setVisibility(View.INVISIBLE);
				Resources res = getResources();
				Bitmap src = BitmapFactory.decodeFile(profilePictureFile.getPath());
				RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(res, src);
				dr.setCircular(true);
				profilePicture.setImageDrawable(dr);
			} else {
				progressProfilePicture.setVisibility(View.INVISIBLE);
			}
		}
	}

	class SetFollowTask extends AsyncTask<String, Void, JSONObject> {

		int follow;

		SetFollowTask() {
			if (followed == 0) {
				follow = 1;
			} else {
				follow = 0;
			}
		}

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, "Following " + params[1]);
			JSONObject response = gae.setFollow(params[0], params[1],  follow);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						if (follow == 1) {
							Toast.makeText(mActivity, "Followed", Toast.LENGTH_LONG).show();
							ImageView imageFollow = (ImageView) mActivity.findViewById(R.id.image_follow);
							ImageView imageCircleFollow = (ImageView) mActivity.findViewById(R.id.image_circle_follow);
							imageCircleFollow.setBackgroundResource(R.drawable.oval_green);
							imageFollow.setBackgroundResource(R.drawable.ic_followed);
							followed = 1;
						} else {
							Toast.makeText(mActivity, "Unfollowed", Toast.LENGTH_LONG).show();
							ImageView imageFollow = (ImageView) mActivity.findViewById(R.id.image_follow);
							ImageView imageCircleFollow = (ImageView) mActivity.findViewById(R.id.image_circle_follow);
							imageCircleFollow.setBackgroundResource(R.drawable.oval_blue);
							imageFollow.setBackgroundResource(R.drawable.ic_follow);
							followed = 0;
						}
					} else {
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Logger.log(e);
				Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
			}
			followFrame.setEnabled(true);
		}
	}

}