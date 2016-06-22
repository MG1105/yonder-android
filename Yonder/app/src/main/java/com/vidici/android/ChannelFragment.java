package com.vidici.android;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class ChannelFragment extends Fragment {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;
	private ProgressBar progressBar;
	ArrayList<Channel> channels;
	ChannelAdapter channelAdapter;
	static HashMap<String, LinkedHashMap<String, JSONObject>> channelInfo = new HashMap<>();
	String channelSort;
	FloatingActionButton addChannel;
	View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_channel, container, false);
		mActivity = getActivity();
		channelSort = getArguments().getString("channelSort");

		addChannel = (FloatingActionButton)view.findViewById(R.id.button_add_channel);
		addChannel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
						.setPositiveButton(android.R.string.ok, null)
						.create();
				alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				alertDialog.setMessage("Create a new hashtag");

				final EditText input = new EditText(mActivity);
				input.setHint("#howhighcanyourankit");
				alertDialog.setView(input);
				Logger.trackEvent(mActivity, "Channel", "Click Add");
				Logger.log(Log.INFO, TAG, "Open add channel dialog");

				alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								if (!User.loggedIn(mActivity)) {
									return;
								}
								String name = input.getText().toString().trim();
								if (isValidChannelName(name)) {
									alertDialog.dismiss();
									Toast.makeText(mActivity, "Creating " + name + "...", Toast.LENGTH_LONG).show();
									AddChannelTask addChannelTask = new AddChannelTask();
									addChannelTask.execute(User.getId(mActivity), name.substring(1));
								}
							}
						});
					}
				});
				alertDialog.show();
			}
		});

		GetChannelsTask getChannelsTask = new GetChannelsTask();
		getChannelsTask.execute(User.getId(mActivity), channelSort);

		return view;
	}

	class GetChannelsTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			try {
				AppEngine gae = new AppEngine();
				JSONObject response = gae.getChannels(params[0], params[1]);
				return response;
			} catch (Exception e) {
				Logger.log(e);
				return null;
			}
		}

		protected void onPostExecute(JSONObject response) {
			try {
				TextView noChannels = (TextView)view.findViewById(R.id.textView_no_channels);
				ImageView noChannelsImage = (ImageView)view.findViewById(R.id.image_no_channels);
				if (response != null) {
					if (response.getString("success").equals("1")) {

						JSONArray channelsArray = response.getJSONArray("channels");
						channels = Channel.fromJson(channelsArray);
						ListView listView = (ListView) view.findViewById(R.id.listView_channels);
						if (channels.size() > 0) {
							// Create the adapter to convert the array to views
							channelAdapter = new ChannelAdapter(mActivity);
							// Attach the adapter to a ListView
							listView.setAdapter(channelAdapter);
							noChannels.setVisibility(View.GONE);
							noChannelsImage.setVisibility(View.GONE);
							listView.setVisibility(View.VISIBLE);
						} else {
							noChannels.setVisibility(View.VISIBLE);
							noChannelsImage.setVisibility(View.VISIBLE);
							listView.setVisibility(View.GONE);
						}
					} else {
						Logger.log(new Exception("Server Side Failure"));
						noChannels.setVisibility(View.VISIBLE);
						noChannelsImage.setVisibility(View.VISIBLE);
					}
				} else { // no internet
					noChannels.setVisibility(View.VISIBLE);
					noChannelsImage.setVisibility(View.VISIBLE);
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
				progressBar = (ProgressBar)view.findViewById(R.id.progress_channels);
				progressBar.setVisibility(View.GONE);
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}

	class ChannelAdapter extends ArrayAdapter<Channel> {
		Channel channel;
		TextView rating, ranking;
		TextView unseen;
		TextView name;
		Button likeButton;
		Button dislikeButton;
		ProgressBar progressLoading;

		public ChannelAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, channels);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			channel = getItem(position);

			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_channel, parent, false);
			}

			likeButton = (Button) convertView.findViewById(R.id.button_channel_like);
			dislikeButton = (Button) convertView.findViewById(R.id.button_channel_dislike);
			rating = (TextView) convertView.findViewById(R.id.textView_channel_item_rating);
			unseen = (TextView) convertView.findViewById(R.id.textView_channel_new);
			name = (TextView) convertView.findViewById(R.id.textView_channel_name);
			ranking = (TextView) convertView.findViewById(R.id.textView_channel_item_ranking);
			progressLoading = (ProgressBar) convertView.findViewById(R.id.progress_channel_loading);

			if (channel.isPlayable()) {
				unseen.setText("");
				progressLoading.setVisibility(View.INVISIBLE);
			} else if (channel.isDownloading()) {
				progressLoading.setVisibility(View.VISIBLE);
			} else if (channel.isVideosEmpty()) {
				progressLoading.setVisibility(View.INVISIBLE);
			} else if (channel.isEmpty()) {
				progressLoading.setVisibility(View.INVISIBLE);
			}

			if (channel.getRated() == 1) {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(false);
				likeButton.setBackgroundResource(R.drawable.ic_up_dark);
				dislikeButton.setBackgroundResource(R.drawable.ic_down);
			} else if (channel.getRated() == -1) {
				dislikeButton.setEnabled(false);
				likeButton.setEnabled(true);
				dislikeButton.setBackgroundResource(R.drawable.ic_down_dark);
				likeButton.setBackgroundResource(R.drawable.ic_up);
			} else {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(true);
				likeButton.setBackgroundResource(R.drawable.ic_up);
				dislikeButton.setBackgroundResource(R.drawable.ic_down);
			}

			if (User.admin) {
				dislikeButton.setEnabled(true);
				likeButton.setEnabled(true);
			}

			unseen.setText(channel.getUnseen());
			name.setText("#" + channel.getName());
			ranking.setText(""+ (position+1));
			rating.setText(channel.getRating());
//            total.setText(channel.getCount()+ " reactions");

			convertView.setOnClickListener(new View.OnClickListener() {
				Channel myChannel = channel;
				ProgressBar myProgressLoading = progressLoading;


				@Override
				public void onClick(View v) {
					if (myChannel.isPlayable() && !GetVideosTask.loading) {
						Intent intentFeedStart = new Intent(mActivity, StoryActivity.class);
						intentFeedStart.putExtra("channelId", myChannel.getId());
						startActivity(intentFeedStart);
					} else if (!GetVideosTask.loading) {
						GetVideosTask.loading = true;
						myChannel.setDownloading(true);
						Logger.trackEvent(mActivity, "Channel", "Load Request");
						myProgressLoading.setVisibility(View.VISIBLE);
						Logger.log(Log.INFO, TAG, "Loading channel " + myChannel.getName());
						GetVideosTask getFeed = new GetVideosTask(mActivity, myChannel, myChannel.getId(), channelAdapter, channelInfo, channelSort);
						getFeed.execute();
					}
				}
			});

			convertView.setOnLongClickListener(new View.OnLongClickListener() {
				String myChannelId = channel.getId();
				String myChannelName = channel.getName();

				@Override
				public boolean onLongClick(View v) {
					Toast.makeText(mActivity, "Opening your camera...", Toast.LENGTH_LONG).show();
					Intent intent = new Intent(getActivity(),CameraPreviewActivity.class);
					intent.putExtra("channelId", myChannelId);
					intent.putExtra("channelName", myChannelName);
					startActivity(intent);
					Logger.trackEvent(mActivity, "Channel", "Reply");
					return true;
				}
			});

			likeButton.setOnClickListener(new View.OnClickListener() {
				Channel myChannel = channel;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				@Override
				public void onClick(View v) {
					if (!User.loggedIn(mActivity)) {
						return;
					}
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateChannelTask rateChannelTask = new RateChannelTask(myChannel, myLike, myDislike);
					rateChannelTask.execute("1", User.getId(mActivity));
					myChannel.setRating(1);
					Logger.log(Log.INFO, TAG, "Liking channel");
					Logger.trackEvent(mActivity, "Channel", "Like Channel");
				}
			});

			dislikeButton.setOnClickListener(new View.OnClickListener() {
				Channel myChannel = channel;
				Button myLike = likeButton;
				Button myDislike = dislikeButton;
				@Override
				public void onClick(View v) {
					if (!User.loggedIn(mActivity)) {
						return;
					}
					myDislike.setEnabled(false);
					myLike.setEnabled(false);
					RateChannelTask rateChannelTask = new RateChannelTask(myChannel, myLike, myDislike);
					rateChannelTask.execute("-1", User.getId(mActivity));
					myChannel.setRating(-1);
					Logger.log(Log.INFO, TAG, "Disliking channel");
					Logger.trackEvent(mActivity, "Channel", "Dislike Channel");
				}
			});

			// Return the completed view to render on screen
			return convertView;

		}

	}

	class RateChannelTask extends AsyncTask<String, Void, JSONObject> {
		Channel myChannel;
		Button myLike;
		Button myDislike;

		protected RateChannelTask(Channel channel, Button like, Button dislike) {
			myChannel = channel;
			myLike = like;
			myDislike = dislike;
		}

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			Logger.log(Log.INFO, TAG, "Rating channel " + params[0]+ " " + params[1]);
			JSONObject response = gae.rateChannel(myChannel.getId(), params[0], params[1]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						myChannel.updateRating();
						// myLike chane to bold
						channelAdapter.notifyDataSetChanged();
					} else {
						myDislike.setEnabled(true);
						myLike.setEnabled(true);
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
					}
				} else {
					myDislike.setEnabled(true);
					myLike.setEnabled(true);
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Logger.log(e);
				Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				myDislike.setEnabled(true);
				myLike.setEnabled(true);
			}
		}
	}

	class AddChannelTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			JSONObject response = gae.addChannel(params[0], params[1]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
//                        actionBar.selectTab(actionBar.getTabAt(1));
//                        GetChannelsTask getChannelsTask = new GetChannelsTask();
//                        getChannelsTask.execute(userId, "new");
						Logger.trackEvent(mActivity, "Channel", "Added Channel");
					} else {
						Logger.log(new Exception("Server Side Failure"));
						Toast.makeText(mActivity, "Failed to add channel", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(mActivity, "Please check your connectivity and try again later", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}

	protected Boolean isValidChannelName(String name) {
		String pattern= "^[a-zA-Z0-9_]*$";
		if (!name.startsWith("#")) {
			Toast.makeText(mActivity, "Must start with #", Toast.LENGTH_LONG).show();
			return false;
		} else if (name.contains(" ")) {
			Toast.makeText(mActivity, "Cannot contain spaces", Toast.LENGTH_LONG).show();
			return false;
		} else if (!name.substring(1).matches(pattern)) {
			Toast.makeText(mActivity, "Cannot contain special characters", Toast.LENGTH_LONG).show();
			return false;
		} else if (name.substring(1).length() > 32) {
			Toast.makeText(mActivity, "Cannot be longer than 32 characters", Toast.LENGTH_LONG).show();
			return false;
		} else {
			return true;
		}
	}

}
