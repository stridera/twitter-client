package com.stridera.apps.twitterClient;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;

import com.codepath.oauth.OAuthLoginActionBarActivity;
import com.stridera.apps.twitterApp.Activities.HomeTimelineActivity;

public class LoginActivity extends OAuthLoginActionBarActivity<TwitterClient> {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

        ImageView ivBird = (ImageView) findViewById(R.id.ivFlutterBird);
        BitmapDrawable frame1 = (BitmapDrawable) getResources().getDrawable(R.mipmap.twitterbird1);
        BitmapDrawable frame2 = (BitmapDrawable) getResources().getDrawable(R.mipmap.twitterbird2);
        BitmapDrawable frame3 = (BitmapDrawable) getResources().getDrawable(R.mipmap.twitterbird3);
        BitmapDrawable frame4 = (BitmapDrawable) getResources().getDrawable(R.mipmap.twitterbird4);

        AnimationDrawable animationDrawable = new AnimationDrawable();
        animationDrawable.addFrame(frame1, 200);
        animationDrawable.addFrame(frame2, 200);
        animationDrawable.addFrame(frame3, 200);
        animationDrawable.addFrame(frame4, 200);
        animationDrawable.setOneShot(false);
        ivBird.setBackground(animationDrawable);
        animationDrawable.start();

	}


	// Inflate the menu; this adds items to the action bar if it is present.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	// OAuth authenticated successfully, launch primary authenticated activity
	// i.e Display application "homepage"
	@Override
	public void onLoginSuccess() {
        Intent intent = new Intent(this, HomeTimelineActivity.class);
        startActivity(intent);
	}

	// OAuth authentication flow failed, handle the error
	// i.e Display an error dialog or toast
	@Override
	public void onLoginFailure(Exception e) {
		e.printStackTrace();
	}

	// Click handler method for the button used to start OAuth flow
	// Uses the client to initiate OAuth authorization
	// This should be tied to a button used to login
	public void loginToRest(View view) {
		getClient().connect();
	}

}
