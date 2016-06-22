package com.vidici.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class LoginFragment extends Fragment {
	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;
	LoginButton loginButton;
	CallbackManager callbackManager;
	SharedPreferences sharedPreferences;
	EditText usernameText;
	Button usernameButton;
	RelativeLayout loginLayout;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login, container, false);
		mActivity = getActivity();
		sharedPreferences = mActivity.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
		usernameText = (EditText) view.findViewById(R.id.fragment_login_textview_username);
		usernameButton = (Button) view.findViewById(R.id.fragment_login_button_username);

		callbackManager = CallbackManager.Factory.create();
		loginButton = (LoginButton) view.findViewById(R.id.login_button);
		loginLayout = (RelativeLayout) view.findViewById(R.id.relativeLayout_login);
		loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));
		// If using in a fragment
		loginButton.setFragment(this);

		if (sharedPreferences.getString("facebook_id", "").length() != 0) {
			loginLayout.setVisibility(View.INVISIBLE);
			usernameButton.setVisibility(View.VISIBLE);
			usernameText.setVisibility(View.VISIBLE);
		}

		// Callback registration
		loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
			@Override
			public void onSuccess(LoginResult loginResult) {

				GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

					@Override
					public void onCompleted(JSONObject object, GraphResponse response) {
						try {
							SharedPreferences sharedPreferences = mActivity.getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
							sharedPreferences.edit().putString("facebook_id", object.getString("id")).apply();
							if (object.has("first_name"))
								sharedPreferences.edit().putString("first_name", object.getString("first_name")).apply();
							if (object.has("last_name"))
								sharedPreferences.edit().putString("last_name", object.getString("last_name")).apply();
							if (object.has("email"))
								sharedPreferences.edit().putString("email", object.getString("email")).apply();
						} catch (JSONException e) {
							e.printStackTrace();
						}

					}
				});
				Bundle parameters = new Bundle();
				parameters.putString("fields", "id, first_name, last_name, email");
				request.setParameters(parameters);
				request.executeAsync();

				Logger.trackEvent(mActivity, "Login", "Choose Nickname");
				Logger.log(Log.INFO, TAG, "Choosing nickname");
				loginLayout.setVisibility(View.INVISIBLE);
				usernameButton.setVisibility(View.VISIBLE);
				usernameText.setVisibility(View.VISIBLE);
			}

			@Override
			public void onCancel() {
				System.out.println("onCancel");
			}

			@Override
			public void onError(FacebookException exception) {
				System.out.println("onError");
			}
		});

		usernameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String name = usernameText.getText().toString().trim();
				if (sharedPreferences.getString("facebook_id", "").length() == 0) {
					Toast.makeText(mActivity, "Facebook has not approved your account yet. Please wait a few seconds and try again...", Toast.LENGTH_LONG).show();
				} else if (isValidNickname(name)) {
					name = name.toLowerCase();
					sharedPreferences.edit().putString("username", name).apply();
					Logger.trackEvent(mActivity, "Login", " Completed");
					AddProfileTask addProfileTask = new AddProfileTask();
					addProfileTask.execute(User.getAndroidId(mActivity), sharedPreferences.getString("facebook_id", ""), sharedPreferences.getString("first_name", ""),
							sharedPreferences.getString("last_name", ""), sharedPreferences.getString("email", ""), sharedPreferences.getString("username", ""));
				}
			}
		});

		// Inflate the layout for this fragment
		return view;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	protected Boolean isValidNickname(String name) {
		String pattern= "^[a-zA-Z0-9_]*$";
		if (name.contains(" ")) {
			Toast.makeText(mActivity, "Cannot contain spaces", Toast.LENGTH_LONG).show();
			return false;
		} else if (!name.matches(pattern)) {
			Toast.makeText(mActivity, "Cannot contain special characters", Toast.LENGTH_LONG).show();
			return false;
		} else if (name.length() > 16) {
			Toast.makeText(mActivity, "Cannot be longer than 16 characters", Toast.LENGTH_LONG).show();
			return false;
		} else {
			return true;
		}
	}

	class AddProfileTask extends AsyncTask<String, Void, JSONObject> {

		protected JSONObject doInBackground(String... params) {
			AppEngine gae = new AppEngine();
			JSONObject response = gae.addProfile(params[0], params[1], params[2], params[3], params[4], params[5]);
			return response;
		}

		protected void onPostExecute(JSONObject response) {
			try {
				if (response != null) {
					if (response.getString("success").equals("1")) {
						sharedPreferences.edit().putBoolean("logged_in", true).apply();
						usernameButton.setVisibility(View.INVISIBLE);
						usernameText.setVisibility(View.INVISIBLE);
						ProfileFragment profileFragment = new ProfileFragment();
						Bundle bundle = new Bundle(1);
						bundle.putString("user_id", sharedPreferences.getString("facebook_id", ""));
						profileFragment.setArguments(bundle);
						FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
						transaction.replace(R.id.login_layout, profileFragment).commit();
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

	// Container Activity must implement this interface
	public interface OnLoginCompleteListener {
		void onLoginComplete();
	}

}