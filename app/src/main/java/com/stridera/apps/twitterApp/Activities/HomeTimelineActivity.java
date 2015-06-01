package com.stridera.apps.twitterApp.Activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.stridera.apps.twitterApp.Adapters.FragmentPagerAdapter;
import com.stridera.apps.twitterApp.Fragments.WriteTweetDialog;
import com.stridera.apps.twitterClient.R;
import com.stridera.apps.twitterClient.TwitterApplication;
import com.stridera.apps.twitterClient.TwitterClient;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;


public class HomeTimelineActivity extends ActionBarActivity implements WriteTweetDialog.OnTweetSentListener {
    FragmentPagerAdapter adapterViewPager;
    FragmentManager fm;

    long user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_timeline);

        getSupportActionBar().setIcon(R.drawable.ic_title_icon);
        updateTitle();

        ViewPager vpPager = (ViewPager) findViewById(R.id.vpPager);
        adapterViewPager = new FragmentPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);

        PagerSlidingTabStrip tabsStrip = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabsStrip.setViewPager(vpPager);
    }

    private void updateTitle() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String screen_name = prefs.getString("screen_name", "");
        setTitle("@" + screen_name);

        if (screen_name.isEmpty()) {
            TwitterClient client = TwitterApplication.getRestClient();
            client.getCurrentUser(new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    SharedPreferences.Editor edit = prefs.edit();
                    try {
                        String screen_name = response.getString("screen_name");
                        setTitle("@" + screen_name);

                        user_id = response.getLong("id");
                        edit.putLong("user_id", user_id);
                        edit.putString("screen_name", screen_name);
                        edit.putString("name", response.getString("name"));
                        edit.putString("location", response.getString("location"));
                    } catch (JSONException e) {
                        Log.d("blah", "Unable to get user information.");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse != null)
                        try {
                            String message = errorResponse.getJSONArray("errors").getJSONObject(0).getString("message");
                            Toast.makeText(HomeTimelineActivity.this, "Fetch Failed: " + message, Toast.LENGTH_LONG).show();
                        } catch (JSONException ex) {
                            Log.d("blah", errorResponse.toString());
                        }
                    else
                        Log.d("blah", "Failed with status " + statusCode);
                }
            });
        } else {
            user_id = prefs.getLong("user_id", 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_timeline, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_write_tweet) {
            writeTweet();
            return true;
        } else if (id == R.id.action_view_profile) {
            showProfile(user_id);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProfile(long user_id) {
        Intent intent = new Intent(HomeTimelineActivity.this, ViewProfileActivity.class);
        intent.putExtra("user_id", user_id);
        startActivity(intent);
    }

    public void writeTweet() {
        writeTweet(0);
    }

    public void writeTweet(long replyTo) {
        fm = getFragmentManager();
        WriteTweetDialog dialog = new WriteTweetDialog();
        Bundle args = new Bundle();
        args.putLong("reply_to", replyTo);
        dialog.setArguments(args);
        dialog.show(fm, "write_tweet_dialog");
    }

    @Override
    public void onTweetSent() {
       // loadTweets(ITEMS_NEWER);
    }
}
