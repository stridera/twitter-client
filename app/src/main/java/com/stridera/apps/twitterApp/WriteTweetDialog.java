package com.stridera.apps.twitterApp;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.stridera.apps.twitterClient.R;
import com.stridera.apps.twitterClient.TwitterApplication;
import com.stridera.apps.twitterClient.TwitterClient;
import com.stridera.apps.twitterClient.models.TwitterUser;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;

public class WriteTweetDialog  extends DialogFragment {
    static final int REQUEST_IMAGE_CAPTURE = 1;

    TwitterClient client;

    private ImageView ivProgress;
    private View vBlackout;
    private EditText etEditor;
    private TextView tvCount;
    private ImageView ivCameraImage;
    private ImageButton btnLocation;
    private ImageButton btnCamera;
    private Button btnTweet;

    private long replyToId;

    // Used for Location and Map Fragment
    Location location;
//    GoogleMap map;

    // Image from the Camera
    String currentCameraImagePath;

    public WriteTweetDialog() {
    }

    OnTweetSentListener onTweetSentListener;
    public interface OnTweetSentListener {
        void onTweetSent();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write_tweet, container);
        getDialog().setTitle(getResources().getString(R.string.compose_tweet));

        replyToId = getArguments().getLong("reply_to", 0);

        ivProgress = (ImageView) view.findViewById(R.id.ivProgress);
        vBlackout = view.findViewById(R.id.vBlackout);
        etEditor = (EditText) view.findViewById(R.id.etTweetEditor);
        tvCount = (TextView) view.findViewById(R.id.tvCharacterCount);
        ivCameraImage = (ImageView) view.findViewById(R.id.ivCameraImage);

        etEditor.addTextChangedListener(tweetTextWatcher);

        location = null;
        btnLocation = (ImageButton) view.findViewById(R.id.btnLocation);
        btnLocation.setOnClickListener(onLocationClickListener);

        currentCameraImagePath = "";
        btnCamera = (ImageButton) view.findViewById(R.id.btnCamera);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            btnCamera.setOnClickListener(onCameraClickListener);
        } else {
            btnCamera.setVisibility(View.GONE);
        }

        btnTweet = (Button) view.findViewById(R.id.btnSubmit);
        btnTweet.setOnClickListener(onTweetListener);

        if (replyToId > 0) {
            TwitterUser user = TwitterUser.getById(replyToId);
            if (user != null) {
                etEditor.setText("@" + user.getScreen_name() + " ");
                etEditor.setSelection(etEditor.getText().length());
            } else
                Log.d("blah", "Unable to find user with id: " + replyToId);
        }

        return view;
    }


    /**
     * Used for monitoring when the tweet is being typed so we can keep track of character count.
     */
    public TextWatcher tweetTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            tvCount.setText(String.valueOf(140 - etEditor.getText().toString().length()));
        }
    };

    /**
     *    User clicked on the location button
     */
    public View.OnClickListener onLocationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (location == null) {
                addRotationAnimation(btnLocation);
                LocationManager locationManager = (LocationManager) getActivity().getSystemService(Activity.LOCATION_SERVICE);
                locationManager.requestSingleUpdate(new Criteria(), locationListener, null);
            } else {
                location = null;
                btnLocation.setImageResource(R.drawable.ic_location);
                addScaleAnimation(btnLocation);
            }
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location l) {
            location = l;
            btnLocation.setImageResource(R.drawable.ic_location_selected);
            addScaleAnimation(btnLocation);
            Log.d("blah", "Location " + l.getLatitude() + ", " + l.getLongitude());

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /**
     * User clicked on the Camera button
     */
    public View.OnClickListener onCameraClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Activity activity = getActivity();
            if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            //file path of captured image
            currentCameraImagePath = cursor.getString(columnIndex);

            cursor.close();

            setCapturedImage(currentCameraImagePath);
        }
    }

    private void setCapturedImage(final String imagePath){
        new AsyncTask<Void,Void,String>(){
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return getRightAngleImage(imagePath);
                }catch (Throwable e){
                    e.printStackTrace();
                }
                return imagePath;
            }

            @Override
            protected void onPostExecute(String imagePath) {
                super.onPostExecute(imagePath);
                ivCameraImage.setImageBitmap(decodeFile(imagePath));
            }
        }.execute();
    }

    private String getRightAngleImage(String photoPath) {

        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degree = 0;

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    degree = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                case ExifInterface.ORIENTATION_UNDEFINED:
                    degree = 0;
                    break;
                default:
                    degree = 90;
            }

            return rotateImage(degree,photoPath);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return photoPath;
    }

    private String rotateImage(int degree, String imagePath){

        if(degree<=0){
            return imagePath;
        }
        try{
            Bitmap b= BitmapFactory.decodeFile(imagePath);

            Matrix matrix = new Matrix();
            if(b.getWidth()>b.getHeight()){
                matrix.setRotate(degree);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
                        matrix, true);
            }

            FileOutputStream fOut = new FileOutputStream(imagePath);
            String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            String imageType = imageName.substring(imageName.lastIndexOf(".") + 1);

            FileOutputStream out = new FileOutputStream(imagePath);
            if (imageType.equalsIgnoreCase("png")) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            }else if (imageType.equalsIgnoreCase("jpeg")|| imageType.equalsIgnoreCase("jpg")) {
                b.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
            fOut.flush();
            fOut.close();

            b.recycle();
        }catch (Exception e){
            e.printStackTrace();
        }
        return imagePath;
    }

    public Bitmap decodeFile(String path) {
        try {
            // Decode deal_image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to sxcale to
            final int REQUIRED_SIZE = 64;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;
            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public View.OnClickListener onTweetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String status = etEditor.getText().toString();

            if (status.isEmpty()) {
                Toast.makeText(getActivity(), "Status is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            etEditor.setEnabled(false);
            btnCamera.setEnabled(false);
            btnLocation.setEnabled(false);
            btnTweet.setEnabled(false);

            vBlackout.setVisibility(View.VISIBLE);
            vBlackout.animate().alpha(0.5F).setDuration(2000);
            addTwitterBirdAnimation(ivProgress);



            client = TwitterApplication.getRestClient();
            if (currentCameraImagePath.isEmpty()) {
                client.postStatus(status, replyToId, location, "",  onStatusSentHandler);
            } else {
                client.postMediaStatus(currentCameraImagePath, onMediaSentHandler);
            }
        }
    };

    private JsonHttpResponseHandler onMediaSentHandler = new JsonHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            Log.d("blah", "Media Response: " + response.toString());

            if (statusCode == 200) {
                try {
                    String mediaId = response.getString("media_id_string");
                    String status = etEditor.getText().toString();

                    client.postStatus(status, replyToId, location, mediaId, onStatusSentHandler);
                } catch (JSONException e) {
                    Log.d("blah", "Can't get media id");
                }
            }
        }


        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            Log.d("blah", "Media Update failed with status: " + statusCode);
            if (errorResponse != null)
                Log.d("blah", "Status Response: " + errorResponse.toString());
        }
    };

    private JsonHttpResponseHandler onStatusSentHandler = new JsonHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            Log.d("blah", "Status Response: " + response.toString());
            if (onTweetSentListener != null)
                onTweetSentListener.onTweetSent();
            else
                Log.d("blah", "Callback is null");
            //TODO: Make bird flip!
            dismiss();
        }


        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            Log.d("blah", "Status Update failed with status: " + statusCode);
            if (errorResponse != null)
                Log.d("blah", "Status Response: " + errorResponse.toString());
        }
    };

    private void addRotationAnimation(View v) {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F);
        rotate.setDuration(1000);
        rotate.setRepeatCount(Animation.INFINITE);
        v.startAnimation(rotate);
    }

    private void addScaleAnimation(View v) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 2, 1, 2, v.getWidth() / 2.0F, v.getHeight() / 2.0F);
        scaleAnimation.setDuration(500);
        v.startAnimation(scaleAnimation);
    }

    private void addTwitterBirdAnimation(View v) {
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
        v.setBackground(animationDrawable);
        animationDrawable.start();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onTweetSentListener = (OnTweetSentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTweetSentListener");
        }
    }
}
