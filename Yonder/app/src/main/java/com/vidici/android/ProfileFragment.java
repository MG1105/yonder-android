package com.vidici.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ProfileFragment extends Fragment {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_profile, container, false);
		mActivity = getActivity();


		ProgressBar progressProfilePicture = (ProgressBar) view.findViewById(R.id.progress_profile);
		ImageView profilePicture = (ImageView) view.findViewById(R.id.imageView_profile);
		File thumbnailFile = new File(Video.loadedDir.getPath()+"/.jpg");
		if (thumbnailFile.exists()) {
			progressProfilePicture.setVisibility(View.INVISIBLE);
			profilePicture.setImageURI(Uri.fromFile(thumbnailFile));
		}

		Fragment feedFragment = new FeedFragment();
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.add(R.id.profile_feed_fragment, feedFragment).commit();

		// Inflate the layout for this fragment
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
	}
}