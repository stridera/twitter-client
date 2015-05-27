package com.stridera.apps.twitterApp;

import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.stridera.apps.twitterApp.Adapters.TweetsArrayAdapter;
import com.stridera.apps.twitterApp.Listeners.EndlessScrollListener;
import com.stridera.apps.twitterClient.R;
import com.stridera.apps.twitterClient.TwitterApplication;
import com.stridera.apps.twitterClient.TwitterClient;
import com.stridera.apps.twitterClient.models.Tweet;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class HomeTimelineActivity extends ActionBarActivity implements WriteTweetDialog.OnTweetSentListener {
    static final private int INITIAL = 0;
    static final private int ITEMS_NEWER = 1;
    static final private int ITEMS_OLDER = 2;

    private SwipeRefreshLayout swipeContainer;
    private TwitterClient client;
    private TweetsArrayAdapter adapter;

    private ImageView ivProgress;

    private long lvMaxId = 0;
    private long lvMinId = 0;

    FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_timeline);

        getSupportActionBar().setIcon(R.drawable.ic_title_icon);
        updateTitle();

        ivProgress = (ImageView) findViewById(R.id.ivProgress);

        client = TwitterApplication.getRestClient();

        ArrayList<Tweet> tweets = new ArrayList<>();
        ListView lvTweets = (ListView) findViewById(R.id.lvTweets);
        lvTweets.setOnScrollListener(onEndlessScrollListener);
        adapter = new TweetsArrayAdapter(this, tweets);
        lvTweets.setAdapter(adapter);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(refreshListener);
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        loadTweets();
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

                        edit.putLong("user_id", response.getLong("id"));
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
        }
    }

    private EndlessScrollListener onEndlessScrollListener = new EndlessScrollListener() {
        @Override
        public void onLoadMore(int page, int totalItemsCount) {
            loadTweets(ITEMS_OLDER);
            Toast.makeText(HomeTimelineActivity.this, "Page " + page, Toast.LENGTH_SHORT).show();
        }
    };

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (isNetworkAvailable())
                loadTweets(ITEMS_NEWER);
            else
                Toast.makeText(HomeTimelineActivity.this,
                        "Network not available.  Please connect and try again.",
                        Toast.LENGTH_SHORT)
                        .show();
        }
    };

    private void loadTweets() {
        startProgress();
        loadTweets(INITIAL);
    }

    private void loadTweets(final int type) {
        long max_id = 0;
        long since_id = 0;

        if (!isNetworkAvailable()) {
            handleOffline(type);
        }

        if (type == ITEMS_NEWER)
            since_id = lvMaxId;
        else if (type == ITEMS_OLDER)
            max_id = lvMinId;

        client.getHomeTimeline(max_id, since_id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                JSONObject tweetJSON;
                long max_id = 0, min_id = 0;

                // Load data in database
                ActiveAndroid.beginTransaction();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        tweetJSON = response.getJSONObject(i);
                        long id = tweetJSON.getLong("id");

                        if (i == 0)
                            min_id = id;
                        if (id > max_id)
                            max_id = id;
                        else if (id < min_id)
                            min_id = id;

                        Tweet t = Tweet.getById(id);
                        if (t == null) {
                            t = new Tweet(tweetJSON);
                            t.save();
                        }
                    }
                    ActiveAndroid.setTransactionSuccessful();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                    Log.e("blah", ex.toString());
                } finally {
                    ActiveAndroid.endTransaction();
                }

                if (type == INITIAL) {
                    adapter.clear();
                    lvMaxId = max_id;
                    lvMinId = min_id;
                }

                if (type == ITEMS_NEWER) {
                    for (Tweet t : Tweet.itemsInRange(min_id, max_id, "ASC")) {
                        adapter.insert(t, 0);
                    }
                    lvMaxId = max_id;
                } else {
                    for (Tweet t : Tweet.itemsInRange(min_id, max_id, "DESC")) {
                        adapter.add(t);
                    }
                    lvMinId = min_id;
                }
                stopProgress();
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
    }

    private void handleOffline(int type) {
        if (type == INITIAL) {
            for (Tweet t : Tweet.recentItems()) {
                long id = t.getTweet_id();
                if (lvMinId == 0 || lvMinId < id)
                    lvMinId = id;

                if (lvMaxId < id)
                    lvMaxId = id;

                adapter.add(t);
            }
        } else if (type == ITEMS_OLDER) {
            for (Tweet t : Tweet.itemsAfterId(lvMinId)) {
                long id = t.getTweet_id();
                if (lvMinId == 0 || lvMinId < id)
                    lvMinId = id;

                adapter.add(t);
            }
        }
        stopProgress();
        if (adapter.getCount() > 0)
            Toast.makeText(HomeTimelineActivity.this,
                    "Internet not available, showing stored items.", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Internet is not available.  Please connect and try again.",
                    Toast.LENGTH_SHORT).show();
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
        }

        return super.onOptionsItemSelected(item);
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
        Log.d("blah", "It's been called!");
        loadTweets(ITEMS_NEWER);
    }

    private void startProgress() {
        BitmapDrawable frame1 = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_twitter_bird_1);
        BitmapDrawable frame2 = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_twitter_bird_2);
        BitmapDrawable frame3 = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_twitter_bird_3);
        BitmapDrawable frame4 = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_twitter_bird_4);

        AnimationDrawable animationDrawable = new AnimationDrawable();
        animationDrawable.addFrame(frame1, 200);
        animationDrawable.addFrame(frame2, 200);
        animationDrawable.addFrame(frame3, 200);
        animationDrawable.addFrame(frame4, 200);
        animationDrawable.setOneShot(false);
        ivProgress.setBackground(animationDrawable);
        animationDrawable.start();
    }

    private void stopProgress() {
        swipeContainer.setRefreshing(false);
        ivProgress.setVisibility(View.GONE);
    }

    public Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
