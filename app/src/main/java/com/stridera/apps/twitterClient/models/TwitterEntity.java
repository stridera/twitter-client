package com.stridera.apps.twitterClient.models;

import org.json.JSONException;
import org.json.JSONObject;

public class TwitterEntity {
//    private TwitterHashtags hashtags;
//    private TwitterUrls urls;
//    private TwitterMedia media;

    public TwitterEntity(JSONObject entities) throws JSONException {
//        JSONArray hashtagsJSON = entities.optJSONArray("hashtags");
//        if (hashtagsJSON != null) {
//            hashtags =  TwitterHashtags(hashtagsJSON);
//        } else {
//            hashtags = null;
//        }
//
//        JSONArray urlsJSON = entities.optJSONArray("urls");
//        if (urlsJSON != null) {
//            urls = TwitterUrls(urlsJSON);
//        } else {
//            urls = null;
//        }
//
//        JSONArray MediaJSON = entities.optJSONArray("media");
//        if (MediaJSON != null) {
//            media = TwitterMedia.parseJSON(MediaJSON);
//        } else {
//            media = null;
//        }
    }
}

/*
    "entities": {
      "hashtags": [],
      "symbols": [],
      "user_mentions": [],
      "urls": [],
      "media": []
    },
 */