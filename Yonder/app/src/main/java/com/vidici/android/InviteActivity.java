package com.vidici.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

public class InviteActivity extends Activity {

	private final String TAG = "Log." + this.getClass().getSimpleName();
	Activity mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.log(Log.INFO, TAG, "Creating Activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_invite);
		mActivity = this;
	}

	public void onResume() {
		super.onResume();
		Logger.log(Log.INFO, TAG, "Resuming Activity");
		Logger.fbActivate(mActivity, true);
		Logger.trackEvent(mActivity, "Invite", "View");

		Button invite = (Button)findViewById(R.id.invite_button);
		invite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
//				Intent sendIntent = new Intent();
//				sendIntent.setAction(Intent.ACTION_SEND);
//				sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out Vidici! It's invite only but this code will get you in \"V44L\"\n\nhttps://play.google.com/store/apps/details?id=com.vidici.android&referrer=utm_source%3Dsharecode");
//				sendIntent.setType("text/plain");
//				startActivity(Intent.createChooser(sendIntent, "SHARE CODE"));
//				Logger.trackEvent(mActivity, "Invite", "Share");

				if (AppInviteDialog.canShow()) {
					String appLinkUrl = "https://fb.me/1594792200819208";
					String previewImageUrl = "http://storage.googleapis.com/yander/fb_invite.jpg";
					AppInviteContent content = new AppInviteContent.Builder()
							.setApplinkUrl(appLinkUrl)
							.setPreviewImageUrl(previewImageUrl)
							.setPromotionDetails("Invite Only Access", User.getId(mActivity).substring(0,10))
							.build();
					AppInviteDialog.show(mActivity, content);
					Logger.trackEvent(mActivity, "Invite", "Fb Invite");
				}
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.log(Log.INFO, TAG, "Pausing Activity");
		Logger.fbActivate(this, false);
	}
}
