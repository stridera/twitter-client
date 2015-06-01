package com.stridera.apps.twitterApp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Picasso;
import com.stridera.apps.twitterApp.Fragments.UserStream;
import com.stridera.apps.twitterClient.R;
import com.stridera.apps.twitterClient.TwitterApplication;
import com.stridera.apps.twitterClient.TwitterClient;
import com.stridera.apps.twitterClient.models.TwitterUser;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

public class ViewProfileActivity extends ActionBarActivity {
    ImageView ivBackgroundImage;
    ImageView ivProfileImage;
    TextView tvFullName;
    TextView tvScreenname;
    TextView tvDescription;
    TextView tvFollowers;
    TextView tvFollowing;
    TextView tvTweetCount;
    FrameLayout flFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        Intent intent = getIntent();
        long user_id = intent.getLongExtra("user_id", 0);

        if (user_id == 0)
            finish();

        TwitterUser twitterUser = TwitterUser.getById(user_id);

        ivBackgroundImage =  (ImageView) findViewById(R.id.ivBackground);
        ivProfileImage = (ImageView) findViewById(R.id.ivProfileImage);
        tvFullName = (TextView) findViewById(R.id.tvFullName);
        tvScreenname = (TextView) findViewById(R.id.tvScreenname);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvFollowers = (TextView) findViewById(R.id.tvFollowersCount);
        tvFollowing = (TextView) findViewById(R.id.tvFollowingCount);
        tvTweetCount = (TextView) findViewById(R.id.tvTweetCount);
        flFrag = (FrameLayout) findViewById(R.id.flFrag);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        UserStream stream = UserStream.newInstance(user_id);
        ft.add(R.id.flFrag, stream);
        ft.commit();

        updateInfo(user_id);
    }

    private void updateInfo(long user_id) {
        TwitterClient client = TwitterApplication.getRestClient();
        client.getUserInfo(user_id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    tvFullName.setText(response.getString("name"));
                    tvScreenname.setText("@" + response.getString("screen_name"));
                    tvDescription.setText(response.getString("description"));
                    tvFollowers.setText(String.format("%d Followers", response.getInt("followers_count")));
                    tvFollowing.setText(String.format("%d Following", response.getInt("friends_count")));
                    tvTweetCount.setText(String.format("%d Tweets", response.getInt("statuses_count")));

                    Picasso.with(ViewProfileActivity.this)
                            .load(response.getString("profile_background_image_url"))
                            .into(ivBackgroundImage);
                    Picasso.with(ViewProfileActivity.this)
                            .load(response.getString("profile_image_url"))
                            .into(ivProfileImage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse != null)
                    try {
                        String message = errorResponse.getJSONArray("errors").getJSONObject(0).getString("message");
                        Toast.makeText(ViewProfileActivity.this, "Fetch Failed: " + message, Toast.LENGTH_LONG).show();
                    } catch (JSONException ex) {
                        Log.d("blah", errorResponse.toString());
                    }
                else
                    Log.d("blah", "Failed with status " + statusCode);
            }
        });
    }
}

/*
{
  "name": "Ryan Sarver",
  "profile_image_url": "http://a0.twimg.com/profile_images/1777569006/image1327396628_normal.png",
  "location": "San Francisco, CA",
  "favourites_count": 3162,
  "profile_use_background_image": true,
  "followers_count": 276334,
  "description": "Director, Platform at Twitter. Detroit and Boston export. Foodie and over-the-hill hockey player. @devon's lesser half",
  "profile_background_image_url": "http://a0.twimg.com/profile_background_images/113854313/xa60e82408188860c483d73444d53e21.png",
  "statuses_count": 13728,
  "friends_count": 1780,
  "screen_name": "rsarver"
}
 */