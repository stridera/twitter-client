package com.stridera.apps.twitterClient.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.stridera.apps.twitterClient.models.entities.TwitterMedia;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class TwitterImage {
    static public Bitmap getImage(TwitterUser item, Context context) {
        return _getImage("user_" + item.getUserId(), item.getProfile_image_url(), context);
    }

    static public Bitmap getImage(TwitterMedia item, Context context) {
        return _getImage("media_" + item.getMedia_url(), item.getMedia_url(), context);
    }

    static private Bitmap _getImage(String key, String media_url, Context context) {
        Bitmap image = null;

        if (media_url != null && !media_url.isEmpty()) {
            String fileName = key + "_" + Uri.parse(media_url).getLastPathSegment();
            File file = new File(fileName);

            //TODO: Consider using external storage if Environment.getExternalStorageState() is mounted
            try {
                if (file.exists()) {
                    BufferedInputStream input = new BufferedInputStream(new FileInputStream(fileName));
                    image = BitmapFactory.decodeStream(input);
                } else {
                    // Get the image
                    URL url = new URL(media_url);
                    image = BitmapFactory.decodeStream(url.openStream());

                    //  Compress and save to disk
                    FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }
}
