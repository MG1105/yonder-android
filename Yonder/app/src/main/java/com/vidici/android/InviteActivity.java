package com.vidici.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

public class InviteActivity extends Activity {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;
	Button invite;
	TextView text1, text2;
	SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.log(Log.INFO, TAG, "Creating Activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_invite);
		mActivity = this;
		sharedPreferences = getSharedPreferences("com.vidici.android", Context.MODE_PRIVATE);
		text1 = (TextView) findViewById(R.id.invite_textview_1);
		text2 = (TextView) findViewById(R.id.invite_textview_2);
		invite = (Button)findViewById(R.id.invite_button);

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("gold")) {
			text1.setText("You ran out of free tokens :/");
			text2.setText("You need tokens to give more Awards. Get 5 tokens for each friend you invite to Vidici");
			invite.setText("Get Tokens");
			Logger.trackEvent(mActivity, "Invite", "View Gold");
		} else if (sharedPreferences.getString("college", "").equals("")) {
			String college = sharedPreferences.getString("college_request", "your school");
			text1.setText("We received your petition to add " + college);
			text2.setText("Get more signatures to be approved faster");
			invite.setText("Share Petition");
			Logger.trackEvent(mActivity, "Invite", "View Petition");
		} else {
			String college = sharedPreferences.getString("college", "Your school");
			text1.setText("We are sponsoring this year's Victoria's Secret Fashion Show After-Party");
			text2.setText("To promote Vidici, we are randomly giving away VIP packages to users who invite their friends (airfare and hotel included). Feeling Lucky?");
			// Users from the winnning school who invite their peers
			invite.setText("Invite Friends");
			Logger.trackEvent(mActivity, "Invite", "View Invite");
		}

		invite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
			if (AppInviteDialog.canShow()) {
				String appLinkUrl = "https://fb.me/1594792200819208";
				String previewImageUrl = "https://storage.googleapis.com/yander/fb_invite.png";
				AppInviteContent content = new AppInviteContent.Builder()
						.setApplinkUrl(appLinkUrl)
						.setPreviewImageUrl(previewImageUrl)
						.setPromotionDetails("VIP Access", User.getId(mActivity).substring(0,10))
						.build();
				AppInviteDialog.show(mActivity, content);

				if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("gold")) {
					Logger.trackEvent(mActivity, "Invite", "Gold Click");
				} else if (sharedPreferences.getString("college", "").equals("")) {
					Logger.trackEvent(mActivity, "Invite", "Petition Click");
				} else {
					Logger.trackEvent(mActivity, "Invite", "Invite Click");
				}
			}
			}
		});
	}

	public void onResume() {
		super.onResume();
		Logger.log(Log.INFO, TAG, "Resuming Activity");
		Logger.fbActivate(mActivity, true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		Logger.fbActivate(this, false);
	}
}
