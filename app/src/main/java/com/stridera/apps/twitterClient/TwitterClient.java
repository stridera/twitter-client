package com.stridera.apps.twitterClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;

import com.codepath.oauth.OAuthBaseClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TwitterClient extends OAuthBaseClient {
	public static final Class<? extends Api> REST_API_CLASS = TwitterApi.class;
	public static final String REST_URL = "https://api.twitter.com/1.1/";
	public static final String REST_CONSUMER_KEY = "KEY";
	public static final String REST_CONSUMER_SECRET = "SECRET";
	public static final String REST_CALLBACK_URL = "oauth://stridera.com";

	public TwitterClient(Context context) {
		super(context, REST_API_CLASS, REST_URL, REST_CONSUMER_KEY, REST_CONSUMER_SECRET, REST_CALLBACK_URL);
	}

    /**
     * Get the current User Data
     * @link https://dev.twitter.com/rest/reference/get/account/verify_credentials
     * @param handler
     */
    public void getCurrentUser(AsyncHttpResponseHandler handler) {
        String apiURL = getApiUrl("account/verify_credentials.json");
        client.get(apiURL, handler);
    }

    /**
     * Get the home timeline for the user
     * @link https://dev.twitter.com/rest/reference/get/statuses/home_timeline
     * @param handler
     */
	public void getHomeTimeline(AsyncHttpResponseHandler handler) {
		getHomeTimeline(0, 0, handler);
	}

	public void getHomeTimeline(long max_id, long since_id, AsyncHttpResponseHandler handler) {
		String apiUrl = getApiUrl("statuses/home_timeline.json");
		RequestParams params = new RequestParams();
		if (max_id > 0)
			params.put("max_id", String.valueOf(max_id));
		else if (since_id > 0)
			params.put("since_id", String.valueOf(since_id));
		client.get(apiUrl, params, handler);
	}

    /**
     * Get the user timeline for the user
     * @link https://dev.twitter.com/rest/reference/get/statuses/home_timeline
     * @param handler
     */
    public void getUserTimeline(long user_id, AsyncHttpResponseHandler handler) {
        getHomeTimeline(0, 0, handler);
    }

    public void getUserTimeline(long user_id, long since_id, AsyncHttpResponseHandler handler) {
        if (user_id == 0) return;

        String apiUrl = getApiUrl("statuses/user_timeline.json");
        RequestParams params = new RequestParams();
        params.put("user_id", user_id);

        if (since_id > 0)
            params.put("since_id", String.valueOf(since_id));
        client.get(apiUrl, params, handler);
    }

    /**
     * Get the mentions timeline for the user
     * @link https://dev.twitter.com/rest/reference/get/statuses/home_timeline
     * @param handler
     */
    public void getMentions(AsyncHttpResponseHandler handler) {
        getHomeTimeline(0, 0, handler);
    }

    public void getMentions(long max_id, long since_id, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("statuses/mentions_timeline.json");
        RequestParams params = new RequestParams();
        if (max_id > 0)
            params.put("max_id", String.valueOf(max_id));
        else if (since_id > 0)
            params.put("since_id", String.valueOf(since_id));
        client.get(apiUrl, params, handler);
    }

    /**
     * Post Media Content and return the ids
     * @link https://dev.twitter.com/rest/reference/post/media/upload
     */
    public void postMediaStatus(String mediaUrl, AsyncHttpResponseHandler handler) {
        String apiUrl = "https://upload.twitter.com/1.1/media/upload.json";

        File file = new File(mediaUrl);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Bitmap image = BitmapFactory.decodeStream(fis);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            RequestParams params = new RequestParams();
            params.put("media_data", encodedImage);
            client.post(apiUrl, params, handler);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Post a new tweet
     * @link https://dev.twitter.com/rest/reference/post/statuses/update
     */
    public void postStatus(String status, long replyID, Location location, String mediaIds, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("statuses/update.json");

        RequestParams params = new RequestParams();
        params.put("status", status);
        if (replyID > 0)
            params.put("in_reply_to_status_id", replyID);
        if (location != null) {
            params.put("lat", location.getLatitude());
            params.put("long", location.getLongitude());
            params.put("display_coordinates", true);
        }
        if (!mediaIds.isEmpty())
            params.put("media_ids", mediaIds);

        client.post(apiUrl, params, handler);
    }

    /**
     * @link https://dev.twitter.com/rest/reference/post/statuses/retweet/%3Aid
     * @param tweet_id id of tweet to favorite
     */
    public void retweetTweet(long tweet_id, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl(String.format("statuses/retweet/%d.json", tweet_id));
        client.post(apiUrl, null, handler);
    }

    /**
     * @link https://dev.twitter.com/rest/reference/post/favorites/create
     * @param tweet_id id of tweet to favorite
     */
    public void favoriteTweet(long tweet_id, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("favorites/create.json");
        RequestParams params = new RequestParams();
        params.put("id", tweet_id);
        client.post(apiUrl, params, handler);
    }
    public void unFavoriteTweet(long tweet_id, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("favorites/destroy.json");
        RequestParams params = new RequestParams();
        params.put("id", tweet_id);
        client.post(apiUrl, params, handler);
    }


    /**
     * @link https://dev.twitter.com/rest/reference/get/users/show
     * @param user_id id of user to get info
     */
    public void getUserInfo(long user_id, AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("users/show.json");
        RequestParams params = new RequestParams();
        params.put("user_id", user_id);
        client.get(apiUrl, params, handler);
    }

}