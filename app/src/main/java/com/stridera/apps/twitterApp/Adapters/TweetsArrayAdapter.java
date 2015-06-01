package com.stridera.apps.twitterApp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Picasso;
import com.stridera.apps.twitterApp.Activities.DetailedViewActivity;
import com.stridera.apps.twitterApp.Activities.HomeTimelineActivity;
import com.stridera.apps.twitterApp.Activities.ViewProfileActivity;
import com.stridera.apps.twitterApp.Utils.Utils;
import com.stridera.apps.twitterClient.R;
import com.stridera.apps.twitterClient.TwitterApplication;
import com.stridera.apps.twitterClient.TwitterClient;
import com.stridera.apps.twitterClient.models.Tweet;
import com.stridera.apps.twitterClient.models.TwitterUser;
import com.stridera.apps.twitterClient.models.entities.TwitterMedia;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class TweetsArrayAdapter extends ArrayAdapter {
    Context parentContext;
    TwitterClient client;

    private static class ViewHolder {
        ImageView ivUserProfile;
        TextView tvUserName;
        TextView tvUserScreenName;
        TextView tvTweet;
        TextView tvTime;
        ImageView ivMedia;
        ImageView ivReply;
        ImageView ivRetweet;
        TextView tvRetweet;
        ImageView ivFavorite;
        TextView tvFavorite;
    }

    public TweetsArrayAdapter(Context context, List<Tweet> tweets) {
        super(context, R.layout.tweet, tweets);
        parentContext = context;
        client = TwitterApplication.getRestClient();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tweet, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.ivUserProfile = (ImageView) convertView.findViewById(R.id.ivUserProfile);
            viewHolder.tvUserName = (TextView) convertView.findViewById(R.id.tvUserName);
            viewHolder.tvUserScreenName = (TextView) convertView.findViewById(R.id.tvUserScreenName);
            viewHolder.tvTweet = (TextView) convertView.findViewById(R.id.tvTweet);
            viewHolder.tvTime = (TextView) convertView.findViewById(R.id.tvTime);
            viewHolder.ivMedia = (ImageView) convertView.findViewById(R.id.ivMedia);
            viewHolder.ivReply = (ImageView) convertView.findViewById(R.id.ivReply);
            viewHolder.tvRetweet = (TextView) convertView.findViewById(R.id.tvRetweet);
            viewHolder.ivRetweet = (ImageView) convertView.findViewById(R.id.ivRetweet);
            viewHolder.tvFavorite = (TextView) convertView.findViewById(R.id.tvFavorite);
            viewHolder.ivFavorite = (ImageView) convertView.findViewById(R.id.ivFavorite);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Tweet tweet = (Tweet) getItem(position);
        TwitterUser user = tweet.getUser();
        TwitterMedia media = tweet.getMedia();


        viewHolder.ivUserProfile.setImageResource(0);
//        Bitmap profileImage = user.getBitmap(getContext());
//        if (profileImage != null)
//            viewHolder.ivUserProfile.setImageBitmap(profileImage);

        viewHolder.ivUserProfile.setTag(user.getUserId());
        Picasso.with(getContext()).load(user.getProfile_image_url()).resize(52, 52).into(viewHolder.ivUserProfile);
        viewHolder.ivUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long user_id = (long) v.getTag();
                Intent intent = new Intent(parentContext, ViewProfileActivity.class);
                intent.putExtra("user_id", user_id);
                parentContext.startActivity(intent);
            }
        });
        viewHolder.tvUserName.setTag(tweet.getTweet_id());
        viewHolder.tvUserName.setText(user.getName());
        viewHolder.tvUserName.setOnClickListener(viewDetailed);
        viewHolder.tvUserScreenName.setText("@" + user.getScreen_name());
        viewHolder.tvTime.setText(tweet.getRelativeTimeCreated());
        viewHolder.tvTweet.setText(Html.fromHtml(tweet.getText()));
        viewHolder.tvRetweet.setText(tweet.getRetweet_count() > 0 ? String.valueOf(tweet.getRetweet_count()) : "");
        viewHolder.tvFavorite.setText(tweet.getFavorite_count() > 0 ? String.valueOf(tweet.getFavorite_count()) : "");
        viewHolder.ivMedia.setTag(tweet.getTweet_id());
        viewHolder.ivMedia.setImageResource(0); viewHolder.ivMedia.setImageDrawable(null);
        if (media != null) {
            Picasso.with(getContext()).load(media.getMedia_url()).into(viewHolder.ivMedia);
        }
        viewHolder.ivFavorite.setOnClickListener(viewDetailed);

        // Clickables
        viewHolder.ivReply.setTag(user.getUserId());
        viewHolder.ivReply.setOnClickListener(replySelected);

        viewHolder.ivRetweet.setTag(tweet.getTweet_id());
        if (tweet.isRetweeted()) viewHolder.ivRetweet.setImageResource(R.mipmap.ic_retweeted);
        else viewHolder.ivRetweet.setImageResource(R.drawable.ic_retweet);
        viewHolder.ivRetweet.setOnClickListener(retweetSelected);

        viewHolder.ivFavorite.setTag(tweet.getTweet_id());
        if (tweet.isFavorited()) viewHolder.ivFavorite.setImageResource(R.mipmap.ic_favorited);
        else viewHolder.ivFavorite.setImageResource(R.drawable.ic_favorite);
        viewHolder.ivFavorite.setOnClickListener(favoriteSelected);

        return convertView;
    }

    private void incrementTv(TextView view) {
        if (view == null) return;

        int count = 1;
        if (view.getText() != null && !view.getText().toString().isEmpty())
            count = Integer.valueOf(view.getText().toString()) + 1;
        view.setText(String.valueOf(count));
    }

    private void decrementTv(TextView view) {
        if (view == null) return;

        if (!view.getText().toString().isEmpty()) {
            int count = Integer.valueOf(view.getText().toString()) - 1;
            view.setText(count > 0 ? String.valueOf(count) : "");
        }
    }

    private void addAnimation(View v) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 2, 1, 2, v.getWidth() / 2.0F, v.getHeight() / 2.0F);
        scaleAnimation.setDuration(500);
        v.startAnimation(scaleAnimation);
    }

    private View.OnClickListener viewDetailed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long tweet_id = (long) v.getTag();
            Intent intent = new Intent(parentContext, DetailedViewActivity.class);
            intent.putExtra("tweet_id", tweet_id);
            parentContext.startActivity(intent);
        }
    };

    private View.OnClickListener replySelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.isNetworkAvailable(parentContext)) {
                Toast.makeText(parentContext, "Network is not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            long user_id = (long) v.getTag();
            Log.d("blah", "Click Listener received id: " + user_id);
            if (parentContext instanceof HomeTimelineActivity)
                ((HomeTimelineActivity) parentContext).writeTweet(user_id);
        }
    };

    private View.OnClickListener retweetSelected = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!Utils.isNetworkAvailable(parentContext)) {
                Toast.makeText(parentContext, "Network is not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = (long) v.getTag();
            final Tweet t = Tweet.getById(id);

            if (!t.isRetweeted()) { // Can un-retweet.. have to delete it.
                client.retweetTweet(id, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);

                        int retweet_count = 0;

                        try {
                            t.setRetweeted(response.getBoolean("retweeted"));
                            retweet_count = response.getInt("retweet_count");
                            t.setRetweet_count(retweet_count);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        ImageView ivRetweet = (ImageView) v.findViewById(R.id.ivRetweet);
                        ivRetweet.setImageResource(R.mipmap.ic_retweeted);
                        TextView tvRetweet = (TextView) ((View) v.getParent()).findViewById(R.id.tvRetweet);
                        tvRetweet.setText(retweet_count > 0 ? String.valueOf(retweet_count) : "");
                        addAnimation(v);
                    }
                });
            }
        }
    };

    private View.OnClickListener favoriteSelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.isNetworkAvailable(parentContext)) {
                Toast.makeText(parentContext, "Network is not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = (long) v.getTag();
            Tweet t = Tweet.getById(id);

            ImageView ivFavorite = (ImageView) v.findViewById(R.id.ivFavorite);
            TextView tvFavorite = (TextView) ((View) v.getParent()).findViewById(R.id.tvFavorite);
            if (!t.isFavorited()) {
                client.favoriteTweet(id, new JsonHttpResponseHandler() {
                    // Todo: Don't assume this is always successful
                });
                t.favorited();
                incrementTv(tvFavorite);
                ivFavorite.setImageResource(R.mipmap.ic_favorited);
            } else {
                client.unFavoriteTweet(id, new JsonHttpResponseHandler() {
                    // Todo: Don't assume this is always successful
                });
                t.unFavorited();
                decrementTv(tvFavorite);
                ivFavorite.setImageResource(R.drawable.ic_favorite);
            }
            addAnimation(v);
        }
    };
}
